package com.aipanelist.model;

/**
 * Exception thrown when maximum retry attempts have been exceeded.
 * Used when operations fail repeatedly despite retry logic.
 */
public class MaxRetriesExceededException extends RuntimeException {
    
    private final int maxRetries;
    private final String operation;
    
    public MaxRetriesExceededException(String operation, int maxRetries) {
        super(String.format("Operation '%s' failed after %d retry attempts", operation, maxRetries));
        this.operation = operation;
        this.maxRetries = maxRetries;
    }
    
    public MaxRetriesExceededException(String operation, int maxRetries, Throwable cause) {
        super(String.format("Operation '%s' failed after %d retry attempts", operation, maxRetries), cause);
        this.operation = operation;
        this.maxRetries = maxRetries;
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public String getOperation() {
        return operation;
    }
}
