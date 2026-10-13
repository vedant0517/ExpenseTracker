package com.expensetracker.controller;

import com.expensetracker.dto.TransactionRequestDto;
import com.expensetracker.model.TransactionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full integration test suite using Spring Boot MockMvc.
 * Tests HTTP status codes, JSON response shapes, validation errors, and report generation.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateTransactionSuccess() throws Exception {
        TransactionRequestDto request = new TransactionRequestDto(
                "October Salary",
                new BigDecimal("6500.00"),
                "Salary",
                TransactionType.INCOME,
                LocalDate.now(),
                "Direct Deposit"
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("October Salary"))
                .andExpect(jsonPath("$.amount").value(6500.00))
                .andExpect(jsonPath("$.type").value("INCOME"));
    }

    @Test
    void testCreateTransactionValidationError() throws Exception {
        // Invalid request: blank title and negative amount
        TransactionRequestDto invalidRequest = new TransactionRequestDto(
                "",
                new BigDecimal("-50.00"),
                "",
                TransactionType.EXPENSE,
                LocalDate.now(),
                ""
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.title").exists())
                .andExpect(jsonPath("$.validationErrors.amount").exists())
                .andExpect(jsonPath("$.validationErrors.category").exists());
    }

    @Test
    void testGetTransactionsPagination() throws Exception {
        mockMvc.perform(get("/api/transactions?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable").exists());
    }

    @Test
    void testGetNonExistentTransactionReturns404() throws Exception {
        mockMvc.perform(get("/api/transactions/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void testGetReportsSummaryAndCategories() throws Exception {
        // Verify summary endpoint
        mockMvc.perform(get("/api/reports/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").exists())
                .andExpect(jsonPath("$.totalExpenses").exists())
                .andExpect(jsonPath("$.netBalance").exists());

        // Verify categories endpoint
        mockMvc.perform(get("/api/reports/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
