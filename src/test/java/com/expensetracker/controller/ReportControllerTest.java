package com.expensetracker.controller;

import com.expensetracker.dto.TransactionRequestDto;
import com.expensetracker.entity.User;
import com.expensetracker.model.TransactionType;
import com.expensetracker.repository.UserJpaRepository;
import com.expensetracker.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test suite for ReportController testing financial summary and category breakdown endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByUsername("alice")) {
            userRepository.save(new User("alice", "alice@example.com", passwordEncoder.encode("password123")));
        }
    }

    @Test
    @WithMockUser(username = "alice")
    void testGetFinancialSummary_Success() throws Exception {
        // Create an income and an expense transaction for alice
        TransactionRequestDto income = new TransactionRequestDto(
                "Salary", new BigDecimal("5000.00"), "Work", TransactionType.INCOME, LocalDate.of(2026, 10, 1), ""
        );
        TransactionRequestDto expense = new TransactionRequestDto(
                "Rent", new BigDecimal("1200.00"), "Housing", TransactionType.EXPENSE, LocalDate.of(2026, 10, 5), ""
        );
        transactionService.createTransaction(income);
        transactionService.createTransaction(expense);

        mockMvc.perform(get("/api/reports/summary?startDate=2026-10-01&endDate=2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(greaterThanOrEqualTo(5000.00)))
                .andExpect(jsonPath("$.totalExpenses").value(greaterThanOrEqualTo(1200.00)))
                .andExpect(jsonPath("$.netBalance").exists())
                .andExpect(jsonPath("$.transactionCount").isNumber())
                .andExpect(jsonPath("$.highestExpense").value(greaterThanOrEqualTo(1200.00)))
                .andExpect(jsonPath("$.averageExpense").isNumber());
    }

    @Test
    @WithMockUser(username = "alice")
    void testGetCategoryExpenses_Success() throws Exception {
        TransactionRequestDto expense = new TransactionRequestDto(
                "Groceries", new BigDecimal("250.00"), "Food", TransactionType.EXPENSE, LocalDate.of(2026, 10, 10), ""
        );
        transactionService.createTransaction(expense);

        mockMvc.perform(get("/api/reports/categories?startDate=2026-10-01&endDate=2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.category == 'Food')].totalAmount").exists());
    }

    @Test
    @WithMockUser(username = "alice")
    void testGetFinancialSummary_InvalidDateRange_Returns400() throws Exception {
        mockMvc.perform(get("/api/reports/summary?startDate=2026-10-31&endDate=2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("startDate cannot be after endDate")));
    }
}
