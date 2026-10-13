package com.expensetracker.dto;

import com.expensetracker.model.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for creating and updating a transaction.
 * 
 * Enforces input validation rules using Jakarta Bean Validation:
 * - @NotBlank: Ensures strings are not null and contain at least one non-whitespace character.
 * - @NotNull: Prevents null object references.
 * - @Positive: Rejects zero and negative monetary values.
 * - @PastOrPresent: Ensures transaction dates are not set in the future.
 * - @Size: Bounds maximum character lengths.
 */
public class TransactionRequestDto {

    @NotBlank(message = "Title is required and cannot be blank")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be strictly greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Category is required and cannot be blank")
    @Size(max = 100, message = "Category cannot exceed 100 characters")
    private String category;

    @NotNull(message = "Transaction type must be INCOME or EXPENSE")
    private TransactionType type;

    @NotNull(message = "Date is required (YYYY-MM-DD)")
    @PastOrPresent(message = "Transaction date cannot be in the future")
    private LocalDate date;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    public TransactionRequestDto() {
    }

    public TransactionRequestDto(String title, BigDecimal amount, String category,
                                 TransactionType type, LocalDate date, String description) {
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.type = type;
        this.date = date;
        this.description = description;
    }

    // --- Getters and Setters ---

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
