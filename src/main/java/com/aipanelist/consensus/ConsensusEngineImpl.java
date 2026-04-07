package com.aipanelist.consensus;

import com.aipanelist.config.NVIDIAConfiguration;
import com.aipanelist.integration.NVIDIAModelClient;
import com.aipanelist.model.*;
import com.aipanelist.repository.ConsensusReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of ConsensusEngine that synthesizes agent reports into consensus.
 * 
 * Uses nvidia/llama-3.1-nemotron-70b-instruct to analyze multiple agent reports
 * and generate a unified consensus report with proper attribution.
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8
 */
@Service
public class ConsensusEngineImpl implements ConsensusEngine {

    private static final Logger logger = LoggerFactory.getLogger(ConsensusEngineImpl.class);
    
    private final NVIDIAModelClient modelClient;
    private final ConsensusReportRepository consensusReportRepository;
    private final NVIDIAConfiguration nvidiaConfig;
    
    public ConsensusEngineImpl(NVIDIAModelClient modelClient,
                              ConsensusReportRepository consensusReportRepository,
                              NVIDIAConfiguration nvidiaConfig) {
        this.modelClient = modelClient;
        this.consensusReportRepository = consensusReportRepository;
        this.nvidiaConfig = nvidiaConfig;
    }

    @Override
    public ConsensusReport generateConsensus(List<AnalysisReport> reports) throws ConsensusGenerationException {
        if (reports == null || reports.isEmpty()) {
            throw new ConsensusGenerationException("Cannot generate consensus from empty report list");
        }
        
        String documentId = reports.get(0).getDocumentId();
        logger.info("Generating consensus for document {} from {} agent reports", documentId, reports.size());
        
        try {
            // Build synthesis prompt
            String synthesisPrompt = buildSynthesisPrompt(reports);
            
            // Call NVIDIA model
            List<ModelRequest.Message> messages = new ArrayList<>();
            messages.add(ModelRequest.Message.builder()
                .role("system")
                .content("You are an expert at synthesizing multiple expert analyses into a unified consensus report. Analyze the provided agent reports and identify common themes, areas of agreement, areas of disagreement, and generate unified recommendations. Attribute insights to specific agent types.")
                .build());
            messages.add(ModelRequest.Message.builder()
                .role("user")
                .content(synthesisPrompt)
                .build());
            
            ModelRequest request = ModelRequest.builder()
                .model(nvidiaConfig.getModelName())
                .messages(messages)
                .temperature(0.7)
                .maxTokens(4000)
                .build();
            
            ModelResponse response = modelClient.sendRequest(request);
            String modelOutput = response.getContent();
            
            // Parse model response
            ConsensusData consensusData = parseModelResponse(modelOutput, reports);
            
            // Create and store consensus report
            String reportId = "consensus-" + UUID.randomUUID().toString();
            ConsensusReport consensusReport = new ConsensusReport(
                reportId,
                documentId,
                consensusData.commonThemes,
                consensusData.agreements,
                consensusData.disagreements,
                consensusData.unifiedRecommendations,
                consensusData.attributedInsights,
                LocalDateTime.now(),
                reports.size()
            );
            
            consensusReportRepository.save(consensusReport);
            logger.info("Consensus report {} generated and stored for document {}", reportId, documentId);
            
            return consensusReport;
            
        } catch (APIException e) {
            String errorMsg = "Failed to call NVIDIA model for consensus generation: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new ConsensusGenerationException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Unexpected error during consensus generation: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new ConsensusGenerationException(errorMsg, e);
        }
    }
    
    /**
     * Build the synthesis prompt from all agent reports.
     */
    private String buildSynthesisPrompt(List<AnalysisReport> reports) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze the following expert panel reports and synthesize them into a consensus.\n\n");
        prompt.append("Please provide your analysis in the following structured format:\n\n");
        prompt.append("## COMMON THEMES\n[List the major themes that appear across multiple agent reports]\n\n");
        prompt.append("## AGREEMENTS\n[List areas where agents agree or reach similar conclusions]\n\n");
        prompt.append("## DISAGREEMENTS\n[List areas where agents have differing opinions or conflicting findings]\n\n");
        prompt.append("## UNIFIED RECOMMENDATIONS\n[Provide synthesized recommendations that integrate insights from all agents]\n\n");
        prompt.append("## ATTRIBUTED INSIGHTS\n[Attribute specific key insights to their source agent types]\n\n");
        prompt.append("---\n\n");
        prompt.append("AGENT REPORTS:\n\n");
        
        for (int i = 0; i < reports.size(); i++) {
            AnalysisReport report = reports.get(i);
            prompt.append("### Report ").append(i + 1).append(": ").append(report.getAgentType()).append("\n\n");
            prompt.append("**Key Findings:**\n").append(report.getKeyFindings()).append("\n\n");
            prompt.append("**Strengths:**\n").append(report.getStrengths()).append("\n\n");
            prompt.append("**Weaknesses:**\n").append(report.getWeaknesses()).append("\n\n");
            prompt.append("**Recommendations:**\n").append(report.getRecommendations()).append("\n\n");
            prompt.append("---\n\n");
        }
        
        return prompt.toString();
    }
    
    /**
     * Parse the model response to extract consensus data.
     */
    private ConsensusData parseModelResponse(String modelOutput, List<AnalysisReport> reports) 
            throws ConsensusGenerationException {
        try {
            ConsensusData data = new ConsensusData();
            
            // Extract sections using regex patterns
            data.commonThemes = extractSection(modelOutput, "COMMON THEMES", "Common themes identified across agent reports");
            data.agreements = extractSection(modelOutput, "AGREEMENTS", "Areas of agreement among agents");
            data.disagreements = extractSection(modelOutput, "DISAGREEMENTS", "Areas of disagreement among agents");
            data.unifiedRecommendations = extractSection(modelOutput, "UNIFIED RECOMMENDATIONS", "Unified recommendations from panel");
            data.attributedInsights = extractSection(modelOutput, "ATTRIBUTED INSIGHTS", "Insights attributed to specific agents");
            
            // Fallback: if sections are empty, use the entire output
            if (data.commonThemes.isEmpty() && data.agreements.isEmpty()) {
                logger.warn("Could not parse structured sections from model output, using full output");
                data.commonThemes = modelOutput;
                data.agreements = "See common themes section";
                data.disagreements = "See common themes section";
                data.unifiedRecommendations = "See common themes section";
                data.attributedInsights = buildAttributionFromReports(reports);
            }
            
            return data;
            
        } catch (Exception e) {
            throw new ConsensusGenerationException("Failed to parse model response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extract a section from the model output.
     */
    private String extractSection(String text, String sectionHeader, String defaultValue) {
        // Try to find section with ## header
        Pattern pattern = Pattern.compile("##\\s*" + Pattern.quote(sectionHeader) + "\\s*\\n(.*?)(?=##|$)", 
                                         Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            String content = matcher.group(1).trim();
            return content.isEmpty() ? defaultValue : content;
        }
        
        // Try alternative format without ##
        pattern = Pattern.compile(Pattern.quote(sectionHeader) + "\\s*:?\\s*\\n(.*?)(?=\\n[A-Z][A-Z\\s]+:|$)", 
                                 Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            String content = matcher.group(1).trim();
            return content.isEmpty() ? defaultValue : content;
        }
        
        return defaultValue;
    }
    
    /**
     * Build attribution text from reports when model doesn't provide it.
     */
    private String buildAttributionFromReports(List<AnalysisReport> reports) {
        StringBuilder attribution = new StringBuilder();
        for (AnalysisReport report : reports) {
            attribution.append(report.getAgentType()).append(": ");
            attribution.append(report.getKeyFindings().substring(0, Math.min(200, report.getKeyFindings().length())));
            attribution.append("...\n\n");
        }
        return attribution.toString();
    }
    
    /**
     * Internal class to hold parsed consensus data.
     */
    private static class ConsensusData {
        String commonThemes = "";
        String agreements = "";
        String disagreements = "";
        String unifiedRecommendations = "";
        String attributedInsights = "";
    }
}
