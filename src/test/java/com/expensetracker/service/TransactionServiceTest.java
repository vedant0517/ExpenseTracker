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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pure Unit Test suite for TransactionServiceImpl using JUnit 5 and Mockito.
 * Tests business logic and mapping in complete isolation from the database and web container.
 */
@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private TransactionJpaRepository repository;

    @InjectMocks
    private TransactionServiceImpl service;

    @Test
    void testCreateTransaction_Success() {
        TransactionRequestDto request = new TransactionRequestDto(
                "Gym Membership",
                new BigDecimal("60.00"),
                "Health",
                TransactionType.EXPENSE,
                LocalDate.now(),
                "Monthly fitness pass"
        );

        Transaction savedEntity = new Transaction(
                10L,
                request.getTitle(),
                request.getAmount(),
                request.getCategory(),
                request.getType(),
                request.getDate(),
                request.getDescription()
        );

        when(repository.save(any(Transaction.class))).thenReturn(savedEntity);

        TransactionResponseDto response = service.createTransaction(request);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("Gym Membership", response.title());
        assertEquals(new BigDecimal("60.00"), response.amount());
        assertEquals("Health", response.category());
        assertEquals(TransactionType.EXPENSE, response.type());
        verify(repository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testGetTransactionById_Found() {
        Transaction entity = new Transaction(
                1L, "Bonus", new BigDecimal("1000.00"), "Work", TransactionType.INCOME, LocalDate.now(), "Q3 Bonus"
        );
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        TransactionResponseDto response = service.getTransactionById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Bonus", response.title());
        assertEquals(new BigDecimal("1000.00"), response.amount());
    }

    @Test
    void testGetTransactionById_NotFound_ThrowsException() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getTransactionById(999L));
        verify(repository, times(1)).findById(999L);
    }

    @Test
    void testUpdateTransaction_Success() {
        Transaction existing = new Transaction(
                5L, "Old Title", new BigDecimal("20.00"), "Old Cat", TransactionType.EXPENSE, LocalDate.now(), "Old"
        );
        TransactionRequestDto updateRequest = new TransactionRequestDto(
                "New Title", new BigDecimal("35.00"), "New Cat", TransactionType.EXPENSE, LocalDate.now(), "New Desc"
        );

        when(repository.findById(5L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        TransactionResponseDto updated = service.updateTransaction(5L, updateRequest);

        assertEquals("New Title", updated.title());
        assertEquals(new BigDecimal("35.00"), updated.amount());
        assertEquals("New Cat", updated.category());
        verify(repository, times(1)).save(existing);
    }

    @Test
    void testUpdateTransaction_NotFound_ThrowsException() {
        TransactionRequestDto updateRequest = new TransactionRequestDto(
                "New Title", new BigDecimal("35.00"), "Cat", TransactionType.EXPENSE, LocalDate.now(), ""
        );
        when(repository.findById(888L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateTransaction(888L, updateRequest));
        verify(repository, never()).save(any());
    }

    @Test
    void testDeleteTransaction_Success() {
        when(repository.existsById(12L)).thenReturn(true);
        doNothing().when(repository).deleteById(12L);

        assertDoesNotThrow(() -> service.deleteTransaction(12L));
        verify(repository, times(1)).deleteById(12L);
    }

    @Test
    void testDeleteTransaction_NotFound_ThrowsException() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.deleteTransaction(99L));
        verify(repository, never()).deleteById(anyLong());
    }

    @Test
    void testGetFinancialSummary_KnownValuesAndScaleCheck() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 31);

        when(repository.sumByTypeAndDateBetween(TransactionType.INCOME, start, end))
                .thenReturn(new BigDecimal("5000.00"));
        when(repository.sumByTypeAndDateBetween(TransactionType.EXPENSE, start, end))
                .thenReturn(new BigDecimal("1250.25"));
        when(repository.countByDateBetween(start, end)).thenReturn(4L);
        when(repository.findHighestExpense(start, end)).thenReturn(new BigDecimal("750.00"));

        Page<Transaction> expensePage = new PageImpl<>(List.of(
                new Transaction(1L, "Rent", new BigDecimal("750.00"), "Housing", TransactionType.EXPENSE, start, ""),
                new Transaction(2L, "Groceries", new BigDecimal("500.25"), "Food", TransactionType.EXPENSE, start, "")
        ));
        when(repository.findFiltered(eq(TransactionType.EXPENSE), isNull(), eq(start), eq(end), any(Pageable.class)))
                .thenReturn(expensePage);

        FinancialSummaryResponseDto summary = service.getFinancialSummary(start, end);

        // Verification of mathematical correctness and BigDecimal scale:
        // Income = 5000.00
        // Expense = 1250.25
        // Net Balance = 5000.00 - 1250.25 = 3749.75
        // Average Expense = 1250.25 / 2 = 625.13
        assertEquals(new BigDecimal("5000.00"), summary.totalIncome());
        assertEquals(new BigDecimal("1250.25"), summary.totalExpenses());
        assertEquals(new BigDecimal("3749.75"), summary.netBalance());
        assertEquals(4L, summary.transactionCount());
        assertEquals(new BigDecimal("750.00"), summary.highestExpense());
        assertEquals(new BigDecimal("625.13"), summary.averageExpense());
    }

    @Test
    void testGetFinancialSummary_InvalidDateRange_ThrowsIllegalArgumentException() {
        LocalDate start = LocalDate.of(2026, 10, 31);
        LocalDate end = LocalDate.of(2026, 10, 1); // start after end!

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.getFinancialSummary(start, end)
        );
        assertTrue(ex.getMessage().contains("startDate cannot be after endDate"));
    }

    @Test
    void testGetCategoryExpenses_CalculatesPercentagesCorrectly() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);

        CategoryTotalProjection p1 = mock(CategoryTotalProjection.class);
        when(p1.getCategory()).thenReturn("food");
        when(p1.getTotalAmount()).thenReturn(new BigDecimal("300.00"));

        CategoryTotalProjection p2 = mock(CategoryTotalProjection.class);
        when(p2.getCategory()).thenReturn("transport");
        when(p2.getTotalAmount()).thenReturn(new BigDecimal("100.00"));

        when(repository.findCategoryExpenseTotals(start, end)).thenReturn(List.of(p1, p2));
        when(repository.sumByTypeAndDateBetween(TransactionType.EXPENSE, start, end))
                .thenReturn(new BigDecimal("400.00"));

        List<CategoryExpenseDto> categories = service.getCategoryExpenses(start, end);

        assertEquals(2, categories.size());
        assertEquals("Food", categories.get(0).category());
        assertEquals(new BigDecimal("300.00"), categories.get(0).totalAmount());
        assertEquals(75.0, categories.get(0).percentage());

        assertEquals("Transport", categories.get(1).category());
        assertEquals(new BigDecimal("100.00"), categories.get(1).totalAmount());
        assertEquals(25.0, categories.get(1).percentage());
    }

    @Test
    void testGetTransactions_PaginationAndMapping() {
        Pageable pageable = PageRequest.of(0, 2);
        Transaction t1 = new Transaction(1L, "Income 1", new BigDecimal("50.00"), "Work", TransactionType.INCOME, LocalDate.now(), "");
        Transaction t2 = new Transaction(2L, "Income 2", new BigDecimal("75.00"), "Work", TransactionType.INCOME, LocalDate.now(), "");
        Page<Transaction> page = new PageImpl<>(List.of(t1, t2), pageable, 2);

        when(repository.findFiltered(TransactionType.INCOME, null, null, null, pageable)).thenReturn(page);

        Page<TransactionResponseDto> result = service.getTransactions(TransactionType.INCOME, null, null, null, pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals("Income 1", result.getContent().get(0).title());
        assertEquals("Income 2", result.getContent().get(1).title());
    }
}
