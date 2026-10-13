package com.expensetracker.service;

import com.expensetracker.exception.StorageException;
import com.expensetracker.model.FinancialPeriodSummary;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionType;
import com.expensetracker.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class handling all business logic for transactions and financial reporting.
 *
 * Responsibilities:
 * - In-memory transactions management with optional CSV persistence.
 * - Sequential, collision-free transaction ID allocation across restarts.
 * - Robust financial reporting (weekly, monthly, yearly, category aggregations).
 * - Stream-based analytics (highest expense, average expense).
 */
public class ExpenseManager {

    private final List<Transaction> transactions;
    private final TransactionRepository repository;
    private int nextId;

    /**
     * Creates an in-memory ExpenseManager without persistent storage (useful for isolated unit testing).
     */
    public ExpenseManager() {
        this(null);
    }

    /**
     * Creates an ExpenseManager backed by a TransactionRepository.
     * Automatically loads existing transactions and adjusts nextId to avoid duplicate IDs.
     *
     * @param repository Persistence repository, or null for in-memory mode
     */
    public ExpenseManager(TransactionRepository repository) {
        this.transactions = new ArrayList<>();
        this.repository = repository;
        this.nextId = 1;
    }

    /**
     * Loads transactions from storage, setting nextId higher than any loaded ID.
     *
     * @throws StorageException if persistent storage fails to read
     */
    public void loadFromStorage() throws StorageException {
        if (repository == null) {
            return;
        }
        List<Transaction> loaded = repository.loadAll();
        transactions.clear();
        transactions.addAll(loaded);

        // Avoid duplicate transaction IDs when the application restarts
        this.nextId = transactions.stream()
                .mapToInt(Transaction::getId)
                .max()
                .orElse(0) + 1;
    }

    /**
     * Persists the current transaction list to storage.
     */
    public void persist() throws StorageException {
        if (repository != null) {
            repository.saveAll(transactions);
        }
    }

    // =========================================================================
    // Core CRUD Operations
    // =========================================================================

    /**
     * Adds a new transaction, assigns a unique ID, and auto-saves to storage.
     */
    public Transaction addTransaction(String title, BigDecimal amount, String category,
                                      TransactionType type, LocalDate date, String description) throws StorageException {
        Transaction transaction;
        if (repository != null) {
            // When repository is present (e.g. JDBC with AUTO_INCREMENT or CSV),
            // let the repository save and return the persistent entity with generated ID.
            Transaction dummy = new Transaction(nextId, title, amount, category, type, date, description);
            transaction = repository.save(dummy);
            this.nextId = Math.max(this.nextId, transaction.getId() + 1);
            transactions.add(transaction);
        } else {
            int id = nextId++;
            transaction = new Transaction(id, title, amount, category, type, date, description);
            transactions.add(transaction);
        }
        return transaction;
    }

    /**
     * Returns an unmodifiable list of all stored transactions.
     */
    public List<Transaction> getAllTransactions() {
        return Collections.unmodifiableList(new ArrayList<>(transactions));
    }

    /**
     * Finds a transaction by its unique ID.
     */
    public Optional<Transaction> findById(int id) {
        return transactions.stream()
                .filter(t -> t.getId() == id)
                .findFirst();
    }

    /**
     * Updates an existing transaction and saves changes to storage.
     */
    public boolean updateTransaction(int id, String title, BigDecimal amount, String category,
                                     TransactionType type, LocalDate date, String description) throws StorageException {
        Optional<Transaction> optional = findById(id);
        if (optional.isEmpty()) {
            return false;
        }

        Transaction transaction = optional.get();
        transaction.setTitle(title);
        transaction.setAmount(amount);
        transaction.setCategory(category);
        transaction.setType(type);
        transaction.setDate(date);
        transaction.setDescription(description);

        if (repository != null) {
            return repository.update(transaction);
        }
        return true;
    }

    /**
     * Deletes a transaction by ID and persists changes.
     */
    public boolean deleteTransaction(int id) throws StorageException {
        boolean removed = transactions.removeIf(t -> t.getId() == id);
        if (removed && repository != null) {
            return repository.deleteById(id);
        }
        return removed;
    }

    // =========================================================================
    // Core Balance & Filter Calculations
    // =========================================================================

    public BigDecimal getTotalByType(TransactionType type) {
        return getTotalByType(this.transactions, type);
    }

    public BigDecimal getTotalIncome() {
        return getTotalByType(TransactionType.INCOME);
    }

    public BigDecimal getTotalExpenses() {
        return getTotalByType(TransactionType.EXPENSE);
    }

    public BigDecimal getBalance() {
        return getTotalIncome().subtract(getTotalExpenses()).setScale(2, RoundingMode.HALF_UP);
    }

    public List<Transaction> filterByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String normalized = category.trim().toLowerCase();
        return transactions.stream()
                .filter(t -> t.getCategory().toLowerCase().contains(normalized))
                .toList();
    }

    public List<Transaction> filterByType(TransactionType type) {
        if (type == null) {
            return Collections.emptyList();
        }
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .toList();
    }

    public List<Transaction> filter(String category, TransactionType type) {
        return transactions.stream()
                .filter(t -> type == null || t.getType() == type)
                .filter(t -> category == null || category.trim().isEmpty() ||
                             t.getCategory().toLowerCase().contains(category.trim().toLowerCase()))
                .toList();
    }

    public int getTransactionCount() {
        return transactions.size();
    }

    public boolean isEmpty() {
        return transactions.isEmpty();
    }

    // =========================================================================
    // Advanced Reports & Aggregations (Java Streams & Lambdas)
    // =========================================================================

    /**
     * Calculates category-wise expense totals, sorted from highest expense to lowest.
     */
    public Map<String, BigDecimal> getCategoryExpenseTotals() {
        return getCategoryExpenseTotals(this.transactions);
    }

    public Map<String, BigDecimal> getCategoryExpenseTotals(List<Transaction> source) {
        return source.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().toLowerCase(),
                        Collectors.reducing(BigDecimal.ZERO.setScale(2), Transaction::getAmount, BigDecimal::add)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Finds the single highest EXPENSE transaction in the given transaction pool.
     */
    public Optional<Transaction> getHighestExpense() {
        return getHighestExpense(this.transactions);
    }

    public Optional<Transaction> getHighestExpense(List<Transaction> source) {
        return source.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .max(Comparator.comparing(Transaction::getAmount));
    }

    /**
     * Calculates the average amount spent per expense transaction.
     */
    public BigDecimal getAverageExpense() {
        return getAverageExpense(this.transactions);
    }

    public BigDecimal getAverageExpense(List<Transaction> source) {
        List<Transaction> expenses = source.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .toList();

        if (expenses.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }

        BigDecimal sum = expenses.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Generates a comprehensive summary for any arbitrary date range (inclusive).
     */
    public FinancialPeriodSummary getSummaryForDateRange(String label, LocalDate startDate, LocalDate endDate) {
        List<Transaction> inRange = transactions.stream()
                .filter(t -> !t.getDate().isBefore(startDate) && !t.getDate().isAfter(endDate))
                .toList();

        BigDecimal income = getTotalByType(inRange, TransactionType.INCOME);
        BigDecimal expense = getTotalByType(inRange, TransactionType.EXPENSE);
        BigDecimal balance = income.subtract(expense).setScale(2, RoundingMode.HALF_UP);

        int expenseCount = (int) inRange.stream().filter(t -> t.getType() == TransactionType.EXPENSE).count();
        int incomeCount = (int) inRange.stream().filter(t -> t.getType() == TransactionType.INCOME).count();

        return new FinancialPeriodSummary(
                label,
                startDate,
                endDate,
                income,
                expense,
                balance,
                inRange.size(),
                expenseCount,
                incomeCount,
                getHighestExpense(inRange),
                getAverageExpense(inRange),
                getCategoryExpenseTotals(inRange)
        );
    }

    /**
     * Generates a weekly financial summary for the week containing referenceDate (Monday through Sunday).
     */
    public FinancialPeriodSummary getWeeklySummary(LocalDate referenceDate) {
        LocalDate startOfWeek = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = referenceDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        String label = String.format("Week of %s to %s", startOfWeek, endOfWeek);
        return getSummaryForDateRange(label, startOfWeek, endOfWeek);
    }

    /**
     * Generates a monthly financial summary for the specified year and month.
     */
    public FinancialPeriodSummary getMonthlySummary(int year, int month) {
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.with(TemporalAdjusters.lastDayOfMonth());
        String label = String.format("%s %d", startOfMonth.getMonth(), year);
        return getSummaryForDateRange(label, startOfMonth, endOfMonth);
    }

    /**
     * Generates a yearly financial summary for the given year.
     */
    public FinancialPeriodSummary getYearlySummary(int year) {
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 31);
        String label = String.format("Year %d", year);
        return getSummaryForDateRange(label, startOfYear, endOfYear);
    }

    // Helper calculation on arbitrary sub-lists
    private BigDecimal getTotalByType(List<Transaction> list, TransactionType type) {
        if (type == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return list.stream()
                .filter(t -> t.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }
}
