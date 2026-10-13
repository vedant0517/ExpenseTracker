package com.expensetracker.repository;

import com.expensetracker.exception.StorageException;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionType;
import com.expensetracker.util.DatabaseConnectionManager;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Plain JDBC implementation of TransactionRepository.
 * 
 * Features:
 * - PreparedStatement for all queries (prevents SQL injection).
 * - try-with-resources for Connection, PreparedStatement, and ResultSet.
 * - Retrieval of database-generated auto-increment keys.
 * - Manual transaction management (commit/rollback) for batch operations.
 * - Credentials hidden from exception logging.
 */
public class JdbcTransactionRepository implements TransactionRepository {

    private final DatabaseConnectionManager connectionManager;

    public JdbcTransactionRepository(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public List<Transaction> loadAll() throws StorageException {
        String sql = "SELECT id, title, amount, category, type, date, description FROM transactions ORDER BY id ASC";
        List<Transaction> list = new ArrayList<>();

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToTransaction(rs));
            }
        } catch (SQLException e) {
            throw new StorageException("Failed to load transactions from database: " + e.getMessage(), e);
        }

        return list;
    }

    @Override
    public Transaction save(Transaction transaction) throws StorageException {
        String sql = "INSERT INTO transactions (title, amount, category, type, date, description) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, transaction.getTitle());
            stmt.setBigDecimal(2, transaction.getAmount());
            stmt.setString(3, transaction.getCategory());
            stmt.setString(4, transaction.getType().name());
            stmt.setDate(5, Date.valueOf(transaction.getDate()));
            stmt.setString(6, transaction.getDescription());

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new StorageException("Creating transaction failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    return new Transaction(
                            generatedId,
                            transaction.getTitle(),
                            transaction.getAmount(),
                            transaction.getCategory(),
                            transaction.getType(),
                            transaction.getDate(),
                            transaction.getDescription()
                    );
                } else {
                    throw new StorageException("Creating transaction failed, no generated ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new StorageException("Database error saving transaction: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Transaction> findById(int id) throws StorageException {
        String sql = "SELECT id, title, amount, category, type, date, description FROM transactions WHERE id = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            throw new StorageException("Database error finding transaction by ID " + id + ": " + e.getMessage(), e);
        }

        return Optional.empty();
    }

    @Override
    public boolean update(Transaction transaction) throws StorageException {
        String sql = "UPDATE transactions SET title = ?, amount = ?, category = ?, type = ?, date = ?, description = ? WHERE id = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, transaction.getTitle());
            stmt.setBigDecimal(2, transaction.getAmount());
            stmt.setString(3, transaction.getCategory());
            stmt.setString(4, transaction.getType().name());
            stmt.setDate(5, Date.valueOf(transaction.getDate()));
            stmt.setString(6, transaction.getDescription());
            stmt.setInt(7, transaction.getId());

            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            throw new StorageException("Database error updating transaction #" + transaction.getId() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(int id) throws StorageException {
        String sql = "DELETE FROM transactions WHERE id = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            int rowsDeleted = stmt.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            throw new StorageException("Database error deleting transaction #" + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void saveAll(List<Transaction> transactions) throws StorageException {
        String sql = "INSERT INTO transactions (title, amount, category, type, date, description) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = connectionManager.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false); // Begin explicit database transaction

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (Transaction t : transactions) {
                    stmt.setString(1, t.getTitle());
                    stmt.setBigDecimal(2, t.getAmount());
                    stmt.setString(3, t.getCategory());
                    stmt.setString(4, t.getType().name());
                    stmt.setDate(5, Date.valueOf(t.getDate()));
                    stmt.setString(6, t.getDescription());
                    stmt.addBatch();
                }
                stmt.executeBatch();
                conn.commit(); // Commit database transaction
            } catch (SQLException e) {
                conn.rollback(); // Roll back all insertions on error
                throw new StorageException("Transaction failed and was rolled back: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new StorageException("Database error during bulk save: " + e.getMessage(), e);
        }
    }

    private Transaction mapRowToTransaction(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String title = rs.getString("title");
        BigDecimal amount = rs.getBigDecimal("amount");
        String category = rs.getString("category");
        TransactionType type = TransactionType.valueOf(rs.getString("type").toUpperCase());
        LocalDate date = rs.getDate("date").toLocalDate();
        String description = rs.getString("description");

        return new Transaction(id, title, amount, category, type, date, description);
    }
}
