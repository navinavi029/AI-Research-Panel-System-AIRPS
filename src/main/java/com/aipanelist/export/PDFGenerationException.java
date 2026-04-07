package com.aipanelist.export;

/**
 * Exception thrown when PDF generation fails.
 */
public class PDFGenerationException extends Exception {
    
    public PDFGenerationException(String message) {
        super(message);
    }
    
    public PDFGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
