package com.aipanelist.api;

import com.aipanelist.processing.DocumentStatus;

/**
 * Exception thrown when attempting to retrieve results for a document that is still being processed.
 */
public class DocumentStillProcessingException extends RuntimeException {
    
    private final String documentId;
    private final DocumentStatus currentStatus;
    private final int statusCode = 202;
    private final String errorCode = "DOCUMENT_PROCESSING";
    
    public DocumentStillProcessingException(String documentId, DocumentStatus currentStatus) {
        super("Document is still processing: " + documentId + " (status: " + currentStatus.getStatus() + ")");
        this.documentId = documentId;
        this.currentStatus = currentStatus;
    }
    
    public String getDocumentId() {
        return documentId;
    }
    
    public DocumentStatus getCurrentStatus() {
        return currentStatus;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
