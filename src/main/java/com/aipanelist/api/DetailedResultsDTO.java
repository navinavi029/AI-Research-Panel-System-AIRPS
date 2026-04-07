package com.aipanelist.api;

import com.aipanelist.model.AnalysisReport;
import com.aipanelist.model.ConsensusReport;

import java.util.List;

/**
 * Data Transfer Object containing detailed analysis results for a document.
 * Includes all individual agent reports and the consensus report.
 */
public class DetailedResultsDTO {
    
    private final String documentId;
    private final List<AnalysisReport> agentReports;
    private final ConsensusReport consensusReport;
    
    public DetailedResultsDTO(String documentId, List<AnalysisReport> agentReports, 
                             ConsensusReport consensusReport) {
        this.documentId = documentId;
        this.agentReports = agentReports;
        this.consensusReport = consensusReport;
    }
    
    public String getDocumentId() {
        return documentId;
    }
    
    public List<AnalysisReport> getAgentReports() {
        return agentReports;
    }
    
    public ConsensusReport getConsensusReport() {
        return consensusReport;
    }
}
