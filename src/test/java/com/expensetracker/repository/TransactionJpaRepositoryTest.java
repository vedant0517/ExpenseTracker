package com.expensetracker.repository;

import com.expensetracker.entity.Transaction;
import com.expensetracker.model.TransactionType;
import com.expensetracker.repository.projection.CategoryTotalProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DataJpaTest testing TransactionJpaRepository against an active in-memory database.
 * Tests JPQL queries, pagination, date ranges, and projections.
 */
@DataJpaTest
public class TransactionJpaRepositoryTest {

    @Autowired
    private TransactionJpaRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        // Seed test transactions spanning multiple dates, types, and categories
        repository.save(new Transaction("October Salary", new BigDecimal("5000.00"), "Salary", TransactionType.INCOME, LocalDate.of(2026, 10, 1), "Paycheck"));
        repository.save(new Transaction("October Rent", new BigDecimal("1200.00"), "Housing", TransactionType.EXPENSE, LocalDate.of(2026, 10, 5), "Rent"));
        repository.save(new Transaction("Groceries", new BigDecimal("350.50"), "Food", TransactionType.EXPENSE, LocalDate.of(2026, 10, 10), "Supermarket"));
        repository.save(new Transaction("Dining Out", new BigDecimal("150.00"), "Food", TransactionType.EXPENSE, LocalDate.of(2026, 10, 12), "Restaurant"));
        repository.save(new Transaction("November Bonus", new BigDecimal("1000.00"), "Salary", TransactionType.INCOME, LocalDate.of(2026, 11, 15), "Bonus"));
    }

    @Test
    void testFindFiltered_ByTypeAndCategory() {
        Page<Transaction> foodExpenses = repository.findFiltered(
                TransactionType.EXPENSE,
                "food",
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertEquals(2, foodExpenses.getTotalElements());
        assertTrue(foodExpenses.getContent().stream().allMatch(t -> t.getCategory().equalsIgnoreCase("Food")));
    }

    @Test
    void testFindFiltered_DateRangeBoundaries() {
        // Query only October transactions
        LocalDate octStart = LocalDate.of(2026, 10, 1);
        LocalDate octEnd = LocalDate.of(2026, 10, 31);

        Page<Transaction> octTransactions = repository.findFiltered(
                null,
                null,
                octStart,
                octEnd,
                PageRequest.of(0, 10, Sort.by("date").ascending())
        );

        assertEquals(4, octTransactions.getTotalElements());
        // Verify November Bonus (Nov 15) is excluded
        assertFalse(octTransactions.getContent().stream().anyMatch(t -> t.getTitle().contains("November")));
    }

    @Test
    void testSumByTypeAndDateBetween() {
        LocalDate octStart = LocalDate.of(2026, 10, 1);
        LocalDate octEnd = LocalDate.of(2026, 10, 31);

        BigDecimal octIncome = repository.sumByTypeAndDateBetween(TransactionType.INCOME, octStart, octEnd);
        BigDecimal octExpense = repository.sumByTypeAndDateBetween(TransactionType.EXPENSE, octStart, octEnd);

        // October Income = 5000.00
        // October Expense = 1200.00 + 350.50 + 150.00 = 1700.50
        assertEquals(new BigDecimal("5000.00"), octIncome);
        assertEquals(new BigDecimal("1700.50"), octExpense);
    }

    @Test
    void testFindHighestExpense() {
        LocalDate octStart = LocalDate.of(2026, 10, 1);
        LocalDate octEnd = LocalDate.of(2026, 10, 31);

        BigDecimal highest = repository.findHighestExpense(octStart, octEnd);
        // Rent (1200.00) is highest in October
        assertEquals(new BigDecimal("1200.00"), highest);
    }

    @Test
    void testFindCategoryExpenseTotals_AggregationAndSorting() {
        List<CategoryTotalProjection> totals = repository.findCategoryExpenseTotals(null, null);

        assertFalse(totals.isEmpty());
        // Housing (1200.00) should be first, Food (350.50 + 150.00 = 500.50) should be second
        assertEquals("housing", totals.get(0).getCategory());
        assertEquals(new BigDecimal("1200.00"), totals.get(0).getTotalAmount());

        assertEquals("food", totals.get(1).getCategory());
        assertEquals(new BigDecimal("500.50"), totals.get(1).getTotalAmount());
    }

    @Test
    void testPaginationAndSorting() {
        Page<Transaction> page0 = repository.findFiltered(
                null, null, null, null,
                PageRequest.of(0, 2, Sort.by("amount").descending())
        );

        assertEquals(5, page0.getTotalElements());
        assertEquals(2, page0.getContent().size());
        assertEquals(3, page0.getTotalPages());

        // Highest amounts: 5000.00, then 1200.00
        assertEquals(new BigDecimal("5000.00"), page0.getContent().get(0).getAmount());
        assertEquals(new BigDecimal("1200.00"), page0.getContent().get(1).getAmount());
    }
}
