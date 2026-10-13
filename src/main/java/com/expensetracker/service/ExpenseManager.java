package com.expensetracker.service;

import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service class handling all business logic for managing transactions.
 * 
 * Adheres to strict separation of concerns:
 * - Maintains in-memory state in an ArrayList.
 * - Auto-generates unique sequential IDs.
 * - Contains ZERO console I/O (no System.out or Scanner).
 * - Exposes pure, testable methods.
 */
public class ExpenseManager {
    private final List<Transaction> transactions;
    private int nextId;

    /**
     * Initializes an empty ExpenseManager with auto-increment ID starting at 1.
     */
    public ExpenseManager() {
        this.transactions = new ArrayList<>();
        this.nextId = 1;
    }

    /**
     * Creates and adds a new transaction with an auto-generated unique ID.
     *
     * @param title       Title of the transaction
     * @param amount      Positive monetary amount
     * @param category    Category (e.g., Food, Salary)
     * @param type        INCOME or EXPENSE
     * @param date        Transaction date
     * @param description Optional description
     * @return The newly created and stored Transaction instance
     */
    public Transaction addTransaction(String title, BigDecimal amount, String category,
                                      TransactionType type, LocalDate date, String description) {
        int id = nextId++;
        Transaction transaction = new Transaction(id, title, amount, category, type, date, description);
        transactions.add(transaction);
        return transaction;
    }

    /**
     * Returns an unmodifiable view of all stored transactions.
     * Defensive design prevents external modification of the internal list.
     */
    public List<Transaction> getAllTransactions() {
        return Collections.unmodifiableList(new ArrayList<>(transactions));
    }

    /**
     * Searches for a transaction by its unique ID.
     *
     * @param id The transaction ID to search for
     * @return Optional containing the transaction if found, or Optional.empty()
     */
    public Optional<Transaction> findById(int id) {
        return transactions.stream()
                .filter(t -> t.getId() == id)
                .findFirst();
    }

    /**
     * Updates an existing transaction.
     *
     * @param id          ID of the transaction to update
     * @param title       New title
     * @param amount      New positive amount
     * @param category    New category
     * @param type        New type
     * @param date        New date
     * @param description New description
     * @return true if updated successfully, false if the transaction ID does not exist
     */
    public boolean updateTransaction(int id, String title, BigDecimal amount, String category,
                                     TransactionType type, LocalDate date, String description) {
        Optional<Transaction> optional = findById(id);
        if (optional.isEmpty()) {
            return false;
        }

        Transaction transaction = optional.get();
        // Setters apply the exact same validation logic
        transaction.setTitle(title);
        transaction.setAmount(amount);
        transaction.setCategory(category);
        transaction.setType(type);
        transaction.setDate(date);
        transaction.setDescription(description);
        return true;
    }

    /**
     * Deletes a transaction by ID.
     *
     * @param id The ID to delete
     * @return true if deleted, false if the ID was not found
     */
    public boolean deleteTransaction(int id) {
        return transactions.removeIf(t -> t.getId() == id);
    }

    /**
     * Calculates the sum of all transactions of the specified type.
     */
    public BigDecimal getTotalByType(TransactionType type) {
        if (type == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    /**
     * Returns the total sum of all INCOME transactions.
     */
    public BigDecimal getTotalIncome() {
        return getTotalByType(TransactionType.INCOME);
    }

    /**
     * Returns the total sum of all EXPENSE transactions.
     */
    public BigDecimal getTotalExpenses() {
        return getTotalByType(TransactionType.EXPENSE);
    }

    /**
     * Calculates current balance: Total Income - Total Expenses.
     */
    public BigDecimal getBalance() {
        return getTotalIncome().subtract(getTotalExpenses()).setScale(2);
    }

    /**
     * Filters transactions by category (case-insensitive substring or exact match).
     */
    public List<Transaction> filterByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String normalized = category.trim().toLowerCase();
        return transactions.stream()
                .filter(t -> t.getCategory().toLowerCase().contains(normalized))
                .toList();
    }

    /**
     * Filters transactions by type (INCOME or EXPENSE).
     */
    public List<Transaction> filterByType(TransactionType type) {
        if (type == null) {
            return Collections.emptyList();
        }
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .toList();
    }

    /**
     * Filters transactions matching both category and type.
     */
    public List<Transaction> filter(String category, TransactionType type) {
        return transactions.stream()
                .filter(t -> type == null || t.getType() == type)
                .filter(t -> category == null || category.trim().isEmpty() ||
                             t.getCategory().toLowerCase().contains(category.trim().toLowerCase()))
                .toList();
    }

    /**
     * Returns the total number of transactions currently stored.
     */
    public int getTransactionCount() {
        return transactions.size();
    }

    /**
     * Checks if the tracker has zero transactions.
     */
    public boolean isEmpty() {
        return transactions.isEmpty();
    }
}
