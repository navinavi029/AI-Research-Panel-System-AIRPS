package com.aipanelist.api;

import com.aipanelist.model.ConsensusReport;
import com.aipanelist.processing.DocumentStatus;

/**
 * Service interface for retrieving and formatting analysis results.
 * Provides methods to retrieve consensus reports, detailed results, and format output as JSON.
 */
public interface ResultsService {
    
    /**
     * Retrieves the consensus report for a document.
     * If the document is still processing, throws an exception indicating the current status.
     *
     * @param documentId the document identifier
     * @return the consensus report
     * @throws DocumentNotFoundException if the document does not exist
     */
    ConsensusReport getConsensusReport(String documentId);
    
    /**
     * Retrieves detailed results including all individual agent reports and the consensus report.
     * If the document is still processing, throws an exception indicating the current status.
     *
     * @param documentId the document identifier
     * @return detailed results containing all analysis reports and consensus report
     * @throws DocumentNotFoundException if the document does not exist
     */
    DetailedResultsDTO getDetailedResults(String documentId);
    
    /**
     * Retrieves the current processing status of a document.
     *
     * @param documentId the document identifier
     * @return the current document status
     * @throws DocumentNotFoundException if the document does not exist
     */
    DocumentStatus getDocumentStatus(String documentId);
    
    /**
     * Formats a consensus report as JSON.
     *
     * @param report the consensus report to format
     * @return JSON string representation of the report
     */
    String formatAsJSON(ConsensusReport report);
    
    /**
     * Formats detailed results as JSON.
     *
     * @param results the detailed results to format
     * @return JSON string representation of the results
     */
    String formatAsJSON(DetailedResultsDTO results);
}
