package com.aipanelist.upload;

import com.aipanelist.config.StorageConfiguration;
import com.aipanelist.model.Document;
import com.aipanelist.model.ProcessingStatus;
import com.aipanelist.repository.DocumentRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementation of PDFUploadService for handling PDF document uploads.
 * 
 * Validates uploaded files for size (≤50MB), MIME type (application/pdf),
 * and PDF structure using Apache PDFBox. Stores files in the configured
 * upload directory and creates database records.
 */
@Service
public class PDFUploadServiceImpl implements PDFUploadService {
    
    private static final Logger logger = LoggerFactory.getLogger(PDFUploadServiceImpl.class);
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB in bytes
    private static final String PDF_MIME_TYPE = "application/pdf";
    
    private final StorageConfiguration storageConfiguration;
    private final DocumentRepository documentRepository;
    
    public PDFUploadServiceImpl(StorageConfiguration storageConfiguration, 
                               DocumentRepository documentRepository) {
        this.storageConfiguration = storageConfiguration;
        this.documentRepository = documentRepository;
    }
    
    @Override
    public DocumentUploadResponse uploadDocument(MultipartFile file) throws InvalidFileException {
        logger.info("Starting upload for file: {}", file.getOriginalFilename());
        
        // Validate the PDF file
        if (!validatePDF(file)) {
            throw new InvalidFileException("File validation failed");
        }
        
        // Generate unique document ID
        String documentId = UUID.randomUUID().toString();
        
        // Store the file
        Path filePath = storeFile(file, documentId);
        
        // Create database record
        Document document = new Document(
            documentId,
            file.getOriginalFilename(),
            file.getSize(),
            LocalDateTime.now(),
            ProcessingStatus.UPLOADED,
            null,
            0,
            0
        );
        
        documentRepository.save(document);
        
        logger.info("Successfully uploaded document with ID: {}", documentId);
        
        return new DocumentUploadResponse(
            documentId,
            file.getOriginalFilename(),
            ProcessingStatus.UPLOADED.name().toLowerCase()
        );
    }
    
    @Override
    public boolean validatePDF(MultipartFile file) {
        // Check if file is empty
        if (file.isEmpty()) {
            logger.warn("Validation failed: File is empty");
            return false;
        }
        
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            logger.warn("Validation failed: File size {} exceeds maximum {}", 
                file.getSize(), MAX_FILE_SIZE);
            return false;
        }
        
        // Check MIME type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals(PDF_MIME_TYPE)) {
            logger.warn("Validation failed: Invalid MIME type: {}", contentType);
            return false;
        }
        
        // Validate PDF structure using PDFBox
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            
            // If we can load the document, it's a valid PDF
            int pageCount = document.getNumberOfPages();
            logger.debug("PDF validation successful: {} pages", pageCount);
            return true;
            
        } catch (IOException e) {
            logger.warn("Validation failed: Corrupted or invalid PDF structure", e);
            return false;
        }
    }
    
    @Override
    public Path storeFile(MultipartFile file, String documentId) {
        try {
            // Create filename with document ID
            String filename = documentId + ".pdf";
            Path targetPath = storageConfiguration.getUploadPath().resolve(filename);
            
            // Copy file to target location
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            
            logger.debug("File stored at: {}", targetPath);
            return targetPath;
            
        } catch (IOException e) {
            logger.error("Failed to store file for document ID: {}", documentId, e);
            throw new RuntimeException("Failed to store file", e);
        }
    }
}
