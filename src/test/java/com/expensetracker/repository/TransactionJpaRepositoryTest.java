package com.expensetracker.repository;

import com.expensetracker.entity.Transaction;
import com.expensetracker.entity.User;
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
 * DataJpaTest testing TransactionJpaRepository against an active in-memory database with tenant isolation.
 */
@DataJpaTest
public class TransactionJpaRepositoryTest {

    @Autowired
    private TransactionJpaRepository transactionRepository;

    @Autowired
    private UserJpaRepository userRepository;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        userRepository.deleteAll();

        user1 = userRepository.save(new User("alice", "alice@example.com", "encoded_password_1"));
        user2 = userRepository.save(new User("bob", "bob@example.com", "encoded_password_2"));

        // Seed transactions for user1
        transactionRepository.save(new Transaction("October Salary", new BigDecimal("5000.00"), "Salary", TransactionType.INCOME, LocalDate.of(2026, 10, 1), "Paycheck", user1));
        transactionRepository.save(new Transaction("October Rent", new BigDecimal("1200.00"), "Housing", TransactionType.EXPENSE, LocalDate.of(2026, 10, 5), "Rent", user1));
        transactionRepository.save(new Transaction("Groceries", new BigDecimal("350.50"), "Food", TransactionType.EXPENSE, LocalDate.of(2026, 10, 10), "Supermarket", user1));
        transactionRepository.save(new Transaction("Dining Out", new BigDecimal("150.00"), "Food", TransactionType.EXPENSE, LocalDate.of(2026, 10, 12), "Restaurant", user1));
        transactionRepository.save(new Transaction("November Bonus", new BigDecimal("1000.00"), "Salary", TransactionType.INCOME, LocalDate.of(2026, 11, 15), "Bonus", user1));

        // Seed transaction for user2 (tenant isolation test)
        transactionRepository.save(new Transaction("Bob Rent", new BigDecimal("2500.00"), "Housing", TransactionType.EXPENSE, LocalDate.of(2026, 10, 5), "Rent", user2));
    }

    @Test
    void testFindFiltered_ByTypeAndCategory_UserScoped() {
        Page<Transaction> foodExpenses = transactionRepository.findFilteredByUserId(
                user1.getId(),
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
    void testTenantIsolation_UserCannotSeeAnotherUserTransactions() {
        // Query user2's transactions
        Page<Transaction> bobTransactions = transactionRepository.findFilteredByUserId(
                user2.getId(),
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );
        assertEquals(1, bobTransactions.getTotalElements());
        assertEquals("Bob Rent", bobTransactions.getContent().get(0).getTitle());

        // Verify user1's queries never return Bob's transaction
        Page<Transaction> aliceHousing = transactionRepository.findFilteredByUserId(
                user1.getId(),
                TransactionType.EXPENSE,
                "housing",
                null,
                null,
                PageRequest.of(0, 10)
        );
        assertEquals(1, aliceHousing.getTotalElements());
        assertEquals("October Rent", aliceHousing.getContent().get(0).getTitle());
        assertEquals(new BigDecimal("1200.00"), aliceHousing.getContent().get(0).getAmount());
    }

    @Test
    void testFindFiltered_DateRangeBoundaries() {
        LocalDate octStart = LocalDate.of(2026, 10, 1);
        LocalDate octEnd = LocalDate.of(2026, 10, 31);

        Page<Transaction> octTransactions = transactionRepository.findFilteredByUserId(
                user1.getId(),
                null,
                null,
                octStart,
                octEnd,
                PageRequest.of(0, 10, Sort.by("date").ascending())
        );

        assertEquals(4, octTransactions.getTotalElements());
        assertFalse(octTransactions.getContent().stream().anyMatch(t -> t.getTitle().contains("November")));
    }

    @Test
    void testSumByUserIdAndTypeAndDateBetween() {
        LocalDate octStart = LocalDate.of(2026, 10, 1);
        LocalDate octEnd = LocalDate.of(2026, 10, 31);

        BigDecimal octIncome = transactionRepository.sumByUserIdAndTypeAndDateBetween(user1.getId(), TransactionType.INCOME, octStart, octEnd);
        BigDecimal octExpense = transactionRepository.sumByUserIdAndTypeAndDateBetween(user1.getId(), TransactionType.EXPENSE, octStart, octEnd);

        // October Income = 5000.00
        // October Expense = 1200.00 + 350.50 + 150.00 = 1700.50
        assertEquals(new BigDecimal("5000.00"), octIncome);
        assertEquals(new BigDecimal("1700.50"), octExpense);
    }

    @Test
    void testFindHighestExpenseByUserId() {
        LocalDate octStart = LocalDate.of(2026, 10, 1);
        LocalDate octEnd = LocalDate.of(2026, 10, 31);

        BigDecimal highest = transactionRepository.findHighestExpenseByUserId(user1.getId(), octStart, octEnd);
        // Rent (1200.00) is highest for user1
        assertEquals(new BigDecimal("1200.00"), highest);
    }

    @Test
    void testCountByUserIdAndTypeAndDateBetween() {
        LocalDate octStart = LocalDate.of(2026, 10, 1);
        LocalDate octEnd = LocalDate.of(2026, 10, 31);

        long octExpenses = transactionRepository.countByUserIdAndTypeAndDateBetween(user1.getId(), TransactionType.EXPENSE, octStart, octEnd);
        long octIncomes = transactionRepository.countByUserIdAndTypeAndDateBetween(user1.getId(), TransactionType.INCOME, octStart, octEnd);
        long totalExpenses = transactionRepository.countByUserIdAndTypeAndDateBetween(user1.getId(), TransactionType.EXPENSE, null, null);
        long totalIncomes = transactionRepository.countByUserIdAndTypeAndDateBetween(user1.getId(), TransactionType.INCOME, null, null);

        assertEquals(3, octExpenses, "User1 October should have 3 expense transactions");
        assertEquals(1, octIncomes, "User1 October should have 1 income transaction");
        assertEquals(3, totalExpenses, "User1 all-time expenses should be 3");
        assertEquals(2, totalIncomes, "User1 all-time incomes should be 2");
    }

    @Test
    void testFindCategoryExpenseTotalsByUserId() {
        List<CategoryTotalProjection> totals = transactionRepository.findCategoryExpenseTotalsByUserId(user1.getId(), null, null);

        assertFalse(totals.isEmpty());
        // Housing (1200.00) should be first, Food (350.50 + 150.00 = 500.50) should be second
        assertEquals("housing", totals.get(0).getCategory());
        assertEquals(new BigDecimal("1200.00"), totals.get(0).getTotalAmount());

        assertEquals("food", totals.get(1).getCategory());
        assertEquals(new BigDecimal("500.50"), totals.get(1).getTotalAmount());
    }

    @Test
    void testFindByIdAndUserId_IDORPrevention() {
        List<Transaction> user1Txns = transactionRepository.findAll();
        Transaction user2Txn = user1Txns.stream()
                .filter(t -> t.getUser().getId().equals(user2.getId()))
                .findFirst()
                .orElseThrow();

        // user1 querying user2's transaction ID -> must return Optional.empty()
        assertTrue(transactionRepository.findByIdAndUserId(user2Txn.getId(), user1.getId()).isEmpty());

        // user2 querying user2's transaction ID -> must succeed
        assertTrue(transactionRepository.findByIdAndUserId(user2Txn.getId(), user2.getId()).isPresent());
    }
}
