package com.aipanelist.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entity representing the consensus report synthesized from all agent analyses.
 * Contains the unified panel output including themes, agreements, disagreements, and recommendations.
 */
@Entity
@Table(name = "consensus_reports")
public class ConsensusReport {
    
    @Id
    @Column(name = "report_id", nullable = false, length = 100)
    private String reportId;
    
    @Column(name = "document_id", nullable = false, length = 100)
    private String documentId;
    
    @Lob
    @Basic(fetch = FetchType.EAGER)
    @Column(name = "common_themes", nullable = false, columnDefinition = "TEXT")
    private String commonThemes;
    
    @Lob
    @Basic(fetch = FetchType.EAGER)
    @Column(name = "agreements", nullable = false, columnDefinition = "TEXT")
    private String agreements;
    
    @Lob
    @Basic(fetch = FetchType.EAGER)
    @Column(name = "disagreements", nullable = false, columnDefinition = "TEXT")
    private String disagreements;
    
    @Lob
    @Basic(fetch = FetchType.EAGER)
    @Column(name = "unified_recommendations", nullable = false, columnDefinition = "TEXT")
    private String unifiedRecommendations;
    
    @Lob
    @Basic(fetch = FetchType.EAGER)
    @Column(name = "attributed_insights", nullable = false, columnDefinition = "TEXT")
    private String attributedInsights;
    
    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
    
    @Column(name = "agent_reports_included", nullable = false)
    private int agentReportsIncluded;
    
    public ConsensusReport() {
    }
    
    public ConsensusReport(String reportId, String documentId, String commonThemes, String agreements,
                          String disagreements, String unifiedRecommendations, String attributedInsights,
                          LocalDateTime generatedAt, int agentReportsIncluded) {
        this.reportId = reportId;
        this.documentId = documentId;
        this.commonThemes = commonThemes;
        this.agreements = agreements;
        this.disagreements = disagreements;
        this.unifiedRecommendations = unifiedRecommendations;
        this.attributedInsights = attributedInsights;
        this.generatedAt = generatedAt;
        this.agentReportsIncluded = agentReportsIncluded;
    }
    
    public String getReportId() {
        return reportId;
    }
    
    public void setReportId(String reportId) {
        this.reportId = reportId;
    }
    
    public String getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    
    public String getCommonThemes() {
        return commonThemes;
    }
    
    public void setCommonThemes(String commonThemes) {
        this.commonThemes = commonThemes;
    }
    
    public String getAgreements() {
        return agreements;
    }
    
    public void setAgreements(String agreements) {
        this.agreements = agreements;
    }
    
    public String getDisagreements() {
        return disagreements;
    }
    
    public void setDisagreements(String disagreements) {
        this.disagreements = disagreements;
    }
    
    public String getUnifiedRecommendations() {
        return unifiedRecommendations;
    }
    
    public void setUnifiedRecommendations(String unifiedRecommendations) {
        this.unifiedRecommendations = unifiedRecommendations;
    }
    
    public String getAttributedInsights() {
        return attributedInsights;
    }
    
    public void setAttributedInsights(String attributedInsights) {
        this.attributedInsights = attributedInsights;
    }
    
    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
    
    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
    
    public int getAgentReportsIncluded() {
        return agentReportsIncluded;
    }
    
    public void setAgentReportsIncluded(int agentReportsIncluded) {
        this.agentReportsIncluded = agentReportsIncluded;
    }
}
