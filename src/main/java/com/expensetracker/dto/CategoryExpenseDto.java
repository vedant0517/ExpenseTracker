package com.expensetracker.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Report response DTO representing total expenses and percentage share for a category.
 */
public record CategoryExpenseDto(
        String category,
        BigDecimal totalAmount,
        double percentage
) {
    public CategoryExpenseDto {
        totalAmount = (totalAmount != null) ? totalAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
    }
}
