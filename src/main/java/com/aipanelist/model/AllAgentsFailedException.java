package com.aipanelist.model;

/**
 * Exception thrown when all agents in a panel fail to produce analysis reports.
 * Indicates a critical failure in the analysis pipeline.
 */
public class AllAgentsFailedException extends RuntimeException {
    
    private final String documentId;
    private final int totalAgents;
    
    public AllAgentsFailedException(String documentId, int totalAgents) {
        super(String.format("All %d agents failed to produce reports for document %s", totalAgents, documentId));
        this.documentId = documentId;
        this.totalAgents = totalAgents;
    }
    
    public AllAgentsFailedException(String documentId, int totalAgents, Throwable cause) {
        super(String.format("All %d agents failed to produce reports for document %s", totalAgents, documentId), cause);
        this.documentId = documentId;
        this.totalAgents = totalAgents;
    }
    
    public String getDocumentId() {
        return documentId;
    }
    
    public int getTotalAgents() {
        return totalAgents;
    }
}
