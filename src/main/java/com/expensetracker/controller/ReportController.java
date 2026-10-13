package com.expensetracker.controller;

import com.expensetracker.dto.CategoryExpenseDto;
import com.expensetracker.dto.ErrorResponseDto;
import com.expensetracker.dto.FinancialSummaryResponseDto;
import com.expensetracker.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller exposing financial summaries, balances, and category analytics for the authenticated user.
 */
@RestController
@RequestMapping("/api/reports")
@Tag(name = "Financial Reports", description = "Endpoints for aggregated income, expense summaries, and category metrics")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final TransactionService transactionService;

    public ReportController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * GET /api/reports/summary — Aggregated income, expenses, and net balance.
     */
    @GetMapping("/summary")
    @Operation(summary = "Get financial summary", description = "Retrieves total income, total expenses, net balance, transaction count, highest single expense, and average expense for the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Financial summary retrieved successfully",
                    content = @Content(schema = @Schema(implementation = FinancialSummaryResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date range parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<FinancialSummaryResponseDto> getFinancialSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        FinancialSummaryResponseDto summary = transactionService.getFinancialSummary(startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/reports/categories — Category-wise expense breakdown with spend percentages.
     */
    @GetMapping("/categories")
    @Operation(summary = "Get category expenses", description = "Calculates total expenses and percentage share per category for the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category breakdown retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid date range parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<List<CategoryExpenseDto>> getCategoryExpenses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<CategoryExpenseDto> categories = transactionService.getCategoryExpenses(startDate, endDate);
        return ResponseEntity.ok(categories);
    }
}
