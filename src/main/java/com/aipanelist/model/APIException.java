package com.aipanelist.model;

/**
 * Exception thrown when NVIDIA API calls fail.
 * 
 * Wraps various API error conditions including network failures,
 * rate limiting, and server errors.
 */
public class APIException extends Exception {

    private final int statusCode;
    private final String errorCode;

    public APIException(String message) {
        super(message);
        this.statusCode = -1;
        this.errorCode = "UNKNOWN";
    }

    public APIException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.errorCode = "UNKNOWN";
    }

    public APIException(String message, int statusCode, String errorCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public APIException(String message, int statusCode, String errorCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public boolean isRetryable() {
        return statusCode >= 500 || statusCode == 429;
    }
}
