package com.expensetracker.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Report response DTO containing aggregated financial metrics:
 * total income, total expenses, net balance, and transaction count.
 */
public record FinancialSummaryResponseDto(
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal netBalance,
        long transactionCount,
        BigDecimal highestExpense,
        BigDecimal averageExpense
) {
    public FinancialSummaryResponseDto {
        totalIncome = (totalIncome != null) ? totalIncome.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
        totalExpenses = (totalExpenses != null) ? totalExpenses.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
        netBalance = (netBalance != null) ? netBalance.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
        highestExpense = (highestExpense != null) ? highestExpense.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
        averageExpense = (averageExpense != null) ? averageExpense.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
    }
}
