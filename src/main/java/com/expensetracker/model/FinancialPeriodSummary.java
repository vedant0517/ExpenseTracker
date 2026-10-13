package com.expensetracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable report model encapsulating aggregated financial metrics
 * for a specific time period (e.g. Weekly, Monthly, Yearly, Custom).
 *
 * Implemented as a Java 17 record.
 */
public record FinancialPeriodSummary(
        String periodLabel,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal netBalance,
        int transactionCount,
        int expenseCount,
        int incomeCount,
        Optional<Transaction> highestExpense,
        BigDecimal averageExpense,
        Map<String, BigDecimal> categoryExpenses
) {
    /**
     * Compact constructor ensuring BigDecimal scale and non-null defaults.
     */
    public FinancialPeriodSummary {
        totalIncome = totalIncome != null ? totalIncome.setScale(2) : BigDecimal.ZERO.setScale(2);
        totalExpenses = totalExpenses != null ? totalExpenses.setScale(2) : BigDecimal.ZERO.setScale(2);
        netBalance = netBalance != null ? netBalance.setScale(2) : BigDecimal.ZERO.setScale(2);
        averageExpense = averageExpense != null ? averageExpense.setScale(2) : BigDecimal.ZERO.setScale(2);
        highestExpense = highestExpense != null ? highestExpense : Optional.empty();
        categoryExpenses = categoryExpenses != null ? Map.copyOf(categoryExpenses) : Map.of();
    }
}
