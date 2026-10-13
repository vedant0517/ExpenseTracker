package com.expensetracker.controller;

import com.expensetracker.dto.ErrorResponseDto;
import com.expensetracker.dto.TransactionRequestDto;
import com.expensetracker.dto.TransactionResponseDto;
import com.expensetracker.model.TransactionType;
import com.expensetracker.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

/**
 * REST controller exposing endpoints for managing financial transactions.
 * All endpoints require Bearer JWT authentication and operate strictly on the caller's transactions.
 */
@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "CRUD and filtering operations for user transactions")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * POST /api/transactions — Create a new transaction.
     */
    @PostMapping
    @Operation(summary = "Create a transaction", description = "Creates a new transaction associated with the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transaction created successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<TransactionResponseDto> createTransaction(
            @Valid @RequestBody TransactionRequestDto requestDto) {

        TransactionResponseDto created = transactionService.createTransaction(requestDto);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    /**
     * GET /api/transactions — List transactions with pagination and optional filtering.
     */
    @GetMapping
    @Operation(summary = "List transactions", description = "Retrieves paginated transactions for the authenticated user, supporting filtering by type, category, and date range.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<Page<TransactionResponseDto>> getTransactions(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 10, sort = "date", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<TransactionResponseDto> page = transactionService.getTransactions(type, category, startDate, endDate, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * GET /api/transactions/{id} — Retrieve a single transaction by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID", description = "Retrieves a single transaction belonging to the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found or not owned by caller",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<TransactionResponseDto> getTransactionById(@PathVariable Long id) {
        TransactionResponseDto response = transactionService.getTransactionById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/transactions/{id} — Update an existing transaction.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update transaction", description = "Updates an existing transaction belonging to the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction updated successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found or not owned by caller",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<TransactionResponseDto> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequestDto requestDto) {

        TransactionResponseDto updated = transactionService.updateTransaction(id, requestDto);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/transactions/{id} — Delete a transaction by ID.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete transaction", description = "Deletes a transaction belonging to the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Transaction deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Transaction not found or not owned by caller",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
}
