package com.aipanelist.processing;

import com.aipanelist.model.ProcessingStatus;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Data transfer object representing the current status of a document being processed.
 * Includes overall status, per-agent progress, and estimated time remaining.
 */
public class DocumentStatus {
    
    private final String documentId;
    private final ProcessingStatus status;
    private final List<AgentProgressInfo> agentProgress;
    private final List<String> errorMessages;
    private final Optional<Duration> estimatedTimeRemaining;
    
    public DocumentStatus(String documentId, ProcessingStatus status, 
                         List<AgentProgressInfo> agentProgress, 
                         List<String> errorMessages,
                         Optional<Duration> estimatedTimeRemaining) {
        this.documentId = documentId;
        this.status = status;
        this.agentProgress = agentProgress;
        this.errorMessages = errorMessages;
        this.estimatedTimeRemaining = estimatedTimeRemaining;
    }
    
    public String getDocumentId() {
        return documentId;
    }
    
    public ProcessingStatus getStatus() {
        return status;
    }
    
    public List<AgentProgressInfo> getAgentProgress() {
        return agentProgress;
    }
    
    public List<String> getErrorMessages() {
        return errorMessages;
    }
    
    public Optional<Duration> getEstimatedTimeRemaining() {
        return estimatedTimeRemaining;
    }
    
    /**
     * Represents progress information for a single agent.
     */
    public static class AgentProgressInfo {
        private final String agentId;
        private final String agentType;
        private final int chunksCompleted;
        private final int totalChunks;
        
        public AgentProgressInfo(String agentId, String agentType, 
                                int chunksCompleted, int totalChunks) {
            this.agentId = agentId;
            this.agentType = agentType;
            this.chunksCompleted = chunksCompleted;
            this.totalChunks = totalChunks;
        }
        
        public String getAgentId() {
            return agentId;
        }
        
        public String getAgentType() {
            return agentType;
        }
        
        public int getChunksCompleted() {
            return chunksCompleted;
        }
        
        public int getTotalChunks() {
            return totalChunks;
        }
        
        public double getProgressPercentage() {
            if (totalChunks == 0) {
                return 0.0;
            }
            return (double) chunksCompleted / totalChunks * 100.0;
        }
    }
}
