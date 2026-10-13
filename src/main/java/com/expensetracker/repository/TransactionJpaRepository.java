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
import java.util.Optional;

/**
 * Spring Data JPA Repository for Transaction entities.
 * All query methods are scoped to the owning user to enforce strict tenant isolation
 * and prevent Insecure Direct Object Reference (IDOR) vulnerabilities.
 */
@Repository
public interface TransactionJpaRepository extends JpaRepository<Transaction, Long> {

    /**
     * Finds a transaction by its ID and owning user's ID to prevent IDOR vulnerabilities.
     */
    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    /**
     * Checks if a transaction exists by its ID and owning user's ID.
     */
    boolean existsByIdAndUserId(Long id, Long userId);

    /**
     * Finds transactions matching optional filters for a specific user, with pagination and sorting.
     */
    @Query("""
        SELECT t FROM Transaction t
        WHERE t.user.id = :userId
          AND (:type IS NULL OR t.type = :type)
          AND (:category IS NULL OR LOWER(t.category) LIKE LOWER(CONCAT('%', :category, '%')))
          AND (:startDate IS NULL OR t.date >= :startDate)
          AND (:endDate IS NULL OR t.date <= :endDate)
    """)
    Page<Transaction> findFilteredByUserId(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("category") String category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    /**
     * Calculates the total sum for a specific transaction type (INCOME or EXPENSE)
     * for a given user within an optional date range. Returns 0.00 if no records match.
     */
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type = :type
          AND (:startDate IS NULL OR t.date >= :startDate)
          AND (:endDate IS NULL OR t.date <= :endDate)
    """)
    BigDecimal sumByUserIdAndTypeAndDateBetween(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Counts total transactions for a user within an optional date range.
     */
    @Query("""
        SELECT COUNT(t)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND (:startDate IS NULL OR t.date >= :startDate)
          AND (:endDate IS NULL OR t.date <= :endDate)
    """)
    long countByUserIdAndDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Counts transactions of a specific type (INCOME or EXPENSE) for a user within an optional date range.
     */
    @Query("""
        SELECT COUNT(t)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type = :type
          AND (:startDate IS NULL OR t.date >= :startDate)
          AND (:endDate IS NULL OR t.date <= :endDate)
    """)
    long countByUserIdAndTypeAndDateBetween(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Finds the maximum single expense for a user within an optional date range.
     */
    @Query("""
        SELECT MAX(t.amount)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type = com.expensetracker.model.TransactionType.EXPENSE
          AND (:startDate IS NULL OR t.date >= :startDate)
          AND (:endDate IS NULL OR t.date <= :endDate)
    """)
    BigDecimal findHighestExpenseByUserId(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Returns total expenses grouped by category for a user, sorted descending by total spent.
     */
    @Query("""
        SELECT LOWER(t.category) as category, SUM(t.amount) as totalAmount
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type = com.expensetracker.model.TransactionType.EXPENSE
          AND (:startDate IS NULL OR t.date >= :startDate)
          AND (:endDate IS NULL OR t.date <= :endDate)
        GROUP BY LOWER(t.category)
        ORDER BY totalAmount DESC
    """)
    List<CategoryTotalProjection> findCategoryExpenseTotalsByUserId(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
