package com.aipanelist.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entity representing the analysis of a single document chunk by an AI agent.
 * Multiple chunk analyses are synthesized into a complete analysis report.
 */
@Entity
@Table(name = "chunk_analyses")
public class ChunkAnalysis {
    
    @Id
    @Column(name = "analysis_id", nullable = false, length = 100)
    private String analysisId;
    
    @Column(name = "report_id", nullable = false, length = 100)
    private String reportId;
    
    @Column(name = "chunk_id", nullable = false, length = 100)
    private String chunkId;
    
    @Column(name = "chunk_sequence", nullable = false)
    private int chunkSequence;
    
    @Lob
    @Column(name = "findings", nullable = false, columnDefinition = "TEXT")
    private String findings;
    
    @Lob
    @Column(name = "context_summary", nullable = false, columnDefinition = "TEXT")
    private String contextSummary;
    
    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;
    
    public ChunkAnalysis() {
    }
    
    public ChunkAnalysis(String analysisId, String reportId, String chunkId, int chunkSequence,
                        String findings, String contextSummary, LocalDateTime analyzedAt) {
        this.analysisId = analysisId;
        this.reportId = reportId;
        this.chunkId = chunkId;
        this.chunkSequence = chunkSequence;
        this.findings = findings;
        this.contextSummary = contextSummary;
        this.analyzedAt = analyzedAt;
    }
    
    public String getAnalysisId() {
        return analysisId;
    }
    
    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }
    
    public String getReportId() {
        return reportId;
    }
    
    public void setReportId(String reportId) {
        this.reportId = reportId;
    }
    
    public String getChunkId() {
        return chunkId;
    }
    
    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }
    
    public int getChunkSequence() {
        return chunkSequence;
    }
    
    public void setChunkSequence(int chunkSequence) {
        this.chunkSequence = chunkSequence;
    }
    
    public String getFindings() {
        return findings;
    }
    
    public void setFindings(String findings) {
        this.findings = findings;
    }
    
    public String getContextSummary() {
        return contextSummary;
    }
    
    public void setContextSummary(String contextSummary) {
        this.contextSummary = contextSummary;
    }
    
    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }
    
    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }
}
