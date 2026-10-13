package com.expensetracker.controller;

import com.expensetracker.dto.CategoryExpenseDto;
import com.expensetracker.dto.FinancialSummaryResponseDto;
import com.expensetracker.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-focused tests using @WebMvcTest for ReportController.
 * Verifies HTTP routing, parameter binding, and error mapping in isolation from the full Spring context.
 */
@WebMvcTest(ReportController.class)
public class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @Test
    void testGetFinancialSummary_Success() throws Exception {
        FinancialSummaryResponseDto mockSummary = new FinancialSummaryResponseDto(
                new BigDecimal("6000.00"),
                new BigDecimal("1500.00"),
                new BigDecimal("4500.00"),
                5L,
                new BigDecimal("1000.00"),
                new BigDecimal("500.00")
        );

        when(transactionService.getFinancialSummary(any(), any())).thenReturn(mockSummary);

        mockMvc.perform(get("/api/reports/summary?startDate=2026-10-01&endDate=2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(6000.00))
                .andExpect(jsonPath("$.totalExpenses").value(1500.00))
                .andExpect(jsonPath("$.netBalance").value(4500.00))
                .andExpect(jsonPath("$.transactionCount").value(5))
                .andExpect(jsonPath("$.highestExpense").value(1000.00))
                .andExpect(jsonPath("$.averageExpense").value(500.00));
    }

    @Test
    void testGetCategoryExpenses_Success() throws Exception {
        List<CategoryExpenseDto> categories = List.of(
                new CategoryExpenseDto("Housing", new BigDecimal("1200.00"), 75.0),
                new CategoryExpenseDto("Food", new BigDecimal("400.00"), 25.0)
        );

        when(transactionService.getCategoryExpenses(any(), any())).thenReturn(categories);

        mockMvc.perform(get("/api/reports/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Housing"))
                .andExpect(jsonPath("$[0].totalAmount").value(1200.00))
                .andExpect(jsonPath("$[0].percentage").value(75.0))
                .andExpect(jsonPath("$[1].category").value("Food"));
    }

    @Test
    void testGetFinancialSummary_InvalidDateRange_Returns400() throws Exception {
        when(transactionService.getFinancialSummary(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 1)))
                .thenThrow(new IllegalArgumentException("startDate cannot be after endDate"));

        mockMvc.perform(get("/api/reports/summary?startDate=2026-10-31&endDate=2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("startDate cannot be after endDate"));
    }
}
