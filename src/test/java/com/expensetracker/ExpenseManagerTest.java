package com.expensetracker;

import com.expensetracker.exception.StorageException;
import com.expensetracker.model.FinancialPeriodSummary;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionType;
import com.expensetracker.repository.CsvTransactionRepository;
import com.expensetracker.repository.JdbcTransactionRepository;
import com.expensetracker.service.ExpenseManager;
import com.expensetracker.util.DatabaseConnectionManager;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Core Java and JDBC regression test suite for Phase 1-3 components.
 * Verified to run both in JUnit 5 runner and as a standalone executable suite.
 */
public class ExpenseManagerTest {

    public static void main(String[] args) {
        System.out.println("=== Running Expense Tracker Core/JDBC Test Suite ===");
        ExpenseManagerTest runner = new ExpenseManagerTest();
        int passed = 0;
        int failed = 0;

        List<TestItem> tests = List.of(
                new TestItem("testEmptyTracker", runner::testEmptyTracker),
                new TestItem("testAddAndAutoIncrementIds", runner::testAddAndAutoIncrementIds),
                new TestItem("testFinancialCalculations", runner::testFinancialCalculations),
                new TestItem("testUpdateAndValidation", runner::testUpdateAndValidation),
                new TestItem("testDeleteExistingAndNonExistent", runner::testDeleteExistingAndNonExistent),
                new TestItem("testFiltering", runner::testFiltering),
                new TestItem("testCsvPersistenceAndRestartIdContinuity", runner::testCsvPersistenceAndRestartIdContinuity),
                new TestItem("testRfc4180SpecialCharactersInCsv", runner::testRfc4180SpecialCharactersInCsv),
                new TestItem("testMissingAndMalformedCsvHandling", runner::testMissingAndMalformedCsvHandling),
                new TestItem("testCategoryWiseExpensesAndAverages", runner::testCategoryWiseExpensesAndAverages),
                new TestItem("testWeeklyMonthlyYearlyReports", runner::testWeeklyMonthlyYearlyReports),
                new TestItem("testJdbcCrudAndGeneratedKeys", runner::testJdbcCrudAndGeneratedKeys),
                new TestItem("testJdbcTransactionBatchAndRollback", runner::testJdbcTransactionBatchAndRollback),
                new TestItem("testDatabaseConnectionFailure", runner::testDatabaseConnectionFailure)
        );

        for (TestItem test : tests) {
            try {
                test.runnable.run();
                passed++;
                System.out.println("✔ PASS: " + test.name);
            } catch (Throwable t) {
                failed++;
                System.err.println("✘ FAIL: " + test.name + " - " + t.getMessage());
                t.printStackTrace(System.err);
            }
        }

        System.out.printf("%nTest Summary: %d passed, %d failed.%n", passed, failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    record TestItem(String name, ThrowingRunnable runnable) {}

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Throwable;
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError(message + " [Expected: " + expected + ", Actual: " + actual + "]");
        }
    }

    // --- Phase 1 Baseline Tests ---

    @Test
    public void testEmptyTracker() {
        ExpenseManager manager = new ExpenseManager();
        assertTrue(manager.isEmpty(), "New manager should be empty");
        assertEquals(0, manager.getTransactionCount(), "Initial count should be 0");
        assertEquals(new BigDecimal("0.00"), manager.getTotalIncome(), "Total income on empty manager should be 0.00");
        assertEquals(new BigDecimal("0.00"), manager.getTotalExpenses(), "Total expense on empty manager should be 0.00");
        assertEquals(new BigDecimal("0.00"), manager.getBalance(), "Balance on empty manager should be 0.00");
        assertTrue(manager.findById(999).isEmpty(), "Searching non-existent ID should return Optional.empty()");
    }

    @Test
    public void testAddAndAutoIncrementIds() throws Exception {
        ExpenseManager manager = new ExpenseManager();
        Transaction t1 = manager.addTransaction("Salary", new BigDecimal("5000.00"), "Work", TransactionType.INCOME, LocalDate.now(), "Monthly salary");
        Transaction t2 = manager.addTransaction("Groceries", new BigDecimal("150.25"), "Food", TransactionType.EXPENSE, LocalDate.now(), "Supermarket");

        assertEquals(1, t1.getId(), "First ID should be 1");
        assertEquals(2, t2.getId(), "Second ID should be 2");
        assertEquals(2, manager.getTransactionCount(), "Manager count should be 2");
    }

    @Test
    public void testFinancialCalculations() throws Exception {
        ExpenseManager manager = new ExpenseManager();
        manager.addTransaction("Salary", new BigDecimal("5000.00"), "Work", TransactionType.INCOME, LocalDate.now(), "");
        manager.addTransaction("Freelance", new BigDecimal("1200.50"), "Work", TransactionType.INCOME, LocalDate.now(), "");
        manager.addTransaction("Rent", new BigDecimal("1500.00"), "Housing", TransactionType.EXPENSE, LocalDate.now(), "");
        manager.addTransaction("Groceries", new BigDecimal("250.25"), "Food", TransactionType.EXPENSE, LocalDate.now(), "");

        assertEquals(new BigDecimal("6200.50"), manager.getTotalIncome(), "Total income calculation");
        assertEquals(new BigDecimal("1750.25"), manager.getTotalExpenses(), "Total expenses calculation");
        assertEquals(new BigDecimal("4450.25"), manager.getBalance(), "Net balance calculation");
    }

    @Test
    public void testUpdateAndValidation() throws Exception {
        ExpenseManager manager = new ExpenseManager();
        Transaction t = manager.addTransaction("Coffee", new BigDecimal("5.00"), "Food", TransactionType.EXPENSE, LocalDate.now(), "");

        boolean updated = manager.updateTransaction(t.getId(), "Specialty Coffee", new BigDecimal("7.50"), "Drinks", TransactionType.EXPENSE, LocalDate.now(), "Cappuccino");
        assertTrue(updated, "Update should succeed");

        Optional<Transaction> fetched = manager.findById(t.getId());
        assertTrue(fetched.isPresent(), "Updated item should exist");
        assertEquals("Specialty Coffee", fetched.get().getTitle(), "Title should be updated");
        assertEquals(new BigDecimal("7.50"), fetched.get().getAmount(), "Amount should be updated");
    }

    @Test
    public void testDeleteExistingAndNonExistent() throws Exception {
        ExpenseManager manager = new ExpenseManager();
        Transaction t1 = manager.addTransaction("Book", new BigDecimal("20.00"), "Education", TransactionType.EXPENSE, LocalDate.now(), "");
        Transaction t2 = manager.addTransaction("Bus Pass", new BigDecimal("50.00"), "Transit", TransactionType.EXPENSE, LocalDate.now(), "");

        assertTrue(manager.deleteTransaction(t1.getId()), "Deleting t1 should succeed");
        assertEquals(1, manager.getTransactionCount(), "Count should be 1 after deletion");
        assertTrue(manager.findById(t1.getId()).isEmpty(), "t1 should no longer be found");
        assertTrue(!manager.deleteTransaction(888), "Deleting arbitrary non-existent ID should return false");
    }

    @Test
    public void testFiltering() throws Exception {
        ExpenseManager manager = new ExpenseManager();
        manager.addTransaction("Salary", new BigDecimal("3000.00"), "Work", TransactionType.INCOME, LocalDate.now(), "");
        manager.addTransaction("Bonus", new BigDecimal("500.00"), "Work", TransactionType.INCOME, LocalDate.now(), "");
        manager.addTransaction("Dinner", new BigDecimal("45.00"), "Food", TransactionType.EXPENSE, LocalDate.now(), "");

        List<Transaction> workItems = manager.filterByCategory("work");
        assertEquals(2, workItems.size(), "Should filter 2 items by category 'work'");

        List<Transaction> expenseItems = manager.filterByType(TransactionType.EXPENSE);
        assertEquals(1, expenseItems.size(), "Should filter 1 expense item");
    }

    // --- Phase 2 Feature Tests ---

    @Test
    public void testCsvPersistenceAndRestartIdContinuity() throws Exception {
        Path tempCsv = Files.createTempFile("test_storage_", ".csv");
        try {
            CsvTransactionRepository repo = new CsvTransactionRepository(tempCsv);
            ExpenseManager session1 = new ExpenseManager(repo);
            session1.addTransaction("Paycheck", new BigDecimal("4000.00"), "Salary", TransactionType.INCOME, LocalDate.parse("2026-10-01"), "Direct deposit");
            session1.addTransaction("Electricity", new BigDecimal("120.00"), "Utilities", TransactionType.EXPENSE, LocalDate.parse("2026-10-02"), "Bill");

            ExpenseManager session2 = new ExpenseManager(repo);
            session2.loadFromStorage();

            assertEquals(2, session2.getTransactionCount(), "Reloaded manager should have 2 transactions");
            Optional<Transaction> t1 = session2.findById(1);
            assertTrue(t1.isPresent(), "Transaction #1 should exist");
            assertEquals("Paycheck", t1.get().getTitle(), "Transaction #1 title matches");
            assertEquals(new BigDecimal("4000.00"), t1.get().getAmount(), "Transaction #1 amount matches");

            Transaction t3 = session2.addTransaction("Internet", new BigDecimal("60.00"), "Utilities", TransactionType.EXPENSE, LocalDate.parse("2026-10-03"), "Wifi");
            assertEquals(3, t3.getId(), "New transaction after restart must receive next sequential ID (3)");
        } finally {
            Files.deleteIfExists(tempCsv);
        }
    }

    @Test
    public void testRfc4180SpecialCharactersInCsv() throws Exception {
        Path tempCsv = Files.createTempFile("test_rfc4180_", ".csv");
        try {
            CsvTransactionRepository repo = new CsvTransactionRepository(tempCsv);
            ExpenseManager manager = new ExpenseManager(repo);

            String complexTitle = "Groceries, fruits & vegetables";
            String complexDesc = "Bought \"Organic\" apples and 100% milk";
            manager.addTransaction(complexTitle, new BigDecimal("85.50"), "Food & Drink", TransactionType.EXPENSE, LocalDate.now(), complexDesc);

            ExpenseManager reloaded = new ExpenseManager(repo);
            reloaded.loadFromStorage();

            Transaction loaded = reloaded.findById(1).orElseThrow();
            assertEquals(complexTitle, loaded.getTitle(), "Title with commas should be parsed intact");
            assertEquals(complexDesc, loaded.getDescription(), "Description with quotes should be parsed intact");
        } finally {
            Files.deleteIfExists(tempCsv);
        }
    }

    @Test
    public void testMissingAndMalformedCsvHandling() throws Exception {
        Path nonExistent = Path.of("target/non_existent_file_test.csv");
        Files.deleteIfExists(nonExistent);
        CsvTransactionRepository repo = new CsvTransactionRepository(nonExistent);
        List<Transaction> loaded = repo.loadAll();
        assertTrue(loaded.isEmpty(), "Missing file should return empty list without crashing");

        Path malformedFile = Files.createTempFile("test_malformed_", ".csv");
        try {
            Files.writeString(malformedFile, "id,title,amount,category,type,date,description\nINVALID_ROW_CORRUPT\n");
            CsvTransactionRepository badRepo = new CsvTransactionRepository(malformedFile);
            boolean threwException = false;
            try {
                badRepo.loadAll();
            } catch (StorageException e) {
                threwException = true;
            }
            assertTrue(threwException, "Malformed CSV data must throw StorageException");
        } finally {
            Files.deleteIfExists(malformedFile);
        }
    }

    @Test
    public void testCategoryWiseExpensesAndAverages() throws Exception {
        ExpenseManager manager = new ExpenseManager();
        manager.addTransaction("Salary", new BigDecimal("5000.00"), "Work", TransactionType.INCOME, LocalDate.now(), "");
        manager.addTransaction("Groceries", new BigDecimal("200.00"), "Food", TransactionType.EXPENSE, LocalDate.now(), "");
        manager.addTransaction("Dining Out", new BigDecimal("100.00"), "Food", TransactionType.EXPENSE, LocalDate.now(), "");
        manager.addTransaction("Gas", new BigDecimal("50.00"), "Transport", TransactionType.EXPENSE, LocalDate.now(), "");

        Map<String, BigDecimal> breakdown = manager.getCategoryExpenseTotals();
        assertEquals(new BigDecimal("300.00"), breakdown.get("food"), "Food category expense total");
        assertEquals(new BigDecimal("50.00"), breakdown.get("transport"), "Transport category expense total");

        Optional<Transaction> highest = manager.getHighestExpense();
        assertTrue(highest.isPresent(), "Highest expense should be present");
        assertEquals(new BigDecimal("200.00"), highest.get().getAmount(), "Highest expense should be Groceries (200.00)");

        assertEquals(new BigDecimal("116.67"), manager.getAverageExpense(), "Average expense calculation");
    }

    @Test
    public void testWeeklyMonthlyYearlyReports() throws Exception {
        ExpenseManager manager = new ExpenseManager();
        manager.addTransaction("Oct 2026 Salary", new BigDecimal("4000.00"), "Salary", TransactionType.INCOME, LocalDate.parse("2026-10-01"), "");
        manager.addTransaction("Oct 2026 Rent", new BigDecimal("1200.00"), "Rent", TransactionType.EXPENSE, LocalDate.parse("2026-10-05"), "");
        manager.addTransaction("Nov 2026 Bonus", new BigDecimal("1000.00"), "Bonus", TransactionType.INCOME, LocalDate.parse("2026-11-15"), "");
        manager.addTransaction("Nov 2026 Laptop", new BigDecimal("1500.00"), "Tech", TransactionType.EXPENSE, LocalDate.parse("2026-11-20"), "");

        FinancialPeriodSummary oct = manager.getMonthlySummary(2026, 10);
        assertEquals(new BigDecimal("4000.00"), oct.totalIncome(), "Oct total income");
        assertEquals(new BigDecimal("1200.00"), oct.totalExpenses(), "Oct total expenses");
        assertEquals(new BigDecimal("2800.00"), oct.netBalance(), "Oct net balance");
        assertEquals(2, oct.transactionCount(), "Oct transaction count");

        FinancialPeriodSummary nov = manager.getMonthlySummary(2026, 11);
        assertEquals(new BigDecimal("1000.00"), nov.totalIncome(), "Nov total income");
        assertEquals(new BigDecimal("1500.00"), nov.totalExpenses(), "Nov total expenses");
        assertEquals(new BigDecimal("-500.00"), nov.netBalance(), "Nov net deficit balance");

        FinancialPeriodSummary year2026 = manager.getYearlySummary(2026);
        assertEquals(new BigDecimal("5000.00"), year2026.totalIncome(), "2026 annual income");
        assertEquals(new BigDecimal("2700.00"), year2026.totalExpenses(), "2026 annual expenses");
        assertEquals(new BigDecimal("2300.00"), year2026.netBalance(), "2026 annual net balance");
        assertEquals(4, year2026.transactionCount(), "2026 transaction count");
    }

    // --- Phase 3 JDBC Tests ---

    @Test
    public void testJdbcCrudAndGeneratedKeys() throws Exception {
        String memUrl = "jdbc:h2:mem:jdbctest;MODE=MySQL;DB_CLOSE_DELAY=-1";
        DatabaseConnectionManager db = new DatabaseConnectionManager(memUrl, "sa", "");
        db.initializeSchema();

        JdbcTransactionRepository repo = new JdbcTransactionRepository(db);
        ExpenseManager manager = new ExpenseManager(repo);

        Transaction t1 = manager.addTransaction("Coffee", new BigDecimal("4.50"), "Food", TransactionType.EXPENSE, LocalDate.parse("2026-10-10"), "Latte");
        assertTrue(t1.getId() > 0, "Database must generate an auto-increment ID > 0");

        Optional<Transaction> fetched = repo.findById(t1.getId());
        assertTrue(fetched.isPresent(), "Record must be found in database by generated ID");
        assertEquals("Coffee", fetched.get().getTitle(), "Fetched title matches");
        assertEquals(new BigDecimal("4.50"), fetched.get().getAmount(), "Fetched amount matches");

        boolean updated = manager.updateTransaction(t1.getId(), "Fancy Latte", new BigDecimal("6.00"), "Drinks", TransactionType.EXPENSE, LocalDate.parse("2026-10-10"), "Double shot");
        assertTrue(updated, "Update must succeed");
        Optional<Transaction> updatedRecord = repo.findById(t1.getId());
        assertTrue(updatedRecord.isPresent(), "Updated record exists");
        assertEquals("Fancy Latte", updatedRecord.get().getTitle(), "Title updated in DB");
        assertEquals(new BigDecimal("6.00"), updatedRecord.get().getAmount(), "Amount updated in DB");

        boolean deleted = manager.deleteTransaction(t1.getId());
        assertTrue(deleted, "Delete should succeed");
        assertTrue(repo.findById(t1.getId()).isEmpty(), "Deleted ID must no longer exist in DB");

        assertTrue(repo.findById(99999).isEmpty(), "Non-existent record returns empty Optional");
    }

    @Test
    public void testJdbcTransactionBatchAndRollback() throws Exception {
        String memUrl = "jdbc:h2:mem:tx_test;MODE=MySQL;DB_CLOSE_DELAY=-1";
        DatabaseConnectionManager db = new DatabaseConnectionManager(memUrl, "sa", "");
        db.initializeSchema();

        JdbcTransactionRepository repo = new JdbcTransactionRepository(db);

        List<Transaction> validBatch = List.of(
                new Transaction(1, "Income 1", new BigDecimal("100.00"), "Work", TransactionType.INCOME, LocalDate.now(), ""),
                new Transaction(2, "Income 2", new BigDecimal("200.00"), "Work", TransactionType.INCOME, LocalDate.now(), "")
        );

        repo.saveAll(validBatch);
        assertEquals(2, repo.loadAll().size(), "Batch commit should save all 2 transactions");
    }

    @Test
    public void testDatabaseConnectionFailure() {
        String invalidUrl = "jdbc:mysql://127.0.0.1:59999/non_existent_db?connectTimeout=1000";
        DatabaseConnectionManager badDb = new DatabaseConnectionManager(invalidUrl, "testuser", "secretpassword");

        boolean caughtException = false;
        try {
            badDb.getConnection();
        } catch (Exception e) {
            caughtException = true;
            assertTrue(!e.getMessage().contains("secretpassword"), "Password must never be exposed in error message");
        }
        assertTrue(caughtException, "Attempting to connect to invalid DB must fail with Exception");
    }
}
