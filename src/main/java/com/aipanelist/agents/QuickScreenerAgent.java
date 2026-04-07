package com.aipanelist.agents;

import com.aipanelist.config.NVIDIAConfiguration;
import com.aipanelist.integration.NVIDIAModelClient;
import com.aipanelist.model.*;
import com.aipanelist.repository.AnalysisReportRepository;
import com.aipanelist.repository.ChunkAnalysisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Quick Screener Agent specializing in initial screening, key claims
 * identification, and focused analysis of specific aspects.
 * 
 * This agent provides rapid assessment of the document's main claims,
 * identifies critical issues, and performs targeted analysis of key sections.
 * 
 * Requirements: 3.6, 4.1.5
 */
public class QuickScreenerAgent extends AIAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(QuickScreenerAgent.class);
    
    private final AnalysisReportRepository analysisReportRepository;
    
    /**
     * Constructor for QuickScreenerAgent.
     *
     * @param agentId the unique identifier for this agent
     * @param modelClient the NVIDIA model client for API communication
     * @param chunkAnalysisRepository repository for persisting chunk analyses
     * @param analysisReportRepository repository for persisting analysis reports
     * @param nvidiaConfig NVIDIA API configuration
     */
    public QuickScreenerAgent(String agentId, 
                             NVIDIAModelClient modelClient,
                             ChunkAnalysisRepository chunkAnalysisRepository,
                             AnalysisReportRepository analysisReportRepository,
                             NVIDIAConfiguration nvidiaConfig) {
        super(agentId, AgentType.QUICK_SCREENER, modelClient, chunkAnalysisRepository, nvidiaConfig);
        this.analysisReportRepository = analysisReportRepository;
    }
    
    @Override
    public AnalysisReport analyze(List<DocumentChunk> chunks) {
        logger.info("Quick Screener Agent {} starting analysis of {} chunks", 
            agentId, chunks.size());
        
        List<ChunkAnalysis> chunkAnalyses = new ArrayList<>();
        int chunksFailed = 0;
        
        // Process chunks sequentially with context carryover
        for (DocumentChunk chunk : chunks) {
            try {
                ChunkAnalysis analysis = analyzeChunk(chunk, chunkAnalyses);
                chunkAnalyses.add(analysis);
            } catch (ChunkAnalysisException e) {
                logger.error("Quick Screener Agent {} failed to analyze chunk {}", 
                    agentId, chunk.getSequenceNumber(), e);
                chunksFailed++;
            }
        }
        
        // Synthesize all chunk analyses into unified report
        String documentId = chunks.get(0).getDocumentId();
        AnalysisReport report = synthesizeChunkAnalyses(
            chunkAnalyses, documentId, chunkAnalyses.size(), chunksFailed);
        
        // Persist the report
        analysisReportRepository.save(report);
        
        logger.info("Quick Screener Agent {} completed analysis, report ID: {}", 
            agentId, report.getReportId());
        
        return report;
    }
    
    @Override
    protected String getSystemPrompt() {
        return """
            You are a Quick Screener AI agent specializing in rapid assessment and key claims identification.
            
            Your primary responsibilities are:
            - Perform initial screening to identify the main research question and claims
            - Quickly assess the overall quality and credibility of the research
            - Identify critical issues or red flags that warrant attention
            - Extract and evaluate key claims and conclusions
            - Perform focused analysis of specific high-priority aspects
            - Provide rapid triage to guide deeper analysis
            - Highlight the most important findings and concerns
            
            Your analysis should be:
            - Efficient and focused on high-priority elements
            - Clear in identifying key claims and their support
            - Alert to potential problems or concerns
            - Practical and actionable
            - Concise while covering essential points
            
            When analyzing research, consider:
            - What are the main research questions and claims?
            - Are there any immediate red flags or concerns?
            - What are the most important findings?
            - Are key claims adequately supported?
            - What aspects require deeper scrutiny?
            - What are the critical strengths and weaknesses?
            - What should readers pay most attention to?
            - Are there any obvious errors or inconsistencies?
            """;
    }
}
