package com.expensetracker.service;

import com.expensetracker.dto.CategoryExpenseDto;
import com.expensetracker.dto.FinancialSummaryResponseDto;
import com.expensetracker.dto.TransactionRequestDto;
import com.expensetracker.dto.TransactionResponseDto;
import com.expensetracker.entity.Transaction;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.model.TransactionType;
import com.expensetracker.repository.TransactionJpaRepository;
import com.expensetracker.repository.projection.CategoryTotalProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service implementation managing transaction lifecycle and financial aggregations.
 *
 * Annotations explained:
 * - @Service: Registers this class as a Spring-managed service bean.
 * - @Transactional(readOnly = true): Configures database transactions as read-only by default,
 *   optimizing Hibernate query caching and dirty checking.
 * - @Transactional (on write methods): Ensures atomic database writes with automatic rollback on error.
 */
@Service
@Transactional(readOnly = true)
public class TransactionServiceImpl implements TransactionService {

    private final TransactionJpaRepository repository;

    /**
     * Constructor injection (recommended over field injection with @Autowired).
     * Enables immutability and easy mock injection during testing.
     */
    public TransactionServiceImpl(TransactionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public TransactionResponseDto createTransaction(TransactionRequestDto requestDto) {
        Transaction entity = new Transaction(
                requestDto.getTitle(),
                requestDto.getAmount(),
                requestDto.getCategory(),
                requestDto.getType(),
                requestDto.getDate(),
                requestDto.getDescription()
        );

        Transaction saved = repository.save(entity);
        return TransactionResponseDto.fromEntity(saved);
    }

    @Override
    public Page<TransactionResponseDto> getTransactions(
            TransactionType type,
            String category,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {

        validateDateRange(startDate, endDate);
        return repository.findFiltered(type, category, startDate, endDate, pageable)
                .map(TransactionResponseDto::fromEntity);
    }

    @Override
    public TransactionResponseDto getTransactionById(Long id) {
        return repository.findById(id)
                .map(TransactionResponseDto::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));
    }

    @Override
    @Transactional
    public TransactionResponseDto updateTransaction(Long id, TransactionRequestDto requestDto) {
        Transaction existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));

        existing.setTitle(requestDto.getTitle());
        existing.setAmount(requestDto.getAmount());
        existing.setCategory(requestDto.getCategory());
        existing.setType(requestDto.getType());
        existing.setDate(requestDto.getDate());
        existing.setDescription(requestDto.getDescription());

        Transaction updated = repository.save(existing);
        return TransactionResponseDto.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteTransaction(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Transaction", "id", id);
        }
        repository.deleteById(id);
    }

    @Override
    public FinancialSummaryResponseDto getFinancialSummary(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        BigDecimal income = repository.sumByTypeAndDateBetween(TransactionType.INCOME, startDate, endDate);
        BigDecimal expenses = repository.sumByTypeAndDateBetween(TransactionType.EXPENSE, startDate, endDate);
        BigDecimal netBalance = income.subtract(expenses).setScale(2, RoundingMode.HALF_UP);
        long count = repository.countByDateBetween(startDate, endDate);
        BigDecimal highestExpense = repository.findHighestExpense(startDate, endDate);

        // Average expense calculation
        BigDecimal avgExpense = BigDecimal.ZERO.setScale(2);
        long expenseCount = repository.findFiltered(TransactionType.EXPENSE, null, startDate, endDate, Pageable.unpaged()).getTotalElements();
        if (expenseCount > 0 && expenses.compareTo(BigDecimal.ZERO) > 0) {
            avgExpense = expenses.divide(BigDecimal.valueOf(expenseCount), 2, RoundingMode.HALF_UP);
        }

        return new FinancialSummaryResponseDto(
                income,
                expenses,
                netBalance,
                count,
                highestExpense,
                avgExpense
        );
    }

    @Override
    public List<CategoryExpenseDto> getCategoryExpenses(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        List<CategoryTotalProjection> projections = repository.findCategoryExpenseTotals(startDate, endDate);
        BigDecimal totalExpenses = repository.sumByTypeAndDateBetween(TransactionType.EXPENSE, startDate, endDate);

        List<CategoryExpenseDto> result = new ArrayList<>();
        for (CategoryTotalProjection p : projections) {
            double percent = (totalExpenses.compareTo(BigDecimal.ZERO) > 0)
                    ? p.getTotalAmount().doubleValue() / totalExpenses.doubleValue() * 100.0
                    : 0.0;
            // Round percent to 1 decimal place
            percent = Math.round(percent * 10.0) / 10.0;
            result.add(new CategoryExpenseDto(capitalize(p.getCategory()), p.getTotalAmount(), percent));
        }

        return result;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    String.format("startDate cannot be after endDate: %s is after %s", startDate, endDate)
            );
        }
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return "";
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
