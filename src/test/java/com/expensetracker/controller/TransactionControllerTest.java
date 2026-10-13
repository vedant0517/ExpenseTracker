package com.expensetracker.controller;

import com.expensetracker.dto.TransactionRequestDto;
import com.expensetracker.entity.User;
import com.expensetracker.model.TransactionType;
import com.expensetracker.repository.UserJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full integration test suite using Spring Boot MockMvc.
 * Tests HTTP status codes, JSON response shapes, validation errors, and authentication/authorization boundaries.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByUsername("alice")) {
            userRepository.save(new User("alice", "alice@example.com", passwordEncoder.encode("password123")));
        }
        if (!userRepository.existsByUsername("bob")) {
            userRepository.save(new User("bob", "bob@example.com", passwordEncoder.encode("password123")));
        }
    }

    @Test
    void testUnauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @WithMockUser(username = "alice")
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
    @WithMockUser(username = "alice")
    void testCreateTransactionValidationError() throws Exception {
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
    @WithMockUser(username = "alice")
    void testGetTransactionsPagination() throws Exception {
        mockMvc.perform(get("/api/transactions?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").exists());
    }

    @Test
    @WithMockUser(username = "alice")
    void testGetNonExistentTransactionReturns404() throws Exception {
        mockMvc.perform(get("/api/transactions/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @WithMockUser(username = "alice")
    void testGetReportsSummaryAndCategories() throws Exception {
        mockMvc.perform(get("/api/reports/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").exists())
                .andExpect(jsonPath("$.totalExpenses").exists())
                .andExpect(jsonPath("$.netBalance").exists());

        mockMvc.perform(get("/api/reports/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(username = "alice")
    void testUpdateTransactionSuccess() throws Exception {
        TransactionRequestDto createReq = new TransactionRequestDto(
                "Initial Coffee", new BigDecimal("5.00"), "Drinks", TransactionType.EXPENSE, LocalDate.now(), "Morning"
        );
        String createRes = mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long createdId = objectMapper.readTree(createRes).get("id").asLong();

        TransactionRequestDto updateReq = new TransactionRequestDto(
                "Updated Coffee", new BigDecimal("6.50"), "Drinks", TransactionType.EXPENSE, LocalDate.now(), "Large latte"
        );
        mockMvc.perform(put("/api/transactions/" + createdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Coffee"))
                .andExpect(jsonPath("$.amount").value(6.50));
    }

    @Test
    @WithMockUser(username = "alice")
    void testDeleteTransactionSuccess() throws Exception {
        TransactionRequestDto createReq = new TransactionRequestDto(
                "To Delete", new BigDecimal("10.00"), "Misc", TransactionType.EXPENSE, LocalDate.now(), ""
        );
        String createRes = mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long createdId = objectMapper.readTree(createRes).get("id").asLong();

        mockMvc.perform(delete("/api/transactions/" + createdId))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/transactions/" + createdId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "alice")
    void testInvalidIdTypeMismatchReturns400() throws Exception {
        mockMvc.perform(get("/api/transactions/invalid-id-string"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("could not be converted to expected type")));
    }

    @Test
    @WithMockUser(username = "alice")
    void testCrossUserAccessPrevention_IDORProtection() throws Exception {
        // 1. Alice creates a transaction
        TransactionRequestDto aliceReq = new TransactionRequestDto(
                "Alice Secret", new BigDecimal("999.00"), "Secret", TransactionType.EXPENSE, LocalDate.now(), "Private"
        );
        String aliceRes = mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aliceReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long aliceTxnId = objectMapper.readTree(aliceRes).get("id").asLong();

        // 2. Bob tries to access Alice's transaction by ID -> Must receive 404 Not Found (IDOR prevention)
        mockMvc.perform(get("/api/transactions/" + aliceTxnId)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("bob")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // 3. Bob tries to update Alice's transaction -> Must receive 404 Not Found
        TransactionRequestDto tamperReq = new TransactionRequestDto(
                "Tampered", new BigDecimal("1.00"), "Food", TransactionType.EXPENSE, LocalDate.now(), ""
        );
        mockMvc.perform(put("/api/transactions/" + aliceTxnId)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("bob"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tamperReq)))
                .andExpect(status().isNotFound());

        // 4. Bob tries to delete Alice's transaction -> Must receive 404 Not Found
        mockMvc.perform(delete("/api/transactions/" + aliceTxnId)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("bob")))
                .andExpect(status().isNotFound());
    }
}
