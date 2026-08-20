package vn.naitei.nhom3.expensemanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import vn.naitei.nhom3.expensemanagement.dto.importexport.ExportEntityType;
import vn.naitei.nhom3.expensemanagement.entity.Budget;
import vn.naitei.nhom3.expensemanagement.entity.Category;
import vn.naitei.nhom3.expensemanagement.entity.Expense;
import vn.naitei.nhom3.expensemanagement.entity.Income;
import vn.naitei.nhom3.expensemanagement.entity.User;
import vn.naitei.nhom3.expensemanagement.entity.enums.CategoryType;
import vn.naitei.nhom3.expensemanagement.entity.enums.Role;
import vn.naitei.nhom3.expensemanagement.entity.enums.UserStatus;
import vn.naitei.nhom3.expensemanagement.repository.BudgetRepository;
import vn.naitei.nhom3.expensemanagement.repository.CategoryRepository;
import vn.naitei.nhom3.expensemanagement.repository.ExpenseRepository;
import vn.naitei.nhom3.expensemanagement.repository.IncomeRepository;
import vn.naitei.nhom3.expensemanagement.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ExportServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private IncomeRepository incomeRepository;
    @Mock
    private BudgetRepository budgetRepository;

    @InjectMocks
    private ExportServiceImpl exportService;

    private static boolean hasBom(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF;
    }

    private static List<CSVRecord> parse(byte[] csvBytes) throws IOException {
        int offset = hasBom(csvBytes) ? 3 : 0;
        String content = new String(csvBytes, offset, csvBytes.length - offset, StandardCharsets.UTF_8);
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();
        try (CSVParser parser = CSVParser.parse(new StringReader(content), format)) {
            return parser.getRecords();
        }
    }

    private static User user(long id, String name, String email, Role role, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }

    private static Category category(long id, String name, CategoryType type, User owner) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setType(type);
        category.setUser(owner);
        return category;
    }

    @Test
    void exportUserWritesIdNameEmailRoleActiveAndDerivedTotals() throws IOException {
        User user = user(1L, "Alice", "alice@test.com", Role.ADMIN, UserStatus.ACTIVE);
        when(userRepository.findAll(eq(Sort.by("id")))).thenReturn(List.of(user));
        when(expenseRepository.sumAmountByUserId(1L)).thenReturn(new BigDecimal("100.00"));
        when(incomeRepository.sumAmountByUserId(1L)).thenReturn(new BigDecimal("300.00"));

        byte[] csv = exportService.exportCsv(ExportEntityType.USER);
        List<CSVRecord> records = parse(csv);

        assertEquals(1, records.size());
        CSVRecord row = records.get(0);
        assertEquals("1", row.get("id"));
        assertEquals("Alice", row.get("name"));
        assertEquals("alice@test.com", row.get("email"));
        assertEquals("ADMIN", row.get("role"));
        assertEquals("true", row.get("active"));
        assertEquals("100.00", row.get("totalExpense"));
        assertEquals("300.00", row.get("totalIncome"));
        assertEquals("200.00", row.get("balance"));
    }

    @Test
    void exportExpenseWritesUserEmailAndCategoryName() throws IOException {
        User user = user(1L, "Bob", "bob@test.com", Role.USER, UserStatus.ACTIVE);
        Category category = category(10L, "Food", CategoryType.EXPENSE, null);
        Expense expense = new Expense();
        expense.setUser(user);
        expense.setCategory(category);
        expense.setTitle("Lunch");
        expense.setAmount(new BigDecimal("50000.00"));
        expense.setExpenseDate(LocalDate.of(2026, 1, 15));
        expense.setNote("with team");
        when(expenseRepository.findAll(eq(Sort.by("id")))).thenReturn(List.of(expense));

        byte[] csv = exportService.exportCsv(ExportEntityType.EXPENSE);
        CSVRecord row = parse(csv).get(0);

        assertEquals("bob@test.com", row.get("userEmail"));
        assertEquals("Lunch", row.get("title"));
        assertEquals("Food", row.get("category"));
        assertEquals("50000.00", row.get("amount"));
        assertEquals("2026-01-15", row.get("date"));
        assertEquals("with team", row.get("note"));
    }

    @Test
    void exportIncomeMapsTitleFieldToSourceColumn() throws IOException {
        User user = user(1L, "Bob", "bob@test.com", Role.USER, UserStatus.ACTIVE);
        Category category = category(20L, "Salary", CategoryType.INCOME, null);
        Income income = new Income();
        income.setUser(user);
        income.setCategory(category);
        income.setTitle("Monthly salary");
        income.setAmount(new BigDecimal("5000000.00"));
        income.setIncomeDate(LocalDate.of(2026, 1, 1));
        when(incomeRepository.findAll(eq(Sort.by("id")))).thenReturn(List.of(income));

        byte[] csv = exportService.exportCsv(ExportEntityType.INCOME);
        CSVRecord row = parse(csv).get(0);

        assertEquals("Monthly salary", row.get("source"));
        assertEquals("Salary", row.get("category"));
    }

    @Test
    void exportCategoryMarksScopeCommonForGlobalAndPrivateForUserOwned() throws IOException {
        Category global = category(1L, "Food", CategoryType.EXPENSE, null);
        Category own = category(2L, "Custom", CategoryType.EXPENSE, user(1L, "Bob", "bob@test.com", Role.USER, UserStatus.ACTIVE));
        when(categoryRepository.findByDeletedAtIsNullOrderByIdAsc()).thenReturn(List.of(global, own));

        byte[] csv = exportService.exportCsv(ExportEntityType.CATEGORY);
        List<CSVRecord> records = parse(csv);

        assertEquals("COMMON", records.get(0).get("scope"));
        assertEquals("PRIVATE", records.get(1).get("scope"));
    }

    @Test
    void exportBudgetMarksIsOverBudgetTrueWhenSpentExceedsAmount() throws IOException {
        User user = user(1L, "Bob", "bob@test.com", Role.USER, UserStatus.ACTIVE);
        Category category = category(10L, "Food", CategoryType.EXPENSE, null);
        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setYear((short) 2026);
        budget.setMonth((byte) 1);
        budget.setAmount(new BigDecimal("100000.00"));
        when(budgetRepository.findAll(eq(Sort.by("id")))).thenReturn(List.of(budget));
        when(expenseRepository.sumAmountByUserIdAndCategoryIdAndExpenseDateBetween(
                1L, 10L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .thenReturn(new BigDecimal("150000.00"));

        byte[] csv = exportService.exportCsv(ExportEntityType.BUDGET);
        CSVRecord row = parse(csv).get(0);

        assertEquals("2026-01", row.get("month"));
        assertEquals("150000.00", row.get("spent"));
        assertEquals("true", row.get("isOverBudget"));
    }

    @Test
    void exportBudgetMarksIsOverBudgetFalseWhenUnderspent() throws IOException {
        User user = user(1L, "Bob", "bob@test.com", Role.USER, UserStatus.ACTIVE);
        Category category = category(10L, "Food", CategoryType.EXPENSE, null);
        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setYear((short) 2026);
        budget.setMonth((byte) 1);
        budget.setAmount(new BigDecimal("100000.00"));
        when(budgetRepository.findAll(eq(Sort.by("id")))).thenReturn(List.of(budget));
        when(expenseRepository.sumAmountByUserIdAndCategoryIdAndExpenseDateBetween(
                any(), any(), any(), any()))
                .thenReturn(new BigDecimal("50000.00"));

        byte[] csv = exportService.exportCsv(ExportEntityType.BUDGET);
        CSVRecord row = parse(csv).get(0);

        assertEquals("false", row.get("isOverBudget"));
    }

    @Test
    void exportedFileStartsWithUtf8Bom() {
        when(categoryRepository.findByDeletedAtIsNullOrderByIdAsc()).thenReturn(List.of());

        byte[] csv = exportService.exportCsv(ExportEntityType.CATEGORY);

        assertTrue(hasBom(csv));
    }

    @Test
    void suggestedFileNameReturnsLowercaseEntityNameCsv() {
        assertEquals("user.csv", exportService.suggestedFileName(ExportEntityType.USER));
        assertEquals("expense.csv", exportService.suggestedFileName(ExportEntityType.EXPENSE));
        assertEquals("income.csv", exportService.suggestedFileName(ExportEntityType.INCOME));
        assertEquals("category.csv", exportService.suggestedFileName(ExportEntityType.CATEGORY));
        assertEquals("budget.csv", exportService.suggestedFileName(ExportEntityType.BUDGET));
    }
}
