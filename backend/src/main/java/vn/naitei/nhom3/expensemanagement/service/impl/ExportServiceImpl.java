package vn.naitei.nhom3.expensemanagement.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.naitei.nhom3.expensemanagement.dto.importexport.ExportEntityType;
import vn.naitei.nhom3.expensemanagement.entity.Budget;
import vn.naitei.nhom3.expensemanagement.entity.Category;
import vn.naitei.nhom3.expensemanagement.entity.Expense;
import vn.naitei.nhom3.expensemanagement.entity.Income;
import vn.naitei.nhom3.expensemanagement.entity.User;
import vn.naitei.nhom3.expensemanagement.entity.enums.UserStatus;
import vn.naitei.nhom3.expensemanagement.exception.BadRequestException;
import vn.naitei.nhom3.expensemanagement.repository.BudgetRepository;
import vn.naitei.nhom3.expensemanagement.repository.CategoryRepository;
import vn.naitei.nhom3.expensemanagement.repository.ExpenseRepository;
import vn.naitei.nhom3.expensemanagement.repository.IncomeRepository;
import vn.naitei.nhom3.expensemanagement.repository.UserRepository;
import vn.naitei.nhom3.expensemanagement.service.ExportService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Ghi thẳng UTF-8 BOM vào đầu file để Excel Windows mở đúng ký tự tiếng Việt
 * (SRS chỉ nói BOM "tuỳ chọn" khi đọc Import, ở đây chủ động thêm khi ghi Export).
 */
@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final BudgetRepository budgetRepository;

    @Override
    public byte[] exportCsv(ExportEntityType entityType) {
        StringWriter writer = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(writer, formatFor(entityType))) {
            switch (entityType) {
                case USER -> writeUsers(printer);
                case EXPENSE -> writeExpenses(printer);
                case INCOME -> writeIncomes(printer);
                case CATEGORY -> writeCategories(printer);
                case BUDGET -> writeBudgets(printer);
            }
        } catch (IOException ex) {
            throw new BadRequestException("Không tạo được file CSV: " + ex.getMessage());
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.writeBytes(UTF8_BOM);
        output.writeBytes(writer.toString().getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }

    @Override
    public String suggestedFileName(ExportEntityType entityType) {
        return entityType.name().toLowerCase() + ".csv";
    }

    private CSVFormat formatFor(ExportEntityType entityType) {
        String[] header = switch (entityType) {
            case USER -> new String[] {
                    "id", "name", "email", "role", "active", "totalExpense", "totalIncome", "balance"};
            case EXPENSE -> new String[] {"userEmail", "title", "category", "amount", "date", "note"};
            case INCOME -> new String[] {"userEmail", "source", "category", "amount", "date", "note"};
            case CATEGORY -> new String[] {"id", "name", "description", "type", "scope"};
            case BUDGET -> new String[] {"userEmail", "category", "month", "amount", "spent", "isOverBudget"};
        };
        return CSVFormat.DEFAULT.builder().setHeader(header).build();
    }

    private void writeUsers(CSVPrinter printer) throws IOException {
        List<User> users = userRepository.findAll(Sort.by("id"));
        for (User user : users) {
            BigDecimal totalExpense = expenseRepository.sumAmountByUserId(user.getId());
            BigDecimal totalIncome = incomeRepository.sumAmountByUserId(user.getId());
            printer.printRecord(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getStatus() == UserStatus.ACTIVE,
                    totalExpense.toPlainString(),
                    totalIncome.toPlainString(),
                    totalIncome.subtract(totalExpense).toPlainString());
        }
    }

    private void writeExpenses(CSVPrinter printer) throws IOException {
        List<Expense> expenses = expenseRepository.findAll(Sort.by("id"));
        for (Expense expense : expenses) {
            printer.printRecord(
                    expense.getUser().getEmail(),
                    expense.getTitle(),
                    expense.getCategory().getName(),
                    expense.getAmount().toPlainString(),
                    expense.getExpenseDate(),
                    expense.getNote());
        }
    }

    private void writeIncomes(CSVPrinter printer) throws IOException {
        List<Income> incomes = incomeRepository.findAll(Sort.by("id"));
        for (Income income : incomes) {
            printer.printRecord(
                    income.getUser().getEmail(),
                    income.getTitle(),
                    income.getCategory().getName(),
                    income.getAmount().toPlainString(),
                    income.getIncomeDate(),
                    income.getNote());
        }
    }

    private void writeCategories(CSVPrinter printer) throws IOException {
        List<Category> categories = categoryRepository.findByDeletedAtIsNullOrderByIdAsc();
        for (Category category : categories) {
            printer.printRecord(
                    category.getId(),
                    category.getName(),
                    category.getDescription(),
                    category.getType(),
                    category.getUser() == null ? "COMMON" : "PRIVATE");
        }
    }

    private void writeBudgets(CSVPrinter printer) throws IOException {
        List<Budget> budgets = budgetRepository.findAll(Sort.by("id"));
        for (Budget budget : budgets) {
            YearMonth yearMonth = YearMonth.of(budget.getYear(), budget.getMonth());
            LocalDate start = yearMonth.atDay(1);
            LocalDate end = yearMonth.atEndOfMonth();
            BigDecimal spent = expenseRepository.sumAmountByUserIdAndCategoryIdAndExpenseDateBetween(
                    budget.getUser().getId(), budget.getCategory().getId(), start, end);

            printer.printRecord(
                    budget.getUser().getEmail(),
                    budget.getCategory().getName(),
                    yearMonth,
                    budget.getAmount().toPlainString(),
                    spent.toPlainString(),
                    spent.compareTo(budget.getAmount()) > 0);
        }
    }
}
