package com.aipanelist.upload;

/**
 * Response DTO for document upload operations.
 * 
 * Contains the unique document identifier, original filename, and initial status
 * after successful upload.
 */
public class DocumentUploadResponse {
    
    private String documentId;
    private String filename;
    private String status;
    
    public DocumentUploadResponse() {
    }
    
    public DocumentUploadResponse(String documentId, String filename, String status) {
        this.documentId = documentId;
        this.filename = filename;
        this.status = status;
    }
    
    public String getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    
    public String getFilename() {
        return filename;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}
