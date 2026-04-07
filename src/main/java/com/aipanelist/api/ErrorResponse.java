package com.aipanelist.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized error response DTO for API error responses.
 * 
 * Provides consistent error structure across all API endpoints with:
 * - Error code for programmatic handling
 * - Human-readable error message
 * - Timestamp of when the error occurred
 * - Optional document ID for context
 * - Optional additional details
 * 
 * Requirements: 9.1, 9.2, 9.5
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private final String code;
    private final String message;
    private final LocalDateTime timestamp;
    private final String documentId;
    private final Map<String, Object> details;
    
    public ErrorResponse(String code, String message) {
        this(code, message, null, null);
    }
    
    public ErrorResponse(String code, String message, String documentId) {
        this(code, message, documentId, null);
    }
    
    public ErrorResponse(String code, String message, String documentId, Map<String, Object> details) {
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.documentId = documentId;
        this.details = details;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getDocumentId() {
        return documentId;
    }
    
    public Map<String, Object> getDetails() {
        return details;
    }
}
