package com.aipanelist.orchestration;

import com.aipanelist.model.AnalysisReport;
import com.aipanelist.model.DocumentChunk;

import java.util.List;

/**
 * Interface for orchestrating a panel of AI agents to analyze research documents.
 * 
 * The PanelOrchestrator creates a panel of 6 specialized AI agents, coordinates
 * their parallel analysis of document chunks, collects their reports, and handles
 * agent failures gracefully.
 * 
 * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 9.3, 10.9
 */
public interface PanelOrchestrator {
    
    /**
     * Create a panel of 6 specialized AI agents for document analysis.
     * 
     * Creates one agent of each type:
     * - Lead Analyst Agent
     * - General Analyst Agent
     * - Methodology Reviewer Agent
     * - Literature Reviewer Agent
     * - Quick Screener Agent
     * - Fact Extractor Agent
     * 
     * Each agent is assigned a unique UUID identifier and configured with
     * a NVIDIAModelClient instance for API communication.
     * 
     * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9
     *
     * @param documentId the document identifier for tracking purposes
     * @return an AnalysisPanel containing all 6 agents
     */
    AnalysisPanel createPanel(String documentId);
    
    /**
     * Orchestrate parallel analysis of document chunks by all agents in the panel.
     * 
     * Submits analysis tasks to all 6 agents in parallel using ExecutorService.
     * Each agent processes chunks sequentially with context carryover.
     * Collects AnalysisReport from each agent with 30-minute timeout per agent.
     * 
     * Handles agent failures gracefully:
     * - Logs error with agent ID and exception details
     * - Continues with remaining agents
     * - Stores failure information in the returned list
     * 
     * Updates AgentProgress entities as agents complete chunks.
     * 
     * Requirements: 3.1, 9.3, 10.9
     *
     * @param panel the analysis panel containing all agents
     * @param chunks the list of document chunks to analyze
     * @return list of AnalysisReports from successful agents (may be less than 6 if failures occur)
     */
    List<AnalysisReport> orchestrateAnalysis(AnalysisPanel panel, List<DocumentChunk> chunks);
}
