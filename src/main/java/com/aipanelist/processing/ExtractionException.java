package com.aipanelist.processing;

/**
 * Exception thrown when document text extraction fails.
 * 
 * This exception is thrown when:
 * - PDF file cannot be read or is corrupted
 * - Text extraction from PDF fails
 * - Document exceeds maximum size limits (500 pages or 1,000,000 tokens)
 */
public class ExtractionException extends Exception {
    
    public ExtractionException(String message) {
        super(message);
    }
    
    public ExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
