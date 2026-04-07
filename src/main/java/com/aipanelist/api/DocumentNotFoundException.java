package com.aipanelist.api;

/**
 * Exception thrown when a requested document is not found in the system.
 */
public class DocumentNotFoundException extends RuntimeException {
    
    private final int statusCode = 404;
    private final String errorCode = "DOCUMENT_NOT_FOUND";
    
    public DocumentNotFoundException(String documentId) {
        super("Document not found: " + documentId);
    }
    
    public DocumentNotFoundException(String documentId, Throwable cause) {
        super("Document not found: " + documentId, cause);
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
