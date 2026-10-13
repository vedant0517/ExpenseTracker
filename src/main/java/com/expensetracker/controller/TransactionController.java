package com.expensetracker.controller;

import com.expensetracker.dto.TransactionRequestDto;
import com.expensetracker.dto.TransactionResponseDto;
import com.expensetracker.model.TransactionType;
import com.expensetracker.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

/**
 * REST controller exposing endpoints for managing financial transactions.
 *
 * Annotations explained:
 * - @RestController: Combines @Controller and @ResponseBody, automatically serializing return values to JSON.
 * - @RequestMapping("/api/transactions"): Base URL path for all endpoints in this controller.
 * - @Valid: Triggers Jakarta Bean Validation on incoming JSON bodies before invoking the method.
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * POST /api/transactions — Create a new transaction.
     * Returns 201 CREATED with the created transaction and Location header.
     */
    @PostMapping
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
     * Supports filtering by type (INCOME/EXPENSE), category (partial substring), and date range.
     * Example: /api/transactions?type=EXPENSE&category=Food&page=0&size=10&sort=date,desc
     */
    @GetMapping
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
    public ResponseEntity<TransactionResponseDto> getTransactionById(@PathVariable Long id) {
        TransactionResponseDto response = transactionService.getTransactionById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/transactions/{id} — Update an existing transaction.
     */
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponseDto> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequestDto requestDto) {

        TransactionResponseDto updated = transactionService.updateTransaction(id, requestDto);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/transactions/{id} — Delete a transaction by ID.
     * Returns 204 NO CONTENT on successful deletion.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
}
