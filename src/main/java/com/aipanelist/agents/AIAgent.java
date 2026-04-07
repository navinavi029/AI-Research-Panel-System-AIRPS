package com.aipanelist.agents;

import com.aipanelist.config.NVIDIAConfiguration;
import com.aipanelist.integration.NVIDIAModelClient;
import com.aipanelist.model.*;
import com.aipanelist.repository.ChunkAnalysisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Abstract base class for AI agents that analyze research documents.
 * 
 * Provides common functionality for multi-chunk document processing including:
 * - Sequential chunk analysis with context carryover
 * - Retry logic with exponential backoff
 * - Chunk failure handling with gap tracking
 * - Synthesis of chunk analyses into unified reports
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.7, 11.1, 11.2, 11.6, 11.8, 11.9
 */
public abstract class AIAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(AIAgent.class);
    
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;
    
    protected final String agentId;
    protected final AgentType type;
    protected final NVIDIAModelClient modelClient;
    protected final ChunkAnalysisRepository chunkAnalysisRepository;
    protected final NVIDIAConfiguration nvidiaConfig;
    
    /**
     * Constructor for AIAgent.
     *
     * @param agentId the unique identifier for this agent
     * @param type the agent type specialization
     * @param modelClient the NVIDIA model client for API communication
     * @param chunkAnalysisRepository repository for persisting chunk analyses
     * @param nvidiaConfig NVIDIA API configuration
     */
    protected AIAgent(String agentId, AgentType type, NVIDIAModelClient modelClient,
                     ChunkAnalysisRepository chunkAnalysisRepository, NVIDIAConfiguration nvidiaConfig) {
        this.agentId = agentId;
        this.type = type;
        this.modelClient = modelClient;
        this.chunkAnalysisRepository = chunkAnalysisRepository;
        this.nvidiaConfig = nvidiaConfig;
    }
    
    /**
     * Analyze a list of document chunks and generate a comprehensive analysis report.
     * 
     * Processes chunks sequentially, maintaining context across chunks, and synthesizes
     * all chunk analyses into a unified report. Handles chunk failures gracefully by
     * continuing with remaining chunks and noting gaps in the final report.
     * 
     * Requirements: 4.2, 4.4, 11.1, 11.9
     *
     * @param chunks the list of document chunks to analyze
     * @return the complete analysis report
     */
    public abstract AnalysisReport analyze(List<DocumentChunk> chunks);
    
    /**
     * Get the system prompt specific to this agent's specialization.
     * 
     * The system prompt defines the agent's role, focus areas, and analysis approach.
     * Each agent specialization provides its own prompt tailored to its expertise.
     * 
     * Requirements: 4.1.1, 4.1.2, 4.1.3, 4.1.4, 4.1.5, 4.1.6
     *
     * @return the system prompt for this agent type
     */
    protected abstract String getSystemPrompt();
    
    /**
     * Analyze a single document chunk with context from previous analyses.
     * 
     * Implements retry logic with exponential backoff (3 attempts) and includes
     * summaries of previous chunk analyses to maintain context continuity.
     * 
     * Requirements: 4.3, 4.7, 11.2, 11.6
     *
     * @param chunk the document chunk to analyze
     * @param previousAnalyses list of analyses from previous chunks for context
     * @return the chunk analysis result
     * @throws ChunkAnalysisException if all retry attempts fail
     */
    protected ChunkAnalysis analyzeChunk(DocumentChunk chunk, List<ChunkAnalysis> previousAnalyses) {
        logger.info("Agent {} analyzing chunk {} of {}", 
            agentId, chunk.getSequenceNumber(), chunk.getTotalChunks());
        
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < MAX_RETRIES) {
            try {
                long startTime = System.currentTimeMillis();
                
                // Build context summary from previous analyses
                String contextSummary = buildContextSummary(previousAnalyses);
                
                // Create prompt with chunk text and context
                String prompt = buildChunkPrompt(chunk, contextSummary);
                
                // Send request to NVIDIA model
                ModelRequest request = ModelRequest.builder()
                    .model(nvidiaConfig.getModelName())
                    .messages(List.of(
                        ModelRequest.Message.builder()
                            .role("system")
                            .content(getSystemPrompt())
                            .build(),
                        ModelRequest.Message.builder()
                            .role("user")
                            .content(prompt)
                            .build()
                    ))
                    .build();
                
                ModelResponse response = modelClient.sendRequest(request);
                
                // Create and persist chunk analysis
                ChunkAnalysis analysis = new ChunkAnalysis(
                    UUID.randomUUID().toString(),
                    null, // reportId will be set during synthesis
                    chunk.getChunkId(),
                    chunk.getSequenceNumber(),
                    response.getContent(),
                    contextSummary,
                    LocalDateTime.now()
                );
                
                long elapsedMs = System.currentTimeMillis() - startTime;
                logger.info("Agent {} completed chunk {} in {}ms", 
                    agentId, chunk.getSequenceNumber(), elapsedMs);
                
                return analysis;
                
            } catch (APIException e) {
                lastException = e;
                attempt++;
                
                if (attempt < MAX_RETRIES) {
                    long backoffMs = INITIAL_BACKOFF_MS * (1L << (attempt - 1));
                    logger.warn("Agent {} chunk {} analysis attempt {} failed, retrying in {}ms: {}", 
                        agentId, chunk.getSequenceNumber(), attempt, backoffMs, e.getMessage());
                    
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ChunkAnalysisException(
                            "Interrupted during retry backoff", ie);
                    }
                } else {
                    logger.error("Agent {} chunk {} analysis failed after {} attempts", 
                        agentId, chunk.getSequenceNumber(), MAX_RETRIES, e);
                }
            }
        }
        
        throw new ChunkAnalysisException(
            String.format("Failed to analyze chunk %d after %d attempts", 
                chunk.getSequenceNumber(), MAX_RETRIES), 
            lastException);
    }
    
    /**
     * Synthesize multiple chunk analyses into a unified analysis report.
     * 
     * Combines findings from all chunk analyses, identifies cross-chunk themes,
     * resolves contradictions, and generates a coherent final report with
     * key findings, strengths, weaknesses, and recommendations.
     * 
     * Requirements: 4.4, 4.5, 11.8, 11.9
     *
     * @param chunkAnalyses list of chunk analyses to synthesize
     * @param documentId the document identifier
     * @param chunksAnalyzed number of chunks successfully analyzed
     * @param chunksFailed number of chunks that failed analysis
     * @return the synthesized analysis report
     */
    protected AnalysisReport synthesizeChunkAnalyses(List<ChunkAnalysis> chunkAnalyses,
                                                     String documentId,
                                                     int chunksAnalyzed,
                                                     int chunksFailed) {
        logger.info("Agent {} synthesizing {} chunk analyses", agentId, chunkAnalyses.size());
        
        try {
            // Build synthesis prompt with all chunk findings
            String synthesisPrompt = buildSynthesisPrompt(chunkAnalyses, chunksFailed);
            
            // Send synthesis request to NVIDIA model
            ModelRequest request = ModelRequest.builder()
                .model(nvidiaConfig.getModelName())
                .messages(List.of(
                    ModelRequest.Message.builder()
                        .role("system")
                        .content(getSystemPrompt())
                        .build(),
                    ModelRequest.Message.builder()
                        .role("user")
                        .content(synthesisPrompt)
                        .build()
                ))
                .build();
            
            ModelResponse response = modelClient.sendRequest(request);
            
            // Parse response into report sections
            ReportSections sections = parseReportSections(response.getContent());
            
            // Create analysis report
            String reportId = UUID.randomUUID().toString();
            AnalysisReport report = new AnalysisReport(
                reportId,
                documentId,
                agentId,
                type,
                sections.keyFindings,
                sections.strengths,
                sections.weaknesses,
                sections.recommendations,
                LocalDateTime.now(),
                chunksAnalyzed,
                chunksFailed
            );
            
            // Update chunk analyses with report ID and persist
            for (ChunkAnalysis analysis : chunkAnalyses) {
                analysis.setReportId(reportId);
                chunkAnalysisRepository.save(analysis);
            }
            
            logger.info("Agent {} completed synthesis for report {}", agentId, reportId);
            
            return report;
            
        } catch (APIException e) {
            logger.error("Agent {} synthesis failed", agentId, e);
            throw new SynthesisException("Failed to synthesize chunk analyses", e);
        }
    }
    
    /**
     * Build context summary from previous chunk analyses.
     * 
     * Requirements: 11.2
     */
    private String buildContextSummary(List<ChunkAnalysis> previousAnalyses) {
        if (previousAnalyses.isEmpty()) {
            return "This is the first chunk of the document.";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("Previous findings from earlier chunks:\n\n");
        
        for (ChunkAnalysis analysis : previousAnalyses) {
            summary.append(String.format("Chunk %d: %s\n\n", 
                analysis.getChunkSequence(),
                summarizeFindings(analysis.getFindings())));
        }
        
        return summary.toString();
    }
    
    /**
     * Build prompt for analyzing a single chunk.
     */
    private String buildChunkPrompt(DocumentChunk chunk, String contextSummary) {
        return String.format(
            """
            %s
            
            You are analyzing chunk %d of %d from a research document.
            
            CHUNK TEXT:
            %s
            
            Provide your analysis focusing on your area of expertise. Include:
            - Key findings from this chunk
            - Notable strengths or weaknesses
            - Important observations
            
            Keep your analysis focused and concise.
            """,
            contextSummary,
            chunk.getSequenceNumber(),
            chunk.getTotalChunks(),
            chunk.getChunkText()
        );
    }
    
    /**
     * Build prompt for synthesizing chunk analyses.
     */
    private String buildSynthesisPrompt(List<ChunkAnalysis> chunkAnalyses, int chunksFailed) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You have analyzed a research document in multiple chunks. ");
        prompt.append("Now synthesize your findings into a comprehensive analysis report.\n\n");
        
        if (chunksFailed > 0) {
            prompt.append(String.format("NOTE: %d chunk(s) failed analysis and are not included.\n\n", 
                chunksFailed));
        }
        
        prompt.append("CHUNK ANALYSES:\n\n");
        for (ChunkAnalysis analysis : chunkAnalyses) {
            prompt.append(String.format("Chunk %d:\n%s\n\n", 
                analysis.getChunkSequence(), analysis.getFindings()));
        }
        
        prompt.append("""
            
            Generate a unified analysis report with the following sections:
            
            KEY FINDINGS:
            [Synthesize the most important findings across all chunks]
            
            STRENGTHS:
            [Identify and explain the document's strengths]
            
            WEAKNESSES:
            [Identify and explain the document's weaknesses]
            
            RECOMMENDATIONS:
            [Provide actionable recommendations based on your analysis]
            
            Ensure your synthesis:
            - Identifies themes that span multiple chunks
            - Resolves any contradictions between chunk-level findings
            - Maintains coherence across all document sections
            - Focuses on your area of expertise
            """);
        
        return prompt.toString();
    }
    
    /**
     * Summarize findings for context carryover (max 200 words).
     */
    private String summarizeFindings(String findings) {
        // Simple truncation for now - could be enhanced with extractive summarization
        if (findings.length() <= 800) {
            return findings;
        }
        return findings.substring(0, 800) + "...";
    }
    
    /**
     * Parse report sections from model response.
     */
    private ReportSections parseReportSections(String content) {
        ReportSections sections = new ReportSections();
        
        // Simple section parsing based on headers
        String[] parts = content.split("(?i)(KEY FINDINGS:|STRENGTHS:|WEAKNESSES:|RECOMMENDATIONS:)");
        
        if (parts.length >= 5) {
            sections.keyFindings = parts[1].trim();
            sections.strengths = parts[2].trim();
            sections.weaknesses = parts[3].trim();
            sections.recommendations = parts[4].trim();
        } else {
            // Fallback if sections not clearly marked
            sections.keyFindings = content;
            sections.strengths = "See key findings";
            sections.weaknesses = "See key findings";
            sections.recommendations = "See key findings";
        }
        
        return sections;
    }
    
    /**
     * Helper class for parsed report sections.
     */
    private static class ReportSections {
        String keyFindings = "";
        String strengths = "";
        String weaknesses = "";
        String recommendations = "";
    }
    
    /**
     * Exception thrown when chunk analysis fails.
     */
    public static class ChunkAnalysisException extends RuntimeException {
        public ChunkAnalysisException(String message, Throwable cause) {
            super(message, cause);
        }
        
        public ChunkAnalysisException(String message) {
            super(message);
        }
    }
    
    /**
     * Exception thrown when synthesis fails.
     */
    public static class SynthesisException extends RuntimeException {
        public SynthesisException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    // Getters
    
    public String getAgentId() {
        return agentId;
    }
    
    public AgentType getType() {
        return type;
    }
}
