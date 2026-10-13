package com.expensetracker;

import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionType;
import com.expensetracker.service.ExpenseManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Pure Core Java test suite testing ExpenseManager without external dependencies.
 * Tests normal cases, boundary conditions, and edge cases.
 */
public class ExpenseManagerTest {

    public static void main(String[] args) {
        System.out.println("=== Running ExpenseManager Core Java Test Suite ===");
        int passed = 0;
        int failed = 0;

        try {
            testEmptyTracker();
            passed++;
            System.out.println("✔ PASS: testEmptyTracker");
        } catch (Throwable t) {
            failed++;
            System.err.println("✘ FAIL: testEmptyTracker - " + t.getMessage());
        }

        try {
            testAddAndAutoIncrementIds();
            passed++;
            System.out.println("✔ PASS: testAddAndAutoIncrementIds");
        } catch (Throwable t) {
            failed++;
            System.err.println("✘ FAIL: testAddAndAutoIncrementIds - " + t.getMessage());
        }

        try {
            testFinancialCalculations();
            passed++;
            System.out.println("✔ PASS: testFinancialCalculations");
        } catch (Throwable t) {
            failed++;
            System.err.println("✘ FAIL: testFinancialCalculations - " + t.getMessage());
        }

        try {
            testUpdateAndValidation();
            passed++;
            System.out.println("✔ PASS: testUpdateAndValidation");
        } catch (Throwable t) {
            failed++;
            System.err.println("✘ FAIL: testUpdateAndValidation - " + t.getMessage());
        }

        try {
            testDeleteExistingAndNonExistent();
            passed++;
            System.out.println("✔ PASS: testDeleteExistingAndNonExistent");
        } catch (Throwable t) {
            failed++;
            System.err.println("✘ FAIL: testDeleteExistingAndNonExistent - " + t.getMessage());
        }

        try {
            testFiltering();
            passed++;
            System.out.println("✔ PASS: testFiltering");
        } catch (Throwable t) {
            failed++;
            System.err.println("✘ FAIL: testFiltering - " + t.getMessage());
        }

        System.out.printf("%nTest Summary: %d passed, %d failed.%n", passed, failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError(message + " [Expected: " + expected + ", Actual: " + actual + "]");
        }
    }

    private static void testEmptyTracker() {
        ExpenseManager manager = new ExpenseManager();
        assertTrue(manager.isEmpty(), "New manager should be empty");
        assertEquals(0, manager.getTransactionCount(), "Initial count should be 0");
        assertEquals(new BigDecimal("0.00"), manager.getTotalIncome(), "Total income on empty manager should be 0.00");
        assertEquals(new BigDecimal("0.00"), manager.getTotalExpenses(), "Total expense on empty manager should be 0.00");
        assertEquals(new BigDecimal("0.00"), manager.getBalance(), "Balance on empty manager should be 0.00");
        assertTrue(manager.findById(999).isEmpty(), "Searching non-existent ID should return Optional.empty()");
        assertTrue(!manager.deleteTransaction(999), "Deleting non-existent ID should return false");
    }

    private static void testAddAndAutoIncrementIds() {
        ExpenseManager manager = new ExpenseManager();
        Transaction t1 = manager.addTransaction("Salary", new BigDecimal("5000.00"), "Work", TransactionType.INCOME, LocalDate.now(), "Monthly salary");
        Transaction t2 = manager.addTransaction("Groceries", new BigDecimal("150.25"), "Food", TransactionType.EXPENSE, LocalDate.now(), "Supermarket");

        assertEquals(1, t1.getId(), "First ID should be 1");
        assertEquals(2, t2.getId(), "Second ID should be 2");
        assertEquals(2, manager.getTransactionCount(), "Manager count should be 2");
    }

    private static void testFinancialCalculations() {
        ExpenseManager manager = new ExpenseManager();
        manager.addTransaction("Salary", new BigDecimal("5000.00"), "Work", TransactionType.INCOME, LocalDate.now(), "");
        manager.addTransaction("Freelance", new BigDecimal("1200.50"), "Work", TransactionType.INCOME, LocalDate.now(), "");
        manager.addTransaction("Rent", new BigDecimal("1500.00"), "Housing", TransactionType.EXPENSE, LocalDate.now(), "");
        manager.addTransaction("Groceries", new BigDecimal("250.25"), "Food", TransactionType.EXPENSE, LocalDate.now(), "");

        // Expected Income: 5000.00 + 1200.50 = 6200.50
        // Expected Expense: 1500.00 + 250.25 = 1750.25
        // Expected Balance: 6200.50 - 1750.25 = 4450.25
        assertEquals(new BigDecimal("6200.50"), manager.getTotalIncome(), "Total income calculation");
        assertEquals(new BigDecimal("1750.25"), manager.getTotalExpenses(), "Total expenses calculation");
        assertEquals(new BigDecimal("4450.25"), manager.getBalance(), "Net balance calculation");
    }

    private static void testUpdateAndValidation() {
        ExpenseManager manager = new ExpenseManager();
        Transaction t = manager.addTransaction("Coffee", new BigDecimal("5.00"), "Food", TransactionType.EXPENSE, LocalDate.now(), "");

        boolean updated = manager.updateTransaction(t.getId(), "Specialty Coffee", new BigDecimal("7.50"), "Drinks", TransactionType.EXPENSE, LocalDate.now(), "Cappuccino");
        assertTrue(updated, "Update should succeed");

        Optional<Transaction> fetched = manager.findById(t.getId());
        assertTrue(fetched.isPresent(), "Updated item should exist");
        assertEquals("Specialty Coffee", fetched.get().getTitle(), "Title should be updated");
        assertEquals(new BigDecimal("7.50"), fetched.get().getAmount(), "Amount should be updated");
        assertEquals("Drinks", fetched.get().getCategory(), "Category should be updated");

        // Try updating non-existent ID
        boolean nonExistentUpdate = manager.updateTransaction(9999, "Test", new BigDecimal("10.00"), "Test", TransactionType.INCOME, LocalDate.now(), "");
        assertTrue(!nonExistentUpdate, "Updating non-existent ID should return false");

        // Try updating with invalid amount (must throw IllegalArgumentException)
        boolean caughtException = false;
        try {
            manager.updateTransaction(t.getId(), "Negative", new BigDecimal("-10.00"), "Food", TransactionType.EXPENSE, LocalDate.now(), "");
        } catch (IllegalArgumentException e) {
            caughtException = true;
        }
        assertTrue(caughtException, "Negative amount in update should throw IllegalArgumentException");
    }

    private static void testDeleteExistingAndNonExistent() {
        ExpenseManager manager = new ExpenseManager();
        Transaction t1 = manager.addTransaction("Book", new BigDecimal("20.00"), "Education", TransactionType.EXPENSE, LocalDate.now(), "");
        Transaction t2 = manager.addTransaction("Bus Pass", new BigDecimal("50.00"), "Transit", TransactionType.EXPENSE, LocalDate.now(), "");

        assertTrue(manager.deleteTransaction(t1.getId()), "Deleting t1 should succeed");
        assertEquals(1, manager.getTransactionCount(), "Count should be 1 after deletion");
        assertTrue(manager.findById(t1.getId()).isEmpty(), "t1 should no longer be found");
        assertTrue(!manager.deleteTransaction(t1.getId()), "Deleting t1 again should return false");
        assertTrue(!manager.deleteTransaction(888), "Deleting arbitrary non-existent ID should return false");
    }

    private static void testFiltering() {
        ExpenseManager manager = new ExpenseManager();
        manager.addTransaction("Salary", new BigDecimal("3000.00"), "Work", TransactionType.INCOME, LocalDate.now(), "");
        manager.addTransaction("Bonus", new BigDecimal("500.00"), "Work", TransactionType.INCOME, LocalDate.now(), "");
        manager.addTransaction("Dinner", new BigDecimal("45.00"), "Food", TransactionType.EXPENSE, LocalDate.now(), "");

        List<Transaction> workItems = manager.filterByCategory("work");
        assertEquals(2, workItems.size(), "Should filter 2 items by category 'work'");

        List<Transaction> expenseItems = manager.filterByType(TransactionType.EXPENSE);
        assertEquals(1, expenseItems.size(), "Should filter 1 expense item");

        List<Transaction> emptyResult = manager.filterByCategory("NonExistent");
        assertEquals(0, emptyResult.size(), "Non-existent category filter should return empty list");
    }
}
