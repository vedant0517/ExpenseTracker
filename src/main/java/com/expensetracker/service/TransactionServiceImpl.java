package com.expensetracker.service;

import com.expensetracker.dto.CategoryExpenseDto;
import com.expensetracker.dto.FinancialSummaryResponseDto;
import com.expensetracker.dto.TransactionRequestDto;
import com.expensetracker.dto.TransactionResponseDto;
import com.expensetracker.entity.Transaction;
import com.expensetracker.entity.User;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.model.TransactionType;
import com.expensetracker.repository.TransactionJpaRepository;
import com.expensetracker.repository.UserJpaRepository;
import com.expensetracker.repository.projection.CategoryTotalProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service implementation managing user-scoped transaction lifecycle and financial aggregations.
 * Enforces strict tenant ownership and prevents IDOR vulnerabilities.
 */
@Service
@Transactional(readOnly = true)
public class TransactionServiceImpl implements TransactionService {

    private final TransactionJpaRepository repository;
    private final UserJpaRepository userRepository;

    public TransactionServiceImpl(TransactionJpaRepository repository, UserJpaRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public TransactionResponseDto createTransaction(TransactionRequestDto requestDto) {
        User user = getCurrentAuthenticatedUser();
        Transaction entity = new Transaction(
                requestDto.getTitle(),
                requestDto.getAmount(),
                requestDto.getCategory(),
                requestDto.getType(),
                requestDto.getDate(),
                requestDto.getDescription(),
                user
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

        User user = getCurrentAuthenticatedUser();
        validateDateRange(startDate, endDate);
        return repository.findFilteredByUserId(user.getId(), type, category, startDate, endDate, pageable)
                .map(TransactionResponseDto::fromEntity);
    }

    @Override
    public TransactionResponseDto getTransactionById(Long id) {
        User user = getCurrentAuthenticatedUser();
        return repository.findByIdAndUserId(id, user.getId())
                .map(TransactionResponseDto::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));
    }

    @Override
    @Transactional
    public TransactionResponseDto updateTransaction(Long id, TransactionRequestDto requestDto) {
        User user = getCurrentAuthenticatedUser();
        Transaction existing = repository.findByIdAndUserId(id, user.getId())
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
        User user = getCurrentAuthenticatedUser();
        Transaction existing = repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));
        repository.delete(existing);
    }

    @Override
    public FinancialSummaryResponseDto getFinancialSummary(LocalDate startDate, LocalDate endDate) {
        User user = getCurrentAuthenticatedUser();
        validateDateRange(startDate, endDate);
        BigDecimal income = repository.sumByUserIdAndTypeAndDateBetween(user.getId(), TransactionType.INCOME, startDate, endDate);
        BigDecimal expenses = repository.sumByUserIdAndTypeAndDateBetween(user.getId(), TransactionType.EXPENSE, startDate, endDate);
        BigDecimal netBalance = income.subtract(expenses).setScale(2, RoundingMode.HALF_UP);
        long count = repository.countByUserIdAndDateBetween(user.getId(), startDate, endDate);
        BigDecimal highestExpense = repository.findHighestExpenseByUserId(user.getId(), startDate, endDate);

        // Average expense calculation: total expenses / number of expense transactions for this user
        BigDecimal avgExpense = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        long expenseCount = repository.countByUserIdAndTypeAndDateBetween(user.getId(), TransactionType.EXPENSE, startDate, endDate);
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
        User user = getCurrentAuthenticatedUser();
        validateDateRange(startDate, endDate);
        List<CategoryTotalProjection> projections = repository.findCategoryExpenseTotalsByUserId(user.getId(), startDate, endDate);
        BigDecimal totalExpenses = repository.sumByUserIdAndTypeAndDateBetween(user.getId(), TransactionType.EXPENSE, startDate, endDate);

        List<CategoryExpenseDto> result = new ArrayList<>();
        for (CategoryTotalProjection p : projections) {
            double percent = 0.0;
            if (totalExpenses != null && totalExpenses.compareTo(BigDecimal.ZERO) > 0 && p.getTotalAmount() != null) {
                // Exact percentage calculation using BigDecimal with HALF_UP rounding to 1 decimal place
                percent = p.getTotalAmount().multiply(BigDecimal.valueOf(100))
                        .divide(totalExpenses, 1, RoundingMode.HALF_UP)
                        .doubleValue();
            }
            result.add(new CategoryExpenseDto(capitalize(p.getCategory()), p.getTotalAmount(), percent));
        }

        return result;
    }

    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new BadCredentialsException("User is not authenticated");
        }
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
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
