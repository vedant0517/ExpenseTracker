package com.expensetracker.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a single financial transaction (Income or Expense).
 * 
 * Demonstrates encapsulation with private fields, validation
 * guards in constructors and setters, and proper data types.
 */
public class Transaction {
    private final int id;
    private String title;
    private BigDecimal amount;
    private String category;
    private TransactionType type;
    private LocalDate date;
    private String description;

    /**
     * Constructs a validated Transaction instance.
     *
     * @param id          Unique transaction identifier (positive integer)
     * @param title       Short title/label (non-empty)
     * @param amount      Monetary amount (must be strictly greater than zero)
     * @param category    Expense/Income category (non-empty)
     * @param type        INCOME or EXPENSE (non-null)
     * @param date        Transaction date (non-null)
     * @param description Optional extra details
     * @throws IllegalArgumentException if any validation rule is violated
     */
    public Transaction(int id, String title, BigDecimal amount, String category,
                       TransactionType type, LocalDate date, String description) {
        if (id <= 0) {
            throw new IllegalArgumentException("Transaction ID must be a positive integer.");
        }
        this.id = id;
        setTitle(title);
        setAmount(amount);
        setCategory(category);
        setType(type);
        setDate(date);
        setDescription(description);
    }

    // --- Getters ---

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public TransactionType getType() {
        return type;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    // --- Setters with Input Validation ---

    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty.");
        }
        this.title = title.trim();
    }

    public void setAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        // Normalize amount to 2 decimal places (standard currency format)
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public void setCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be null or empty.");
        }
        this.category = category.trim();
    }

    public void setType(TransactionType type) {
        if (type == null) {
            throw new IllegalArgumentException("Transaction type cannot be null.");
        }
        this.type = type;
    }

    public void setDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null.");
        }
        this.date = date;
    }

    public void setDescription(String description) {
        // Optional field: null or empty gets converted to empty string for safe printing
        this.description = (description == null) ? "" : description.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
            "Transaction #%-4d | %-10s | %-7s | %10.2f | %-12s | %s%s",
            id,
            date,
            type,
            amount,
            category,
            title,
            description.isEmpty() ? "" : " (" + description + ")"
        );
    }
}
