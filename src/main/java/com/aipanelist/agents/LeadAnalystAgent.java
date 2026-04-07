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
 * Lead Analyst Agent specializing in deep critical analysis, research validity,
 * and overall study quality assessment.
 * 
 * This agent provides comprehensive critical evaluation of research documents,
 * focusing on methodological rigor, validity of conclusions, and overall
 * scientific quality.
 * 
 * Requirements: 3.2, 4.1.1
 */
public class LeadAnalystAgent extends AIAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(LeadAnalystAgent.class);
    
    private final AnalysisReportRepository analysisReportRepository;
    
    /**
     * Constructor for LeadAnalystAgent.
     *
     * @param agentId the unique identifier for this agent
     * @param modelClient the NVIDIA model client for API communication
     * @param chunkAnalysisRepository repository for persisting chunk analyses
     * @param analysisReportRepository repository for persisting analysis reports
     * @param nvidiaConfig NVIDIA API configuration
     */
    public LeadAnalystAgent(String agentId, 
                           NVIDIAModelClient modelClient,
                           ChunkAnalysisRepository chunkAnalysisRepository,
                           AnalysisReportRepository analysisReportRepository,
                           NVIDIAConfiguration nvidiaConfig) {
        super(agentId, AgentType.LEAD_ANALYST, modelClient, chunkAnalysisRepository, nvidiaConfig);
        this.analysisReportRepository = analysisReportRepository;
    }
    
    @Override
    public AnalysisReport analyze(List<DocumentChunk> chunks) {
        logger.info("Lead Analyst Agent {} starting analysis of {} chunks", 
            agentId, chunks.size());
        
        List<ChunkAnalysis> chunkAnalyses = new ArrayList<>();
        int chunksFailed = 0;
        
        // Process chunks sequentially with context carryover
        for (DocumentChunk chunk : chunks) {
            try {
                ChunkAnalysis analysis = analyzeChunk(chunk, chunkAnalyses);
                chunkAnalyses.add(analysis);
            } catch (ChunkAnalysisException e) {
                logger.error("Lead Analyst Agent {} failed to analyze chunk {}", 
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
        
        logger.info("Lead Analyst Agent {} completed analysis, report ID: {}", 
            agentId, report.getReportId());
        
        return report;
    }
    
    @Override
    protected String getSystemPrompt() {
        return """
            You are a Lead Analyst AI agent specializing in deep critical analysis of research documents.
            
            Your primary responsibilities are:
            - Conduct comprehensive critical evaluation of research validity and scientific rigor
            - Assess the overall quality and credibility of the study
            - Identify fundamental strengths and weaknesses in the research approach
            - Evaluate whether conclusions are supported by the evidence presented
            - Assess the significance and impact of the research findings
            - Identify potential biases, confounding factors, or methodological flaws
            - Provide high-level strategic recommendations for improvement
            
            Your analysis should be:
            - Rigorous and evidence-based
            - Balanced, highlighting both strengths and weaknesses
            - Focused on the big picture and overall research quality
            - Critical but constructive
            - Grounded in scientific principles and best practices
            
            When analyzing research, consider:
            - Is the research question clearly defined and significant?
            - Are the methods appropriate for answering the research question?
            - Is the evidence sufficient to support the conclusions?
            - Are alternative explanations adequately addressed?
            - What are the key limitations and how do they affect the findings?
            - What is the overall contribution to the field?
            """;
    }
}
