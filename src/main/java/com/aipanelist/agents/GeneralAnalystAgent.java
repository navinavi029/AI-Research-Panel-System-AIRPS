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
 * General Analyst Agent specializing in comprehensive document review covering
 * all aspects of the research.
 * 
 * This agent provides broad, holistic analysis examining all dimensions of the
 * research document including content, structure, clarity, and completeness.
 * 
 * Requirements: 3.3, 4.1.2
 */
public class GeneralAnalystAgent extends AIAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(GeneralAnalystAgent.class);
    
    private final AnalysisReportRepository analysisReportRepository;
    
    /**
     * Constructor for GeneralAnalystAgent.
     *
     * @param agentId the unique identifier for this agent
     * @param modelClient the NVIDIA model client for API communication
     * @param chunkAnalysisRepository repository for persisting chunk analyses
     * @param analysisReportRepository repository for persisting analysis reports
     * @param nvidiaConfig NVIDIA API configuration
     */
    public GeneralAnalystAgent(String agentId, 
                              NVIDIAModelClient modelClient,
                              ChunkAnalysisRepository chunkAnalysisRepository,
                              AnalysisReportRepository analysisReportRepository,
                              NVIDIAConfiguration nvidiaConfig) {
        super(agentId, AgentType.GENERAL_ANALYST, modelClient, chunkAnalysisRepository, nvidiaConfig);
        this.analysisReportRepository = analysisReportRepository;
    }
    
    @Override
    public AnalysisReport analyze(List<DocumentChunk> chunks) {
        logger.info("General Analyst Agent {} starting analysis of {} chunks", 
            agentId, chunks.size());
        
        List<ChunkAnalysis> chunkAnalyses = new ArrayList<>();
        int chunksFailed = 0;
        
        // Process chunks sequentially with context carryover
        for (DocumentChunk chunk : chunks) {
            try {
                ChunkAnalysis analysis = analyzeChunk(chunk, chunkAnalyses);
                chunkAnalyses.add(analysis);
            } catch (ChunkAnalysisException e) {
                logger.error("General Analyst Agent {} failed to analyze chunk {}", 
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
        
        logger.info("General Analyst Agent {} completed analysis, report ID: {}", 
            agentId, report.getReportId());
        
        return report;
    }
    
    @Override
    protected String getSystemPrompt() {
        return """
            You are a General Analyst AI agent specializing in comprehensive review of research documents.
            
            Your primary responsibilities are:
            - Provide broad, holistic analysis covering all aspects of the document
            - Examine content quality, organization, and presentation
            - Assess clarity and accessibility of the writing
            - Evaluate completeness and thoroughness of the research
            - Identify gaps in coverage or missing elements
            - Review the logical flow and structure of arguments
            - Assess the overall coherence and consistency of the document
            
            Your analysis should be:
            - Comprehensive and multi-dimensional
            - Balanced across all aspects of the document
            - Attentive to both content and presentation
            - Focused on the reader's experience and understanding
            - Constructive and actionable
            
            When analyzing research, consider:
            - Is the document well-organized and easy to follow?
            - Are key concepts clearly explained and defined?
            - Is the writing clear, precise, and appropriate for the audience?
            - Are all necessary sections present and adequately developed?
            - Is the document internally consistent?
            - Are figures, tables, and supplementary materials effective?
            - What would improve the overall quality and impact of the document?
            """;
    }
}
