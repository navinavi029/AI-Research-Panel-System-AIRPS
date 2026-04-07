package com.aipanelist.agents;

import com.aipanelist.config.NVIDIAConfiguration;
import com.aipanelist.integration.NVIDIAModelClient;
import com.aipanelist.model.*;
import com.aipanelist.repository.AnalysisReportRepository;
import com.aipanelist.repository.ChunkAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for specialized AI agent implementations.
 * Tests that each agent type has correct specialization and system prompts.
 * 
 * Requirements: 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 4.1.1, 4.1.2, 4.1.3, 4.1.4, 4.1.5, 4.1.6, 4.1.7
 */
@ExtendWith(MockitoExtension.class)
class SpecializedAgentsTest {
    
    @Mock
    private NVIDIAModelClient modelClient;
    
    @Mock
    private ChunkAnalysisRepository chunkAnalysisRepository;
    
    @Mock
    private AnalysisReportRepository analysisReportRepository;
    
    @Mock
    private NVIDIAConfiguration nvidiaConfig;
    
    private LeadAnalystAgent leadAnalyst;
    private GeneralAnalystAgent generalAnalyst;
    private MethodologyReviewerAgent methodologyReviewer;
    private LiteratureReviewerAgent literatureReviewer;
    private QuickScreenerAgent quickScreener;
    private FactExtractorAgent factExtractor;
    
    @BeforeEach
    void setUp() {
        when(nvidiaConfig.getModelName()).thenReturn("meta/llama-3_3-70b-instruct");
        leadAnalyst = new LeadAnalystAgent("lead-1", modelClient, 
            chunkAnalysisRepository, analysisReportRepository, nvidiaConfig);
        generalAnalyst = new GeneralAnalystAgent("general-1", modelClient, 
            chunkAnalysisRepository, analysisReportRepository, nvidiaConfig);
        methodologyReviewer = new MethodologyReviewerAgent("methodology-1", modelClient, 
            chunkAnalysisRepository, analysisReportRepository, nvidiaConfig);
        literatureReviewer = new LiteratureReviewerAgent("literature-1", modelClient, 
            chunkAnalysisRepository, analysisReportRepository, nvidiaConfig);
        quickScreener = new QuickScreenerAgent("screener-1", modelClient, 
            chunkAnalysisRepository, analysisReportRepository, nvidiaConfig);
        factExtractor = new FactExtractorAgent("extractor-1", modelClient, 
            chunkAnalysisRepository, analysisReportRepository, nvidiaConfig);
    }
    
    @Test
    void leadAnalystAgent_HasCorrectType() {
        assertThat(leadAnalyst.getType()).isEqualTo(AgentType.LEAD_ANALYST);
    }
    
    @Test
    void leadAnalystAgent_HasSystemPromptForCriticalAnalysis() {
        String prompt = leadAnalyst.getSystemPrompt();
        
        assertThat(prompt).containsIgnoringCase("Lead Analyst");
        assertThat(prompt).containsIgnoringCase("critical analysis");
        assertThat(prompt).containsIgnoringCase("research validity");
        assertThat(prompt).containsIgnoringCase("overall quality");
    }
    
    @Test
    void leadAnalystAgent_AnalyzesAndPersistsReport() throws APIException {
        // Arrange
        DocumentChunk chunk = createChunk(1, 1, "Research content");
        ModelResponse chunkResponse = createModelResponse("Critical analysis findings");
        ModelResponse synthesisResponse = createModelResponse(
            "KEY FINDINGS:\nCritical issues\nSTRENGTHS:\nStrong methodology\n" +
            "WEAKNESSES:\nLimited sample\nRECOMMENDATIONS:\nExpand study");
        
        when(modelClient.sendRequest(any(ModelRequest.class)))
            .thenReturn(chunkResponse)
            .thenReturn(synthesisResponse);
        
        // Act
        AnalysisReport report = leadAnalyst.analyze(List.of(chunk));
        
        // Assert
        assertThat(report.getAgentType()).isEqualTo(AgentType.LEAD_ANALYST);
        assertThat(report.getAgentId()).isEqualTo("lead-1");
        verify(analysisReportRepository).save(report);
    }
    
    @Test
    void generalAnalystAgent_HasCorrectType() {
        assertThat(generalAnalyst.getType()).isEqualTo(AgentType.GENERAL_ANALYST);
    }
    
    @Test
    void generalAnalystAgent_HasSystemPromptForComprehensiveReview() {
        String prompt = generalAnalyst.getSystemPrompt();
        
        assertThat(prompt).containsIgnoringCase("General Analyst");
        assertThat(prompt).containsIgnoringCase("comprehensive");
        assertThat(prompt).containsIgnoringCase("all aspects");
        assertThat(prompt).containsIgnoringCase("holistic");
    }
    
    @Test
    void generalAnalystAgent_AnalyzesAndPersistsReport() throws APIException {
        // Arrange
        DocumentChunk chunk = createChunk(1, 1, "Research content");
        ModelResponse chunkResponse = createModelResponse("Comprehensive analysis");
        ModelResponse synthesisResponse = createModelResponse(
            "KEY FINDINGS:\nOverall assessment\nSTRENGTHS:\nWell organized\n" +
            "WEAKNESSES:\nSome gaps\nRECOMMENDATIONS:\nImprove clarity");
        
        when(modelClient.sendRequest(any(ModelRequest.class)))
            .thenReturn(chunkResponse)
            .thenReturn(synthesisResponse);
        
        // Act
        AnalysisReport report = generalAnalyst.analyze(List.of(chunk));
        
        // Assert
        assertThat(report.getAgentType()).isEqualTo(AgentType.GENERAL_ANALYST);
        assertThat(report.getAgentId()).isEqualTo("general-1");
        verify(analysisReportRepository).save(report);
    }
    
    @Test
    void methodologyReviewerAgent_HasCorrectType() {
        assertThat(methodologyReviewer.getType()).isEqualTo(AgentType.METHODOLOGY_REVIEWER);
    }
    
    @Test
    void methodologyReviewerAgent_HasSystemPromptForMethodologyAnalysis() {
        String prompt = methodologyReviewer.getSystemPrompt();
        
        assertThat(prompt).containsIgnoringCase("Methodology Reviewer");
        assertThat(prompt).containsIgnoringCase("research methodology");
        assertThat(prompt).containsIgnoringCase("statistical");
        assertThat(prompt).containsIgnoringCase("data analysis");
    }
    
    @Test
    void methodologyReviewerAgent_AnalyzesAndPersistsReport() throws APIException {
        // Arrange
        DocumentChunk chunk = createChunk(1, 1, "Methods section");
        ModelResponse chunkResponse = createModelResponse("Methodology assessment");
        ModelResponse synthesisResponse = createModelResponse(
            "KEY FINDINGS:\nSound methods\nSTRENGTHS:\nRigorous design\n" +
            "WEAKNESSES:\nSmall sample\nRECOMMENDATIONS:\nPower analysis");
        
        when(modelClient.sendRequest(any(ModelRequest.class)))
            .thenReturn(chunkResponse)
            .thenReturn(synthesisResponse);
        
        // Act
        AnalysisReport report = methodologyReviewer.analyze(List.of(chunk));
        
        // Assert
        assertThat(report.getAgentType()).isEqualTo(AgentType.METHODOLOGY_REVIEWER);
        assertThat(report.getAgentId()).isEqualTo("methodology-1");
        verify(analysisReportRepository).save(report);
    }
    
    @Test
    void literatureReviewerAgent_HasCorrectType() {
        assertThat(literatureReviewer.getType()).isEqualTo(AgentType.LITERATURE_REVIEWER);
    }
    
    @Test
    void literatureReviewerAgent_HasSystemPromptForLiteratureAnalysis() {
        String prompt = literatureReviewer.getSystemPrompt();
        
        assertThat(prompt).containsIgnoringCase("Literature Reviewer");
        assertThat(prompt).containsIgnoringCase("literature");
        assertThat(prompt).containsIgnoringCase("citations");
        assertThat(prompt).containsIgnoringCase("theoretical framework");
    }
    
    @Test
    void literatureReviewerAgent_AnalyzesAndPersistsReport() throws APIException {
        // Arrange
        DocumentChunk chunk = createChunk(1, 1, "Literature review");
        ModelResponse chunkResponse = createModelResponse("Literature assessment");
        ModelResponse synthesisResponse = createModelResponse(
            "KEY FINDINGS:\nGood coverage\nSTRENGTHS:\nCurrent references\n" +
            "WEAKNESSES:\nMissing key papers\nRECOMMENDATIONS:\nAdd recent studies");
        
        when(modelClient.sendRequest(any(ModelRequest.class)))
            .thenReturn(chunkResponse)
            .thenReturn(synthesisResponse);
        
        // Act
        AnalysisReport report = literatureReviewer.analyze(List.of(chunk));
        
        // Assert
        assertThat(report.getAgentType()).isEqualTo(AgentType.LITERATURE_REVIEWER);
        assertThat(report.getAgentId()).isEqualTo("literature-1");
        verify(analysisReportRepository).save(report);
    }
    
    @Test
    void quickScreenerAgent_HasCorrectType() {
        assertThat(quickScreener.getType()).isEqualTo(AgentType.QUICK_SCREENER);
    }
    
    @Test
    void quickScreenerAgent_HasSystemPromptForRapidScreening() {
        String prompt = quickScreener.getSystemPrompt();
        
        assertThat(prompt).containsIgnoringCase("Quick Screener");
        assertThat(prompt).containsIgnoringCase("screening");
        assertThat(prompt).containsIgnoringCase("key claims");
        assertThat(prompt).containsIgnoringCase("rapid");
    }
    
    @Test
    void quickScreenerAgent_AnalyzesAndPersistsReport() throws APIException {
        // Arrange
        DocumentChunk chunk = createChunk(1, 1, "Research abstract");
        ModelResponse chunkResponse = createModelResponse("Quick screening results");
        ModelResponse synthesisResponse = createModelResponse(
            "KEY FINDINGS:\nMain claims identified\nSTRENGTHS:\nClear hypothesis\n" +
            "WEAKNESSES:\nLimited evidence\nRECOMMENDATIONS:\nDeeper analysis needed");
        
        when(modelClient.sendRequest(any(ModelRequest.class)))
            .thenReturn(chunkResponse)
            .thenReturn(synthesisResponse);
        
        // Act
        AnalysisReport report = quickScreener.analyze(List.of(chunk));
        
        // Assert
        assertThat(report.getAgentType()).isEqualTo(AgentType.QUICK_SCREENER);
        assertThat(report.getAgentId()).isEqualTo("screener-1");
        verify(analysisReportRepository).save(report);
    }
    
    @Test
    void factExtractorAgent_HasCorrectType() {
        assertThat(factExtractor.getType()).isEqualTo(AgentType.FACT_EXTRACTOR);
    }
    
    @Test
    void factExtractorAgent_HasSystemPromptForFactExtraction() {
        String prompt = factExtractor.getSystemPrompt();
        
        assertThat(prompt).containsIgnoringCase("Fact Extractor");
        assertThat(prompt).containsIgnoringCase("extract");
        assertThat(prompt).containsIgnoringCase("facts");
        assertThat(prompt).containsIgnoringCase("data points");
    }
    
    @Test
    void factExtractorAgent_AnalyzesAndPersistsReport() throws APIException {
        // Arrange
        DocumentChunk chunk = createChunk(1, 1, "Results section");
        ModelResponse chunkResponse = createModelResponse("Extracted facts");
        ModelResponse synthesisResponse = createModelResponse(
            "KEY FINDINGS:\nN=100, p<0.05\nSTRENGTHS:\nClear data\n" +
            "WEAKNESSES:\nLimited metrics\nRECOMMENDATIONS:\nAdd effect sizes");
        
        when(modelClient.sendRequest(any(ModelRequest.class)))
            .thenReturn(chunkResponse)
            .thenReturn(synthesisResponse);
        
        // Act
        AnalysisReport report = factExtractor.analyze(List.of(chunk));
        
        // Assert
        assertThat(report.getAgentType()).isEqualTo(AgentType.FACT_EXTRACTOR);
        assertThat(report.getAgentId()).isEqualTo("extractor-1");
        verify(analysisReportRepository).save(report);
    }
    
    @Test
    void allAgents_IncludeSpecializationInReportMetadata() throws APIException {
        // Arrange
        DocumentChunk chunk = createChunk(1, 1, "Content");
        ModelResponse response = createModelResponse("Analysis");
        ModelResponse synthesis = createModelResponse(
            "KEY FINDINGS:\nTest\nSTRENGTHS:\nTest\nWEAKNESSES:\nTest\nRECOMMENDATIONS:\nTest");
        
        when(modelClient.sendRequest(any(ModelRequest.class)))
            .thenReturn(response)
            .thenReturn(synthesis);
        
        // Act
        AnalysisReport leadReport = leadAnalyst.analyze(List.of(chunk));
        AnalysisReport generalReport = generalAnalyst.analyze(List.of(chunk));
        AnalysisReport methodologyReport = methodologyReviewer.analyze(List.of(chunk));
        AnalysisReport literatureReport = literatureReviewer.analyze(List.of(chunk));
        AnalysisReport screenerReport = quickScreener.analyze(List.of(chunk));
        AnalysisReport extractorReport = factExtractor.analyze(List.of(chunk));
        
        // Assert - Requirement 4.1.7: Each agent includes specialization in metadata
        assertThat(leadReport.getAgentType()).isEqualTo(AgentType.LEAD_ANALYST);
        assertThat(generalReport.getAgentType()).isEqualTo(AgentType.GENERAL_ANALYST);
        assertThat(methodologyReport.getAgentType()).isEqualTo(AgentType.METHODOLOGY_REVIEWER);
        assertThat(literatureReport.getAgentType()).isEqualTo(AgentType.LITERATURE_REVIEWER);
        assertThat(screenerReport.getAgentType()).isEqualTo(AgentType.QUICK_SCREENER);
        assertThat(extractorReport.getAgentType()).isEqualTo(AgentType.FACT_EXTRACTOR);
    }
    
    @Test
    void allAgents_HandleMultipleChunksSequentially() throws APIException {
        // Arrange
        List<DocumentChunk> chunks = List.of(
            createChunk(1, 3, "First chunk"),
            createChunk(2, 3, "Second chunk"),
            createChunk(3, 3, "Third chunk")
        );
        
        ModelResponse chunkResponse = createModelResponse("Chunk analysis");
        ModelResponse synthesisResponse = createModelResponse(
            "KEY FINDINGS:\nMulti-chunk\nSTRENGTHS:\nComplete\n" +
            "WEAKNESSES:\nNone\nRECOMMENDATIONS:\nGood");
        
        when(modelClient.sendRequest(any(ModelRequest.class)))
            .thenReturn(chunkResponse)
            .thenReturn(chunkResponse)
            .thenReturn(chunkResponse)
            .thenReturn(synthesisResponse);
        
        // Act
        AnalysisReport report = leadAnalyst.analyze(chunks);
        
        // Assert
        assertThat(report.getChunksAnalyzed()).isEqualTo(3);
        assertThat(report.getChunksFailed()).isEqualTo(0);
        verify(modelClient, times(4)).sendRequest(any(ModelRequest.class)); // 3 chunks + 1 synthesis
    }
    
    // Helper methods
    
    private ModelResponse createModelResponse(String content) {
        return ModelResponse.builder()
            .id("response-id")
            .object("chat.completion")
            .created(System.currentTimeMillis())
            .model("meta/llama-3_3-70b-instruct")
            .choices(List.of(
                ModelResponse.Choice.builder()
                    .index(0)
                    .message(ModelResponse.Message.builder()
                        .role("assistant")
                        .content(content)
                        .build())
                    .finishReason("stop")
                    .build()
            ))
            .usage(ModelResponse.Usage.builder()
                .promptTokens(100)
                .completionTokens(50)
                .totalTokens(150)
                .build())
            .build();
    }
    
    private DocumentChunk createChunk(int sequence, int total, String text) {
        return new DocumentChunk(
            "chunk-" + sequence,
            "doc-123",
            sequence,
            total,
            text,
            text.length() / 4,
            0,
            text.length(),
            0
        );
    }
}
