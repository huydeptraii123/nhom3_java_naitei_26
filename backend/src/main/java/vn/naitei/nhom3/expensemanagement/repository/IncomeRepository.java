package vn.naitei.nhom3.expensemanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.naitei.nhom3.expensemanagement.entity.Income;

import java.math.BigDecimal;
import java.util.List;

public interface IncomeRepository extends JpaRepository<Income, Long> {

    List<Income> findByUserId(Long userId);

    List<Income> findByUserIdAndCategoryId(Long userId, Long categoryId);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Income i WHERE i.user.id = :userId")
    BigDecimal sumAmountByUserId(@Param("userId") Long userId);
}
