package com.expensetracker.repository;

import com.expensetracker.entity.Transaction;
import com.expensetracker.model.TransactionType;
import com.expensetracker.repository.projection.CategoryTotalProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA Repository for Transaction entities.
 *
 * Spring Data JPA automatically generates the runtime implementation of this interface,
 * providing standard CRUD (save, findById, deleteById, count, findAll)
 * and executing custom JPQL queries.
 */
@Repository
public interface TransactionJpaRepository extends JpaRepository<Transaction, Long> {

    /**
     * Finds transactions matching optional filters with pagination and sorting.
     */
    @Query("""
        SELECT t FROM Transaction t
        WHERE (:type IS NULL OR t.type = :type)
          AND (:category IS NULL OR LOWER(t.category) LIKE LOWER(CONCAT('%', :category, '%')))
          AND (:startDate IS NULL OR t.date >= :startDate)
          AND (:endDate IS NULL OR t.date <= :endDate)
    """)
    Page<Transaction> findFiltered(
            @Param("type") TransactionType type,
            @Param("category") String category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    /**
     * Calculates the total sum for a specific transaction type (INCOME or EXPENSE)
     * within an optional date range. Returns 0.00 if no records match.
     */
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.type = :type
          AND (:startDate IS NULL OR t.date >= :startDate)
          AND (:endDate IS NULL OR t.date <= :endDate)
    """)
    BigDecimal sumByTypeAndDateBetween(
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Counts transactions within an optional date range.
     */
    @Query("""
        SELECT COUNT(t)
        FROM Transaction t
        WHERE (:startDate IS NULL OR t.date >= :startDate)
          AND (:endDate IS NULL OR t.date <= :endDate)
    """)
    long countByDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Finds the maximum single expense within an optional date range.
     */
    @Query("""
        SELECT MAX(t.amount)
        FROM Transaction t
        WHERE t.type = com.expensetracker.model.TransactionType.EXPENSE
          AND (:startDate IS NULL OR t.date >= :startDate)
          AND (:endDate IS NULL OR t.date <= :endDate)
    """)
    BigDecimal findHighestExpense(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Returns total expenses grouped by category, sorted descending by total spent.
     */
    @Query("""
        SELECT LOWER(t.category) as category, SUM(t.amount) as totalAmount
        FROM Transaction t
        WHERE t.type = com.expensetracker.model.TransactionType.EXPENSE
          AND (:startDate IS NULL OR t.date >= :startDate)
          AND (:endDate IS NULL OR t.date <= :endDate)
        GROUP BY LOWER(t.category)
        ORDER BY totalAmount DESC
    """)
    List<CategoryTotalProjection> findCategoryExpenseTotals(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
