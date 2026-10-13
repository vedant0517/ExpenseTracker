package com.expensetracker.model;

/**
 * Represents the type of a financial transaction.
 * 
 * Using an enum provides type-safety, preventing invalid strings
 * from representing transaction types throughout the application.
 */
public enum TransactionType {
    INCOME,
    EXPENSE;

    /**
     * Parses a string safely into a TransactionType (case-insensitive).
     *
     * @param value raw string value (e.g., "income", "EXPENSE")
     * @return matching TransactionType, or null if invalid
     */
    public static TransactionType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return TransactionType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
