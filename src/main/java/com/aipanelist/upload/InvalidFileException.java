package com.aipanelist.upload;

/**
 * Exception thrown when an uploaded file fails validation.
 * 
 * This exception is thrown when:
 * - File size exceeds the maximum allowed (50MB)
 * - File is not a valid PDF
 * - PDF structure is corrupted
 */
public class InvalidFileException extends Exception {
    
    public InvalidFileException(String message) {
        super(message);
    }
    
    public InvalidFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
