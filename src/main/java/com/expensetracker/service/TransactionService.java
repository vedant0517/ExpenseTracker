package com.expensetracker.service;

import com.expensetracker.dto.CategoryExpenseDto;
import com.expensetracker.dto.FinancialSummaryResponseDto;
import com.expensetracker.dto.TransactionRequestDto;
import com.expensetracker.dto.TransactionResponseDto;
import com.expensetracker.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/**
 * Service contract defining business operations for transaction management and reporting.
 */
public interface TransactionService {

    /**
     * Creates and stores a new transaction from the validated request DTO.
     */
    TransactionResponseDto createTransaction(TransactionRequestDto requestDto);

    /**
     * Retrieves a paginated and filtered list of transactions.
     */
    Page<TransactionResponseDto> getTransactions(
            TransactionType type,
            String category,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    /**
     * Retrieves a single transaction by its primary key ID.
     */
    TransactionResponseDto getTransactionById(Long id);

    /**
     * Updates an existing transaction.
     */
    TransactionResponseDto updateTransaction(Long id, TransactionRequestDto requestDto);

    /**
     * Deletes a transaction by its primary key ID.
     */
    void deleteTransaction(Long id);

    /**
     * Computes the financial summary (income, expenses, net balance, highest/average expense).
     */
    FinancialSummaryResponseDto getFinancialSummary(LocalDate startDate, LocalDate endDate);

    /**
     * Computes category-wise expense breakdown and percentage share.
     */
    List<CategoryExpenseDto> getCategoryExpenses(LocalDate startDate, LocalDate endDate);
}
