package com.aipanelist.processing;

import com.aipanelist.model.ExtractedDocument;

import java.nio.file.Path;

/**
 * Service interface for processing PDF documents and extracting text content.
 * 
 * Handles text extraction from PDF files using Apache PDFBox, preserving
 * logical reading order, calculating token counts, and validating document
 * size limits.
 */
public interface DocumentProcessor {
    
    /**
     * Extracts text content from a PDF file and stores it as an ExtractedDocument.
     * 
     * @param documentId the unique identifier for the document
     * @param pdfPath the file system path to the PDF file
     * @return the ExtractedDocument entity containing extracted text and metadata
     * @throws ExtractionException if text extraction fails or document exceeds size limits
     */
    ExtractedDocument extractText(String documentId, Path pdfPath) throws ExtractionException;
    
    /**
     * Calculates the approximate token count for the given text.
     * Uses the approximation: 1 token ≈ 4 characters for English text.
     * 
     * @param text the text to count tokens for
     * @return the approximate number of tokens
     */
    int calculateTokenCount(String text);
}
