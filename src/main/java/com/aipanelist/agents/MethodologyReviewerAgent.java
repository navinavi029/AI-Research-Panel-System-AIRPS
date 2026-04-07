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
 * Methodology Reviewer Agent specializing in research methodology, statistical
 * approaches, and data analysis techniques.
 * 
 * This agent provides expert evaluation of research methods, experimental design,
 * statistical analysis, and data handling procedures.
 * 
 * Requirements: 3.4, 4.1.3
 */
public class MethodologyReviewerAgent extends AIAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(MethodologyReviewerAgent.class);
    
    private final AnalysisReportRepository analysisReportRepository;
    
    /**
     * Constructor for MethodologyReviewerAgent.
     *
     * @param agentId the unique identifier for this agent
     * @param modelClient the NVIDIA model client for API communication
     * @param chunkAnalysisRepository repository for persisting chunk analyses
     * @param analysisReportRepository repository for persisting analysis reports
     * @param nvidiaConfig NVIDIA API configuration
     */
    public MethodologyReviewerAgent(String agentId, 
                                   NVIDIAModelClient modelClient,
                                   ChunkAnalysisRepository chunkAnalysisRepository,
                                   AnalysisReportRepository analysisReportRepository,
                                   NVIDIAConfiguration nvidiaConfig) {
        super(agentId, AgentType.METHODOLOGY_REVIEWER, modelClient, chunkAnalysisRepository, nvidiaConfig);
        this.analysisReportRepository = analysisReportRepository;
    }
    
    @Override
    public AnalysisReport analyze(List<DocumentChunk> chunks) {
        logger.info("Methodology Reviewer Agent {} starting analysis of {} chunks", 
            agentId, chunks.size());
        
        List<ChunkAnalysis> chunkAnalyses = new ArrayList<>();
        int chunksFailed = 0;
        
        // Process chunks sequentially with context carryover
        for (DocumentChunk chunk : chunks) {
            try {
                ChunkAnalysis analysis = analyzeChunk(chunk, chunkAnalyses);
                chunkAnalyses.add(analysis);
            } catch (ChunkAnalysisException e) {
                logger.error("Methodology Reviewer Agent {} failed to analyze chunk {}", 
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
        
        logger.info("Methodology Reviewer Agent {} completed analysis, report ID: {}", 
            agentId, report.getReportId());
        
        return report;
    }
    
    @Override
    protected String getSystemPrompt() {
        return """
            You are a Methodology Reviewer AI agent specializing in research methodology and statistical analysis.
            
            Your primary responsibilities are:
            - Evaluate the appropriateness and rigor of research methods
            - Assess experimental design, sampling strategies, and data collection procedures
            - Review statistical approaches and analytical techniques
            - Identify potential methodological flaws or limitations
            - Evaluate the validity and reliability of measurements
            - Assess control of confounding variables and sources of bias
            - Review data handling, processing, and quality control procedures
            
            Your analysis should be:
            - Technically rigorous and methodologically sound
            - Focused on the validity and reliability of the research approach
            - Attentive to statistical assumptions and their violations
            - Critical of methodological weaknesses
            - Constructive in suggesting improvements
            
            When analyzing research, consider:
            - Is the research design appropriate for the research question?
            - Are the methods clearly described and reproducible?
            - Is the sample size adequate and appropriately justified?
            - Are statistical tests appropriate for the data and hypotheses?
            - Are assumptions of statistical tests met?
            - Are potential sources of bias adequately controlled?
            - Is the data analysis transparent and well-documented?
            - What methodological improvements would strengthen the research?
            """;
    }
}
