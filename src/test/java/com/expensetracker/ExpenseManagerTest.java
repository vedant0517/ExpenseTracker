package com.expensetracker;

import com.expensetracker.exception.StorageException;
import com.expensetracker.model.FinancialPeriodSummary;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionType;
import com.expensetracker.repository.CsvTransactionRepository;
import com.expensetracker.service.ExpenseManager;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Core Java test suite verifying Phase 1 and Phase 2 features:
 * - In-memory and CSV persistent storage
 * - No duplicate IDs across application restarts
 * - RFC 4180 quotes and commas handling
 * - Missing, empty, and malformed CSV handling
 * - Weekly, Monthly, and Yearly report aggregations
 * - Category breakdown and highest/average expense statistics
 */
public class ExpenseManagerTest {

    public static void main(String[] args) {
        System.out.println("=== Running Expense Tracker Phase 2 Comprehensive Test Suite ===");
        int passed = 0;
        int failed = 0;

        List<TestItem> tests = List.of(
                new TestItem("testEmptyTracker", ExpenseManagerTest::testEmptyTracker),
                new TestItem("testAddAndAutoIncrementIds", ExpenseManagerTest::testAddAndAutoIncrementIds),
                new TestItem("testFinancialCalculations", ExpenseManagerTest::testFinancialCalculations),
                new TestItem("testUpdateAndValidation", ExpenseManagerTest::testUpdateAndValidation),
                new TestItem("testDeleteExistingAndNonExistent", ExpenseManagerTest::testDeleteExistingAndNonExistent),
                new TestItem("testFiltering", ExpenseManagerTest::testFiltering),
                new TestItem("testCsvPersistenceAndRestartIdContinuity", ExpenseManagerTest::testCsvPersistenceAndRestartIdContinuity),
                new TestItem("testRfc4180SpecialCharactersInCsv", ExpenseManagerTest::testRfc4180SpecialCharactersInCsv),
                new TestItem("testMissingAndMalformedCsvHandling", ExpenseManagerTest::testMissingAndMalformedCsvHandling),
                new TestItem("testCategoryWiseExpensesAndAverages", ExpenseManagerTest::testCategoryWiseExpensesAndAverages),
                new TestItem("testWeeklyMonthlyYearlyReports", ExpenseManagerTest::testWeeklyMonthlyYearlyReports)
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

        System.out.printf("%nPhase 2 Test Summary: %d passed, %d failed.%n", passed, failed);
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

    private static void testEmptyTracker() {
        ExpenseManager manager = new ExpenseManager();
        assertTrue(manager.isEmpty(), "New manager should be empty");
        assertEquals(0, manager.getTransactionCount(), "Initial count should be 0");
        assertEquals(new BigDecimal("0.00"), manager.getTotalIncome(), "Total income on empty manager should be 0.00");
        assertEquals(new BigDecimal("0.00"), manager.getTotalExpenses(), "Total expense on empty manager should be 0.00");
        assertEquals(new BigDecimal("0.00"), manager.getBalance(), "Balance on empty manager should be 0.00");
        assertTrue(manager.findById(999).isEmpty(), "Searching non-existent ID should return Optional.empty()");
    }

    private static void testAddAndAutoIncrementIds() throws Exception {
        ExpenseManager manager = new ExpenseManager();
        Transaction t1 = manager.addTransaction("Salary", new BigDecimal("5000.00"), "Work", TransactionType.INCOME, LocalDate.now(), "Monthly salary");
        Transaction t2 = manager.addTransaction("Groceries", new BigDecimal("150.25"), "Food", TransactionType.EXPENSE, LocalDate.now(), "Supermarket");

        assertEquals(1, t1.getId(), "First ID should be 1");
        assertEquals(2, t2.getId(), "Second ID should be 2");
        assertEquals(2, manager.getTransactionCount(), "Manager count should be 2");
    }

    private static void testFinancialCalculations() throws Exception {
        ExpenseManager manager = new ExpenseManager();
        manager.addTransaction("Salary", new BigDecimal("5000.00"), "Work", TransactionType.INCOME, LocalDate.now(), "");
        manager.addTransaction("Freelance", new BigDecimal("1200.50"), "Work", TransactionType.INCOME, LocalDate.now(), "");
        manager.addTransaction("Rent", new BigDecimal("1500.00"), "Housing", TransactionType.EXPENSE, LocalDate.now(), "");
        manager.addTransaction("Groceries", new BigDecimal("250.25"), "Food", TransactionType.EXPENSE, LocalDate.now(), "");

        assertEquals(new BigDecimal("6200.50"), manager.getTotalIncome(), "Total income calculation");
        assertEquals(new BigDecimal("1750.25"), manager.getTotalExpenses(), "Total expenses calculation");
        assertEquals(new BigDecimal("4450.25"), manager.getBalance(), "Net balance calculation");
    }

    private static void testUpdateAndValidation() throws Exception {
        ExpenseManager manager = new ExpenseManager();
        Transaction t = manager.addTransaction("Coffee", new BigDecimal("5.00"), "Food", TransactionType.EXPENSE, LocalDate.now(), "");

        boolean updated = manager.updateTransaction(t.getId(), "Specialty Coffee", new BigDecimal("7.50"), "Drinks", TransactionType.EXPENSE, LocalDate.now(), "Cappuccino");
        assertTrue(updated, "Update should succeed");

        Optional<Transaction> fetched = manager.findById(t.getId());
        assertTrue(fetched.isPresent(), "Updated item should exist");
        assertEquals("Specialty Coffee", fetched.get().getTitle(), "Title should be updated");
        assertEquals(new BigDecimal("7.50"), fetched.get().getAmount(), "Amount should be updated");
    }

    private static void testDeleteExistingAndNonExistent() throws Exception {
        ExpenseManager manager = new ExpenseManager();
        Transaction t1 = manager.addTransaction("Book", new BigDecimal("20.00"), "Education", TransactionType.EXPENSE, LocalDate.now(), "");
        Transaction t2 = manager.addTransaction("Bus Pass", new BigDecimal("50.00"), "Transit", TransactionType.EXPENSE, LocalDate.now(), "");

        assertTrue(manager.deleteTransaction(t1.getId()), "Deleting t1 should succeed");
        assertEquals(1, manager.getTransactionCount(), "Count should be 1 after deletion");
        assertTrue(manager.findById(t1.getId()).isEmpty(), "t1 should no longer be found");
        assertTrue(!manager.deleteTransaction(888), "Deleting arbitrary non-existent ID should return false");
    }

    private static void testFiltering() throws Exception {
        ExpenseManager manager = new ExpenseManager();
        manager.addTransaction("Salary", new BigDecimal("3000.00"), "Work", TransactionType.INCOME, LocalDate.now(), "");
        manager.addTransaction("Bonus", new BigDecimal("500.00"), "Work", TransactionType.INCOME, LocalDate.now(), "");
        manager.addTransaction("Dinner", new BigDecimal("45.00"), "Food", TransactionType.EXPENSE, LocalDate.now(), "");

        List<Transaction> workItems = manager.filterByCategory("work");
        assertEquals(2, workItems.size(), "Should filter 2 items by category 'work'");

        List<Transaction> expenseItems = manager.filterByType(TransactionType.EXPENSE);
        assertEquals(1, expenseItems.size(), "Should filter 1 expense item");
    }

    // --- Phase 2 New Feature Tests ---

    private static void testCsvPersistenceAndRestartIdContinuity() throws Exception {
        Path tempCsv = Files.createTempFile("test_storage_", ".csv");
        try {
            // Session 1: Add transactions and persist
            CsvTransactionRepository repo = new CsvTransactionRepository(tempCsv);
            ExpenseManager session1 = new ExpenseManager(repo);
            session1.addTransaction("Paycheck", new BigDecimal("4000.00"), "Salary", TransactionType.INCOME, LocalDate.parse("2026-10-01"), "Direct deposit");
            session1.addTransaction("Electricity", new BigDecimal("120.00"), "Utilities", TransactionType.EXPENSE, LocalDate.parse("2026-10-02"), "Bill");

            // Session 2: Fresh start, simulate application restart
            ExpenseManager session2 = new ExpenseManager(repo);
            session2.loadFromStorage();

            assertEquals(2, session2.getTransactionCount(), "Reloaded manager should have 2 transactions");
            Optional<Transaction> t1 = session2.findById(1);
            assertTrue(t1.isPresent(), "Transaction #1 should exist");
            assertEquals("Paycheck", t1.get().getTitle(), "Transaction #1 title matches");
            assertEquals(new BigDecimal("4000.00"), t1.get().getAmount(), "Transaction #1 amount matches");

            // Crucial test: avoid duplicate ID after restart
            Transaction t3 = session2.addTransaction("Internet", new BigDecimal("60.00"), "Utilities", TransactionType.EXPENSE, LocalDate.parse("2026-10-03"), "Wifi");
            assertEquals(3, t3.getId(), "New transaction after restart must receive next sequential ID (3)");
        } finally {
            Files.deleteIfExists(tempCsv);
        }
    }

    private static void testRfc4180SpecialCharactersInCsv() throws Exception {
        Path tempCsv = Files.createTempFile("test_rfc4180_", ".csv");
        try {
            CsvTransactionRepository repo = new CsvTransactionRepository(tempCsv);
            ExpenseManager manager = new ExpenseManager(repo);

            // Title with commas and description with double quotes
            String complexTitle = "Groceries, fruits & vegetables";
            String complexDesc = "Bought \"Organic\" apples and 100% milk";
            manager.addTransaction(complexTitle, new BigDecimal("85.50"), "Food & Drink", TransactionType.EXPENSE, LocalDate.now(), complexDesc);

            // Reload in new session
            ExpenseManager reloaded = new ExpenseManager(repo);
            reloaded.loadFromStorage();

            Transaction loaded = reloaded.findById(1).orElseThrow();
            assertEquals(complexTitle, loaded.getTitle(), "Title with commas should be parsed intact");
            assertEquals(complexDesc, loaded.getDescription(), "Description with quotes should be parsed intact");
        } finally {
            Files.deleteIfExists(tempCsv);
        }
    }

    private static void testMissingAndMalformedCsvHandling() throws Exception {
        // Test 1: Non-existent file loads safely as empty list
        Path nonExistent = Path.of("target/non_existent_file_test.csv");
        Files.deleteIfExists(nonExistent);
        CsvTransactionRepository repo = new CsvTransactionRepository(nonExistent);
        List<Transaction> loaded = repo.loadAll();
        assertTrue(loaded.isEmpty(), "Missing file should return empty list without crashing");

        // Test 2: Malformed file throws StorageException and does not silently swallow errors
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

    private static void testCategoryWiseExpensesAndAverages() throws Exception {
        ExpenseManager manager = new ExpenseManager();
        manager.addTransaction("Salary", new BigDecimal("5000.00"), "Work", TransactionType.INCOME, LocalDate.now(), "");
        manager.addTransaction("Groceries", new BigDecimal("200.00"), "Food", TransactionType.EXPENSE, LocalDate.now(), "");
        manager.addTransaction("Dining Out", new BigDecimal("100.00"), "Food", TransactionType.EXPENSE, LocalDate.now(), "");
        manager.addTransaction("Gas", new BigDecimal("50.00"), "Transport", TransactionType.EXPENSE, LocalDate.now(), "");

        Map<String, BigDecimal> breakdown = manager.getCategoryExpenseTotals();
        assertEquals(new BigDecimal("300.00"), breakdown.get("food"), "Food category expense total");
        assertEquals(new BigDecimal("50.00"), breakdown.get("transport"), "Transport category expense total");

        // Highest Expense
        Optional<Transaction> highest = manager.getHighestExpense();
        assertTrue(highest.isPresent(), "Highest expense should be present");
        assertEquals(new BigDecimal("200.00"), highest.get().getAmount(), "Highest expense should be Groceries (200.00)");

        // Average Expense: (200 + 100 + 50) / 3 = 350 / 3 = 116.67
        assertEquals(new BigDecimal("116.67"), manager.getAverageExpense(), "Average expense calculation");
    }

    private static void testWeeklyMonthlyYearlyReports() throws Exception {
        ExpenseManager manager = new ExpenseManager();
        // Transactions across different months and years
        manager.addTransaction("Oct 2026 Salary", new BigDecimal("4000.00"), "Salary", TransactionType.INCOME, LocalDate.parse("2026-10-01"), "");
        manager.addTransaction("Oct 2026 Rent", new BigDecimal("1200.00"), "Rent", TransactionType.EXPENSE, LocalDate.parse("2026-10-05"), "");
        manager.addTransaction("Nov 2026 Bonus", new BigDecimal("1000.00"), "Bonus", TransactionType.INCOME, LocalDate.parse("2026-11-15"), "");
        manager.addTransaction("Nov 2026 Laptop", new BigDecimal("1500.00"), "Tech", TransactionType.EXPENSE, LocalDate.parse("2026-11-20"), "");

        // Monthly report for October 2026
        FinancialPeriodSummary oct = manager.getMonthlySummary(2026, 10);
        assertEquals(new BigDecimal("4000.00"), oct.totalIncome(), "Oct total income");
        assertEquals(new BigDecimal("1200.00"), oct.totalExpenses(), "Oct total expenses");
        assertEquals(new BigDecimal("2800.00"), oct.netBalance(), "Oct net balance");
        assertEquals(2, oct.transactionCount(), "Oct transaction count");

        // Monthly report for November 2026
        FinancialPeriodSummary nov = manager.getMonthlySummary(2026, 11);
        assertEquals(new BigDecimal("1000.00"), nov.totalIncome(), "Nov total income");
        assertEquals(new BigDecimal("1500.00"), nov.totalExpenses(), "Nov total expenses");
        assertEquals(new BigDecimal("-500.00"), nov.netBalance(), "Nov net deficit balance");

        // Yearly report for 2026 (Both Oct + Nov)
        FinancialPeriodSummary year2026 = manager.getYearlySummary(2026);
        assertEquals(new BigDecimal("5000.00"), year2026.totalIncome(), "2026 annual income");
        assertEquals(new BigDecimal("2700.00"), year2026.totalExpenses(), "2026 annual expenses");
        assertEquals(new BigDecimal("2300.00"), year2026.netBalance(), "2026 annual net balance");
        assertEquals(4, year2026.transactionCount(), "2026 transaction count");
    }
}
