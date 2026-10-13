package com.expensetracker.dto;

/**
 * Response payload returned after successful user registration or login, containing JWT token.
 */
public record AuthResponseDto(
        String token,
        String tokenType,
        String username,
        String email
) {
    public AuthResponseDto(String token, String username, String email) {
        this(token, "Bearer", username, email);
    }
}
