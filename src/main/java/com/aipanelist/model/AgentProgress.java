package com.aipanelist.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entity representing the progress of an AI agent analyzing a document.
 * Tracks how many chunks have been completed out of the total.
 */
@Entity
@Table(name = "agent_progress")
public class AgentProgress {
    
    @Id
    @Column(name = "progress_id", nullable = false, length = 100)
    private String progressId;
    
    @Column(name = "document_id", nullable = false, length = 100)
    private String documentId;
    
    @Column(name = "agent_id", nullable = false, length = 100)
    private String agentId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false, length = 30)
    private AgentType agentType;
    
    @Column(name = "chunks_completed", nullable = false)
    private int chunksCompleted;
    
    @Column(name = "total_chunks", nullable = false)
    private int totalChunks;
    
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
    
    public AgentProgress() {
    }
    
    public AgentProgress(String progressId, String documentId, String agentId, AgentType agentType,
                        int chunksCompleted, int totalChunks, LocalDateTime lastUpdated) {
        this.progressId = progressId;
        this.documentId = documentId;
        this.agentId = agentId;
        this.agentType = agentType;
        this.chunksCompleted = chunksCompleted;
        this.totalChunks = totalChunks;
        this.lastUpdated = lastUpdated;
    }
    
    public String getProgressId() {
        return progressId;
    }
    
    public void setProgressId(String progressId) {
        this.progressId = progressId;
    }
    
    public String getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    
    public String getAgentId() {
        return agentId;
    }
    
    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }
    
    public AgentType getAgentType() {
        return agentType;
    }
    
    public void setAgentType(AgentType agentType) {
        this.agentType = agentType;
    }
    
    public int getChunksCompleted() {
        return chunksCompleted;
    }
    
    public void setChunksCompleted(int chunksCompleted) {
        this.chunksCompleted = chunksCompleted;
    }
    
    public int getTotalChunks() {
        return totalChunks;
    }
    
    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
