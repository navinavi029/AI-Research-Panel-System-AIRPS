package com.aipanelist.orchestration;

import com.aipanelist.agents.AIAgent;

import java.util.List;

/**
 * Represents a panel of AI agents created for analyzing a specific document.
 * 
 * Contains exactly 6 specialized agents, one of each type:
 * - Lead Analyst
 * - General Analyst
 * - Methodology Reviewer
 * - Literature Reviewer
 * - Quick Screener
 * - Fact Extractor
 */
public class AnalysisPanel {
    
    private final String documentId;
    private final List<AIAgent> agents;
    
    /**
     * Constructor for AnalysisPanel.
     *
     * @param documentId the document identifier this panel will analyze
     * @param agents the list of 6 specialized AI agents
     */
    public AnalysisPanel(String documentId, List<AIAgent> agents) {
        if (agents == null || agents.size() != 6) {
            throw new IllegalArgumentException("Panel must contain exactly 6 agents");
        }
        this.documentId = documentId;
        this.agents = agents;
    }
    
    /**
     * Get the document ID this panel is analyzing.
     *
     * @return the document identifier
     */
    public String getDocumentId() {
        return documentId;
    }
    
    /**
     * Get all agents in the panel.
     *
     * @return list of all 6 agents
     */
    public List<AIAgent> getAgents() {
        return agents;
    }
}
