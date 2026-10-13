package com.expensetracker.exception;

/**
 * Exception thrown when a requested database resource (such as a Transaction ID) is not found.
 * Triggers an HTTP 404 NOT FOUND response via GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
