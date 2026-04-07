package com.aipanelist.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entity representing the extracted text content from a PDF document.
 * Stores the full text extracted from the document along with metadata.
 */
@Entity
@Table(name = "extracted_documents")
public class ExtractedDocument {
    
    @Id
    @Column(name = "document_id", nullable = false, length = 100)
    private String documentId;
    
    @Lob
    @Column(name = "extracted_text", nullable = false, columnDefinition = "TEXT")
    private String extractedText;
    
    @Column(name = "token_count", nullable = false)
    private int tokenCount;
    
    @Column(name = "extracted_at", nullable = false)
    private LocalDateTime extractedAt;
    
    @Column(name = "reading_order_preserved", nullable = false)
    private boolean readingOrderPreserved;
    
    public ExtractedDocument() {
    }
    
    public ExtractedDocument(String documentId, String extractedText, int tokenCount, 
                            LocalDateTime extractedAt, boolean readingOrderPreserved) {
        this.documentId = documentId;
        this.extractedText = extractedText;
        this.tokenCount = tokenCount;
        this.extractedAt = extractedAt;
        this.readingOrderPreserved = readingOrderPreserved;
    }
    
    public String getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    
    public String getExtractedText() {
        return extractedText;
    }
    
    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }
    
    public int getTokenCount() {
        return tokenCount;
    }
    
    public void setTokenCount(int tokenCount) {
        this.tokenCount = tokenCount;
    }
    
    public LocalDateTime getExtractedAt() {
        return extractedAt;
    }
    
    public void setExtractedAt(LocalDateTime extractedAt) {
        this.extractedAt = extractedAt;
    }
    
    public boolean isReadingOrderPreserved() {
        return readingOrderPreserved;
    }
    
    public void setReadingOrderPreserved(boolean readingOrderPreserved) {
        this.readingOrderPreserved = readingOrderPreserved;
    }
}
