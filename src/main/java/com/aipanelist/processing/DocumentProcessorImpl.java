package com.aipanelist.processing;

import com.aipanelist.model.ExtractedDocument;
import com.aipanelist.repository.DocumentRepository;
import com.aipanelist.repository.ExtractedDocumentRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Implementation of DocumentProcessor for extracting text from PDF documents.
 * 
 * Uses Apache PDFBox PDFTextStripper to extract text while preserving logical
 * reading order. Validates document size limits and calculates token counts.
 */
@Service
public class DocumentProcessorImpl implements DocumentProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessorImpl.class);
    
    // Document size limits
    private static final int MAX_PAGES = 500;
    private static final int MAX_TOKENS = 1_000_000;
    
    // Token calculation: 1 token ≈ 4 characters
    private static final int CHARACTERS_PER_TOKEN = 4;
    
    private final ExtractedDocumentRepository extractedDocumentRepository;
    private final DocumentRepository documentRepository;
    
    public DocumentProcessorImpl(ExtractedDocumentRepository extractedDocumentRepository,
                                DocumentRepository documentRepository) {
        this.extractedDocumentRepository = extractedDocumentRepository;
        this.documentRepository = documentRepository;
    }
    
    @Override
    public ExtractedDocument extractText(String documentId, Path pdfPath) throws ExtractionException {
        logger.info("Starting text extraction for document: {}", documentId);
        
        try {
            // Validate file exists
            if (!Files.exists(pdfPath)) {
                throw new ExtractionException("PDF file not found: " + pdfPath);
            }
            
            // Load PDF document
            byte[] pdfBytes = Files.readAllBytes(pdfPath);
            
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                
                // Validate page count
                int pageCount = document.getNumberOfPages();
                logger.debug("Document has {} pages", pageCount);
                
                if (pageCount > MAX_PAGES) {
                    String errorMsg = String.format(
                        "Document exceeds maximum page limit: %d pages (max: %d)",
                        pageCount, MAX_PAGES
                    );
                    logger.error(errorMsg);
                    throw new ExtractionException(errorMsg);
                }
                
                // Configure PDFTextStripper to preserve logical reading order
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true); // Preserve logical reading order
                
                // Extract text
                String extractedText = stripper.getText(document);
                
                if (extractedText == null || extractedText.trim().isEmpty()) {
                    logger.warn("No text content extracted from document: {}", documentId);
                    extractedText = ""; // Store empty string rather than null
                }
                
                // Calculate token count
                int tokenCount = calculateTokenCount(extractedText);
                logger.debug("Extracted text has {} tokens", tokenCount);
                
                // Validate token count
                if (tokenCount > MAX_TOKENS) {
                    String errorMsg = String.format(
                        "Document exceeds maximum token limit: %d tokens (max: %d)",
                        tokenCount, MAX_TOKENS
                    );
                    logger.error(errorMsg);
                    throw new ExtractionException(errorMsg);
                }
                
                // Create ExtractedDocument entity
                ExtractedDocument extractedDocument = new ExtractedDocument(
                    documentId,
                    extractedText,
                    tokenCount,
                    LocalDateTime.now(),
                    true // Reading order is preserved by PDFTextStripper
                );
                
                // Save to database
                extractedDocumentRepository.save(extractedDocument);
                
                // Update Document entity with page count and token count
                documentRepository.findById(documentId).ifPresent(doc -> {
                    doc.setTotalPages(pageCount);
                    doc.setTotalTokens(tokenCount);
                    documentRepository.save(doc);
                });
                
                logger.info("Successfully extracted text for document: {} ({} pages, {} tokens)",
                    documentId, pageCount, tokenCount);
                
                return extractedDocument;
                
            }
            
        } catch (IOException e) {
            String errorMsg = "Failed to extract text from PDF: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new ExtractionException(errorMsg, e);
        }
    }
    
    @Override
    public int calculateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        // Approximate token count: 1 token ≈ 4 characters
        return text.length() / CHARACTERS_PER_TOKEN;
    }
}
