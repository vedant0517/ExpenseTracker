package com.expensetracker.repository;

import com.expensetracker.exception.StorageException;
import com.expensetracker.model.Transaction;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface defining data access operations for Transaction entities.
 * Decouples business logic from specific persistence implementations (JDBC, CSV, in-memory).
 */
public interface TransactionRepository {

    /**
     * Loads all transactions from the underlying storage.
     */
    List<Transaction> loadAll() throws StorageException;

    /**
     * Saves a new transaction, populating the generated ID if applicable.
     */
    Transaction save(Transaction transaction) throws StorageException;

    /**
     * Finds a single transaction by ID.
     */
    Optional<Transaction> findById(int id) throws StorageException;

    /**
     * Updates an existing transaction.
     *
     * @return true if updated, false if record was not found
     */
    boolean update(Transaction transaction) throws StorageException;

    /**
     * Deletes a transaction by ID.
     *
     * @return true if deleted, false if record was not found
     */
    boolean deleteById(int id) throws StorageException;

    /**
     * Bulk persists a list of transactions using a database transaction
     * where all items must succeed or rollback together.
     */
    void saveAll(List<Transaction> transactions) throws StorageException;
}
