package com.expensetracker.controller;

import com.expensetracker.dto.CategoryExpenseDto;
import com.expensetracker.dto.FinancialSummaryResponseDto;
import com.expensetracker.service.TransactionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller exposing financial summaries, balances, and category analytics.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final TransactionService transactionService;

    public ReportController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * GET /api/reports/summary — Aggregated income, expenses, and net balance.
     * Optional date range filtering:
     * Example: /api/reports/summary?startDate=2026-10-01&endDate=2026-10-31
     */
    @GetMapping("/summary")
    public ResponseEntity<FinancialSummaryResponseDto> getFinancialSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        FinancialSummaryResponseDto summary = transactionService.getFinancialSummary(startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/reports/categories — Category-wise expense breakdown with spend percentages.
     * Example: /api/reports/categories?startDate=2026-10-01&endDate=2026-10-31
     */
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryExpenseDto>> getCategoryExpenses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<CategoryExpenseDto> categories = transactionService.getCategoryExpenses(startDate, endDate);
        return ResponseEntity.ok(categories);
    }
}
