package com.expensetracker.exception;

/**
 * Custom checked exception thrown when an error occurs during
 * loading from or saving to persistent storage.
 */
public class StorageException extends Exception {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
