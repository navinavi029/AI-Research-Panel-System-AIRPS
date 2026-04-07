package com.aipanelist.async;

/**
 * Service for asynchronous document analysis orchestration.
 * 
 * Coordinates the complete analysis pipeline: extraction → chunking → panel analysis → consensus.
 * Runs asynchronously in a dedicated thread pool, updating document status at each stage.
 * 
 * Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6
 */
public interface AsyncAnalysisService {
    
    /**
     * Process a document asynchronously through the complete analysis pipeline.
     * 
     * Pipeline stages:
     * 1. UPLOADED → PROCESSING: Extract text from PDF
     * 2. PROCESSING → ANALYZING: Chunk document and run panel analysis
     * 3. ANALYZING → DELIBERATING: Generate consensus report
     * 4. DELIBERATING → COMPLETE: Store final results
     * 
     * On error at any stage: update status to FAILED with error message.
     * 
     * @param documentId the unique identifier of the document to process
     */
    void processDocument(String documentId);
}
