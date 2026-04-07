package com.aipanelist.upload;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

/**
 * Service interface for PDF document upload operations.
 * 
 * Handles validation, storage, and metadata management for uploaded PDF files.
 * Validates file size (≤50MB), MIME type (application/pdf), and PDF structure.
 */
public interface PDFUploadService {
    
    /**
     * Uploads a PDF document to the system.
     * 
     * Validates the file, generates a unique document ID, stores the file,
     * and creates a database record with initial status.
     * 
     * @param file the PDF file to upload
     * @return DocumentUploadResponse containing documentId, filename, and status
     * @throws InvalidFileException if validation fails (size, type, or structure)
     */
    DocumentUploadResponse uploadDocument(MultipartFile file) throws InvalidFileException;
    
    /**
     * Validates a PDF file for upload.
     * 
     * Checks:
     * - File size ≤ 50MB
     * - MIME type = application/pdf
     * - Valid PDF structure using Apache PDFBox
     * 
     * @param file the file to validate
     * @return true if valid, false otherwise
     */
    boolean validatePDF(MultipartFile file);
    
    /**
     * Stores a file to the upload directory.
     * 
     * Saves the file to /app/data/uploads/{documentId}.pdf
     * 
     * @param file the file to store
     * @param documentId the unique document identifier
     * @return Path to the stored file
     */
    Path storeFile(MultipartFile file, String documentId);
}
