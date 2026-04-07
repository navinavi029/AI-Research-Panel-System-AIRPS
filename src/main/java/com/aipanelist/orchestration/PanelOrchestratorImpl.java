package com.aipanelist.orchestration;

import com.aipanelist.agents.*;
import com.aipanelist.config.NVIDIAConfiguration;
import com.aipanelist.integration.NVIDIAModelClient;
import com.aipanelist.model.AgentProgress;
import com.aipanelist.model.AnalysisReport;
import com.aipanelist.model.DocumentChunk;
import com.aipanelist.repository.AgentProgressRepository;
import com.aipanelist.repository.AnalysisReportRepository;
import com.aipanelist.repository.ChunkAnalysisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Implementation of PanelOrchestrator that creates and coordinates AI agent panels.
 * 
 * Creates panels of 6 specialized agents, orchestrates their parallel analysis,
 * handles failures gracefully, and tracks progress through AgentProgress entities.
 * 
 * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 9.3, 10.9
 */
@Service
public class PanelOrchestratorImpl implements PanelOrchestrator {
    
    private static final Logger logger = LoggerFactory.getLogger(PanelOrchestratorImpl.class);
    private static final int AGENT_TIMEOUT_MINUTES = 30;
    
    private final NVIDIAModelClient modelClient;
    private final ChunkAnalysisRepository chunkAnalysisRepository;
    private final AnalysisReportRepository analysisReportRepository;
    private final AgentProgressRepository agentProgressRepository;
    private final NVIDIAConfiguration nvidiaConfig;
    private final ExecutorService executorService;
    
    /**
     * Constructor for PanelOrchestratorImpl.
     *
     * @param modelClient the NVIDIA model client for API communication
     * @param chunkAnalysisRepository repository for persisting chunk analyses
     * @param analysisReportRepository repository for persisting analysis reports
     * @param agentProgressRepository repository for tracking agent progress
     * @param nvidiaConfig NVIDIA API configuration
     */
    public PanelOrchestratorImpl(NVIDIAModelClient modelClient,
                                ChunkAnalysisRepository chunkAnalysisRepository,
                                AnalysisReportRepository analysisReportRepository,
                                AgentProgressRepository agentProgressRepository,
                                NVIDIAConfiguration nvidiaConfig) {
        this.modelClient = modelClient;
        this.chunkAnalysisRepository = chunkAnalysisRepository;
        this.analysisReportRepository = analysisReportRepository;
        this.agentProgressRepository = agentProgressRepository;
        this.nvidiaConfig = nvidiaConfig;
        this.executorService = Executors.newFixedThreadPool(6);
    }
    
    @Override
    public AnalysisPanel createPanel(String documentId) {
        logger.info("Creating analysis panel for document {}", documentId);
        
        List<AIAgent> agents = new ArrayList<>();
        
        // Create Lead Analyst Agent
        String leadAnalystId = UUID.randomUUID().toString();
        agents.add(new LeadAnalystAgent(
            leadAnalystId,
            modelClient,
            chunkAnalysisRepository,
            analysisReportRepository,
            nvidiaConfig
        ));
        logger.debug("Created Lead Analyst Agent with ID: {}", leadAnalystId);
        
        // Create General Analyst Agent
        String generalAnalystId = UUID.randomUUID().toString();
        agents.add(new GeneralAnalystAgent(
            generalAnalystId,
            modelClient,
            chunkAnalysisRepository,
            analysisReportRepository,
            nvidiaConfig
        ));
        logger.debug("Created General Analyst Agent with ID: {}", generalAnalystId);
        
        // Create Methodology Reviewer Agent
        String methodologyReviewerId = UUID.randomUUID().toString();
        agents.add(new MethodologyReviewerAgent(
            methodologyReviewerId,
            modelClient,
            chunkAnalysisRepository,
            analysisReportRepository,
            nvidiaConfig
        ));
        logger.debug("Created Methodology Reviewer Agent with ID: {}", methodologyReviewerId);
        
        // Create Literature Reviewer Agent
        String literatureReviewerId = UUID.randomUUID().toString();
        agents.add(new LiteratureReviewerAgent(
            literatureReviewerId,
            modelClient,
            chunkAnalysisRepository,
            analysisReportRepository,
            nvidiaConfig
        ));
        logger.debug("Created Literature Reviewer Agent with ID: {}", literatureReviewerId);
        
        // Create Quick Screener Agent
        String quickScreenerId = UUID.randomUUID().toString();
        agents.add(new QuickScreenerAgent(
            quickScreenerId,
            modelClient,
            chunkAnalysisRepository,
            analysisReportRepository,
            nvidiaConfig
        ));
        logger.debug("Created Quick Screener Agent with ID: {}", quickScreenerId);
        
        // Create Fact Extractor Agent
        String factExtractorId = UUID.randomUUID().toString();
        agents.add(new FactExtractorAgent(
            factExtractorId,
            modelClient,
            chunkAnalysisRepository,
            analysisReportRepository,
            nvidiaConfig
        ));
        logger.debug("Created Fact Extractor Agent with ID: {}", factExtractorId);
        
        AnalysisPanel panel = new AnalysisPanel(documentId, agents);
        logger.info("Successfully created panel with {} agents for document {}", 
            agents.size(), documentId);
        
        return panel;
    }
    
    @Override
    public List<AnalysisReport> orchestrateAnalysis(AnalysisPanel panel, List<DocumentChunk> chunks) {
        String documentId = panel.getDocumentId();
        logger.info("Orchestrating analysis for document {} with {} agents and {} chunks",
            documentId, panel.getAgents().size(), chunks.size());
        
        // Initialize progress tracking for all agents
        initializeAgentProgress(panel, chunks.size());
        
        // Submit all agent tasks in parallel
        List<Future<AnalysisReport>> futures = new ArrayList<>();
        for (AIAgent agent : panel.getAgents()) {
            Future<AnalysisReport> future = executorService.submit(() -> {
                try {
                    return analyzeWithProgressTracking(agent, chunks, documentId);
                } catch (Exception e) {
                    logger.error("Agent {} failed during analysis", agent.getAgentId(), e);
                    throw e;
                }
            });
            futures.add(future);
        }
        
        // Collect results with timeout
        List<AnalysisReport> reports = new ArrayList<>();
        List<AIAgent> agents = panel.getAgents();
        
        for (int i = 0; i < futures.size(); i++) {
            AIAgent agent = agents.get(i);
            Future<AnalysisReport> future = futures.get(i);
            
            try {
                AnalysisReport report = future.get(AGENT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                reports.add(report);
                logger.info("Agent {} ({}) completed successfully", 
                    agent.getAgentId(), agent.getType());
            } catch (TimeoutException e) {
                logger.error("Agent {} ({}) timed out after {} minutes",
                    agent.getAgentId(), agent.getType(), AGENT_TIMEOUT_MINUTES);
                future.cancel(true);
            } catch (InterruptedException e) {
                logger.error("Agent {} ({}) was interrupted",
                    agent.getAgentId(), agent.getType(), e);
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                logger.error("Agent {} ({}) failed with exception",
                    agent.getAgentId(), agent.getType(), e.getCause());
            }
        }
        
        logger.info("Analysis orchestration completed for document {}. " +
            "Successful agents: {}/{}", documentId, reports.size(), agents.size());
        
        return reports;
    }
    
    /**
     * Initialize AgentProgress entities for all agents in the panel.
     */
    private void initializeAgentProgress(AnalysisPanel panel, int totalChunks) {
        String documentId = panel.getDocumentId();
        
        for (AIAgent agent : panel.getAgents()) {
            AgentProgress progress = new AgentProgress(
                UUID.randomUUID().toString(),
                documentId,
                agent.getAgentId(),
                agent.getType(),
                0,
                totalChunks,
                LocalDateTime.now()
            );
            agentProgressRepository.save(progress);
            logger.debug("Initialized progress tracking for agent {} ({})",
                agent.getAgentId(), agent.getType());
        }
    }
    
    /**
     * Analyze chunks with progress tracking updates.
     * 
     * Wraps the agent's analyze method to update AgentProgress after each chunk.
     */
    private AnalysisReport analyzeWithProgressTracking(AIAgent agent, 
                                                       List<DocumentChunk> chunks,
                                                       String documentId) {
        // Create a custom agent wrapper that updates progress
        ProgressTrackingAgent trackingAgent = new ProgressTrackingAgent(
            agent, documentId, agentProgressRepository);
        
        return trackingAgent.analyze(chunks);
    }
    
    /**
     * Wrapper class that tracks progress during chunk analysis.
     */
    private static class ProgressTrackingAgent {
        private final AIAgent delegate;
        private final String documentId;
        private final AgentProgressRepository progressRepository;
        
        public ProgressTrackingAgent(AIAgent delegate, 
                                    String documentId,
                                    AgentProgressRepository progressRepository) {
            this.delegate = delegate;
            this.documentId = documentId;
            this.progressRepository = progressRepository;
        }
        
        public AnalysisReport analyze(List<DocumentChunk> chunks) {
            // Note: The actual progress tracking happens inside AIAgent.analyzeChunk
            // We would need to modify AIAgent to accept a progress callback
            // For now, we'll update progress after analysis completes
            
            AnalysisReport report = delegate.analyze(chunks);
            
            // Update final progress
            progressRepository.findByDocumentIdAndAgentId(documentId, delegate.getAgentId())
                .ifPresent(progress -> {
                    progress.setChunksCompleted(report.getChunksAnalyzed());
                    progress.setLastUpdated(LocalDateTime.now());
                    progressRepository.save(progress);
                });
            
            return report;
        }
    }
}
