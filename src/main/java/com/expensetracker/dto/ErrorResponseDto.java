package com.expensetracker.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Structured error response DTO returned on validation errors,
 * resource not found (404), bad requests (400), or server errors (500).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDto(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {
    public static ErrorResponseDto of(int status, String error, String message, String path) {
        return new ErrorResponseDto(LocalDateTime.now(), status, error, message, path, null);
    }

    public static ErrorResponseDto ofValidation(int status, String error, String message, String path, Map<String, String> errors) {
        return new ErrorResponseDto(LocalDateTime.now(), status, error, message, path, errors);
    }
}
