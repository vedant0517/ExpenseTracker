package com.expensetracker.repository;

import com.expensetracker.exception.StorageException;
import com.expensetracker.model.Transaction;

import java.util.List;

/**
 * Storage abstraction contract for reading and writing transactions.
 * Allows decoupling the storage implementation (CSV, Database, etc.)
 * from the business logic layer.
 */
public interface TransactionRepository {

    /**
     * Loads all persisted transactions from storage.
     *
     * @return List of persisted transactions (empty if no file/data exists)
     * @throws StorageException if the storage file exists but cannot be read or is corrupt
     */
    List<Transaction> loadAll() throws StorageException;

    /**
     * Persists all transactions to storage.
     *
     * @param transactions The list of transactions to save
     * @throws StorageException if an I/O error occurs during write
     */
    void saveAll(List<Transaction> transactions) throws StorageException;
}
