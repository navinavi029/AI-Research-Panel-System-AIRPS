package com.aipanelist.model;

/**
 * Enum representing the processing status of a document in the AI Panelist System.
 * Documents progress through these states during analysis.
 */
public enum ProcessingStatus {
    /**
     * Document has been uploaded and stored
     */
    UPLOADED,
    
    /**
     * Text extraction from PDF is in progress
     */
    PROCESSING,
    
    /**
     * AI agents are analyzing the document content
     */
    ANALYZING,
    
    /**
     * Consensus engine is synthesizing agent reports
     */
    DELIBERATING,
    
    /**
     * Analysis is complete and consensus report is ready
     */
    COMPLETE,
    
    /**
     * An error occurred during processing
     */
    FAILED
}
