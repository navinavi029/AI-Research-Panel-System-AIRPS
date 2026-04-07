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
 * Literature Reviewer Agent specializing in literature context, citations,
 * theoretical framework, and related work analysis.
 * 
 * This agent evaluates how the research fits within the broader scholarly
 * context, assesses citation quality, and reviews theoretical foundations.
 * 
 * Requirements: 3.5, 4.1.4
 */
public class LiteratureReviewerAgent extends AIAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(LiteratureReviewerAgent.class);
    
    private final AnalysisReportRepository analysisReportRepository;
    
    /**
     * Constructor for LiteratureReviewerAgent.
     *
     * @param agentId the unique identifier for this agent
     * @param modelClient the NVIDIA model client for API communication
     * @param chunkAnalysisRepository repository for persisting chunk analyses
     * @param analysisReportRepository repository for persisting analysis reports
     * @param nvidiaConfig NVIDIA API configuration
     */
    public LiteratureReviewerAgent(String agentId, 
                                  NVIDIAModelClient modelClient,
                                  ChunkAnalysisRepository chunkAnalysisRepository,
                                  AnalysisReportRepository analysisReportRepository,
                                  NVIDIAConfiguration nvidiaConfig) {
        super(agentId, AgentType.LITERATURE_REVIEWER, modelClient, chunkAnalysisRepository, nvidiaConfig);
        this.analysisReportRepository = analysisReportRepository;
    }
    
    @Override
    public AnalysisReport analyze(List<DocumentChunk> chunks) {
        logger.info("Literature Reviewer Agent {} starting analysis of {} chunks", 
            agentId, chunks.size());
        
        List<ChunkAnalysis> chunkAnalyses = new ArrayList<>();
        int chunksFailed = 0;
        
        // Process chunks sequentially with context carryover
        for (DocumentChunk chunk : chunks) {
            try {
                ChunkAnalysis analysis = analyzeChunk(chunk, chunkAnalyses);
                chunkAnalyses.add(analysis);
            } catch (ChunkAnalysisException e) {
                logger.error("Literature Reviewer Agent {} failed to analyze chunk {}", 
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
        
        logger.info("Literature Reviewer Agent {} completed analysis, report ID: {}", 
            agentId, report.getReportId());
        
        return report;
    }
    
    @Override
    protected String getSystemPrompt() {
        return """
            You are a Literature Reviewer AI agent specializing in scholarly context and theoretical foundations.
            
            Your primary responsibilities are:
            - Evaluate the quality and comprehensiveness of the literature review
            - Assess how the research fits within the broader scholarly context
            - Review the theoretical framework and conceptual foundations
            - Evaluate citation practices and reference quality
            - Identify gaps in the literature coverage
            - Assess the novelty and contribution relative to existing work
            - Review the integration of prior research into the current study
            
            Your analysis should be:
            - Contextually aware of the broader research landscape
            - Focused on theoretical coherence and scholarly positioning
            - Attentive to citation quality and completeness
            - Critical of gaps in literature coverage
            - Constructive in identifying relevant missing references
            
            When analyzing research, consider:
            - Is the literature review comprehensive and up-to-date?
            - Are key theoretical concepts clearly defined and grounded in literature?
            - Are citations appropriate, accurate, and sufficient?
            - How does this work relate to and build upon prior research?
            - Are there important gaps in the literature coverage?
            - Is the theoretical framework coherent and well-justified?
            - Does the research make a clear contribution to the field?
            - Are alternative theoretical perspectives adequately considered?
            """;
    }
}
