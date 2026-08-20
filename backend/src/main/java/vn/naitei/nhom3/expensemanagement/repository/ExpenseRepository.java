package vn.naitei.nhom3.expensemanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.naitei.nhom3.expensemanagement.entity.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    List<Expense> findByUserId(Long userId);

    List<Expense> findByUserIdAndCategoryId(Long userId, Long categoryId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user.id = :userId")
    BigDecimal sumAmountByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e "
            + "WHERE e.user.id = :userId AND e.category.id = :categoryId "
            + "AND e.expenseDate BETWEEN :start AND :end")
    BigDecimal sumAmountByUserIdAndCategoryIdAndExpenseDateBetween(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}
