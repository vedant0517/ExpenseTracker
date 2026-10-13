package com.expensetracker.dto;

import com.expensetracker.entity.Transaction;
import com.expensetracker.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Immutable response DTO representing a transaction returned to clients.
 *
 * Implemented as a Java 17 record. Shields internal entity structure
 * from public API contracts.
 */
public record TransactionResponseDto(
        Long id,
        String title,
        BigDecimal amount,
        String category,
        TransactionType type,
        LocalDate date,
        String description
) {
    /**
     * Maps a JPA Transaction entity into an API response DTO.
     */
    public static TransactionResponseDto fromEntity(Transaction entity) {
        if (entity == null) {
            return null;
        }
        return new TransactionResponseDto(
                entity.getId(),
                entity.getTitle(),
                entity.getAmount(),
                entity.getCategory(),
                entity.getType(),
                entity.getDate(),
                entity.getDescription()
        );
    }
}
