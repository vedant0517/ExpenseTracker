package com.expensetracker.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for authenticating an existing user.
 */
public record LoginRequestDto(
        @NotBlank(message = "Username or email is required")
        String usernameOrEmail,

        @NotBlank(message = "Password is required")
        String password
) {}
