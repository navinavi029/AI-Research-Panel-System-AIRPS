package com.aipanelist.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entity representing an uploaded document in the AI Panelist System.
 * Tracks the document metadata and processing status.
 */
@Entity
@Table(name = "documents")
public class Document {
    
    @Id
    @Column(name = "document_id", nullable = false, length = 100)
    private String documentId;
    
    @Column(name = "filename", nullable = false, length = 255)
    private String filename;
    
    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;
    
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProcessingStatus status;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "total_tokens")
    private int totalTokens;
    
    @Column(name = "total_pages")
    private int totalPages;
    
    public Document() {
    }
    
    public Document(String documentId, String filename, long fileSizeBytes, LocalDateTime uploadedAt, 
                   ProcessingStatus status, String errorMessage, int totalTokens, int totalPages) {
        this.documentId = documentId;
        this.filename = filename;
        this.fileSizeBytes = fileSizeBytes;
        this.uploadedAt = uploadedAt;
        this.status = status;
        this.errorMessage = errorMessage;
        this.totalTokens = totalTokens;
        this.totalPages = totalPages;
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
    
    public long getFileSizeBytes() {
        return fileSizeBytes;
    }
    
    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }
    
    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
    
    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
    
    public ProcessingStatus getStatus() {
        return status;
    }
    
    public void setStatus(ProcessingStatus status) {
        this.status = status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public int getTotalTokens() {
        return totalTokens;
    }
    
    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
