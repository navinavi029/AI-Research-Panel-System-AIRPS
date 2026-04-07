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
 * Fact Extractor Agent specializing in extracting key facts, data points,
 * and creating structured summaries.
 * 
 * This agent focuses on identifying and organizing factual information,
 * numerical data, and key details from the research document.
 * 
 * Requirements: 3.7, 4.1.6
 */
public class FactExtractorAgent extends AIAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(FactExtractorAgent.class);
    
    private final AnalysisReportRepository analysisReportRepository;
    
    /**
     * Constructor for FactExtractorAgent.
     *
     * @param agentId the unique identifier for this agent
     * @param modelClient the NVIDIA model client for API communication
     * @param chunkAnalysisRepository repository for persisting chunk analyses
     * @param analysisReportRepository repository for persisting analysis reports
     * @param nvidiaConfig NVIDIA API configuration
     */
    public FactExtractorAgent(String agentId, 
                             NVIDIAModelClient modelClient,
                             ChunkAnalysisRepository chunkAnalysisRepository,
                             AnalysisReportRepository analysisReportRepository,
                             NVIDIAConfiguration nvidiaConfig) {
        super(agentId, AgentType.FACT_EXTRACTOR, modelClient, chunkAnalysisRepository, nvidiaConfig);
        this.analysisReportRepository = analysisReportRepository;
    }
    
    @Override
    public AnalysisReport analyze(List<DocumentChunk> chunks) {
        logger.info("Fact Extractor Agent {} starting analysis of {} chunks", 
            agentId, chunks.size());
        
        List<ChunkAnalysis> chunkAnalyses = new ArrayList<>();
        int chunksFailed = 0;
        
        // Process chunks sequentially with context carryover
        for (DocumentChunk chunk : chunks) {
            try {
                ChunkAnalysis analysis = analyzeChunk(chunk, chunkAnalyses);
                chunkAnalyses.add(analysis);
            } catch (ChunkAnalysisException e) {
                logger.error("Fact Extractor Agent {} failed to analyze chunk {}", 
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
        
        logger.info("Fact Extractor Agent {} completed analysis, report ID: {}", 
            agentId, report.getReportId());
        
        return report;
    }
    
    @Override
    protected String getSystemPrompt() {
        return """
            You are a Fact Extractor AI agent specializing in extracting and organizing factual information.
            
            Your primary responsibilities are:
            - Extract key facts, data points, and numerical information
            - Identify and organize important findings and results
            - Create structured summaries of research content
            - Catalog methodological details and parameters
            - Extract sample characteristics and demographics
            - Identify key measurements, outcomes, and effect sizes
            - Organize information in a clear, accessible format
            
            Your analysis should be:
            - Precise and factually accurate
            - Well-organized and structured
            - Comprehensive in coverage of key information
            - Clear and easy to reference
            - Focused on extracting rather than interpreting
            
            When analyzing research, consider:
            - What are the key factual claims and data points?
            - What are the main numerical results and statistics?
            - What are the sample characteristics (size, demographics, etc.)?
            - What methods and procedures were used?
            - What measurements and instruments were employed?
            - What are the primary outcomes and effect sizes?
            - What are the key dates, locations, and contextual details?
            - How can this information be organized for easy reference?
            """;
    }
}
