package com.aipanelist.consensus;

import com.aipanelist.config.NVIDIAConfiguration;
import com.aipanelist.integration.NVIDIAModelClient;
import com.aipanelist.model.*;
import com.aipanelist.repository.ConsensusReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConsensusEngineImpl.
 * 
 * Tests consensus generation from multiple agent reports including
 * prompt building, model interaction, response parsing, and error handling.
 */
class ConsensusEngineTest {

    private NVIDIAModelClient mockModelClient;
    private ConsensusReportRepository mockRepository;
    private NVIDIAConfiguration mockConfig;
    private ConsensusEngineImpl consensusEngine;

    @BeforeEach
    void setUp() {
        mockModelClient = mock(NVIDIAModelClient.class);
        mockRepository = mock(ConsensusReportRepository.class);
        mockConfig = mock(NVIDIAConfiguration.class);
        when(mockConfig.getModelName()).thenReturn("meta/llama-3.3-70b-instruct");
        consensusEngine = new ConsensusEngineImpl(mockModelClient, mockRepository, mockConfig);
    }

    @Test
    void testGenerateConsensus_Success() throws Exception {
        // Arrange
        List<AnalysisReport> reports = createSampleReports();
        String modelOutput = createStructuredModelOutput();
        
        ModelResponse mockResponse = ModelResponse.builder()
            .choices(List.of(
                ModelResponse.Choice.builder()
                    .message(ModelResponse.Message.builder()
                        .content(modelOutput)
                        .build())
                    .build()
            ))
            .build();
        
        when(mockModelClient.sendRequest(any(ModelRequest.class))).thenReturn(mockResponse);
        when(mockRepository.save(any(ConsensusReport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        ConsensusReport result = consensusEngine.generateConsensus(reports);
        
        // Assert
        assertNotNull(result);
        assertEquals("doc-123", result.getDocumentId());
        assertEquals(3, result.getAgentReportsIncluded());
        assertTrue(result.getCommonThemes().contains("methodology"));
        assertTrue(result.getAgreements().contains("strong"));
        assertTrue(result.getDisagreements().contains("sample size"));
        assertTrue(result.getUnifiedRecommendations().contains("Expand"));
        assertTrue(result.getAttributedInsights().contains("LITERATURE_REVIEWER"));
        
        verify(mockRepository, times(1)).save(any(ConsensusReport.class));
    }

    @Test
    void testGenerateConsensus_BuildsCorrectPrompt() throws Exception {
        // Arrange
        List<AnalysisReport> reports = createSampleReports();
        String modelOutput = createStructuredModelOutput();
        
        ModelResponse mockResponse = ModelResponse.builder()
            .choices(List.of(
                ModelResponse.Choice.builder()
                    .message(ModelResponse.Message.builder()
                        .content(modelOutput)
                        .build())
                    .build()
            ))
            .build();
        
        when(mockModelClient.sendRequest(any(ModelRequest.class))).thenReturn(mockResponse);
        when(mockRepository.save(any(ConsensusReport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        ArgumentCaptor<ModelRequest> requestCaptor = ArgumentCaptor.forClass(ModelRequest.class);
        
        // Act
        consensusEngine.generateConsensus(reports);
        
        // Assert
        verify(mockModelClient).sendRequest(requestCaptor.capture());
        ModelRequest capturedRequest = requestCaptor.getValue();
        
        assertEquals("nvidia/llama-3.1-nemotron-70b-instruct", capturedRequest.getModel());
        assertEquals(2, capturedRequest.getMessages().size());
        
        String userMessage = capturedRequest.getMessages().get(1).getContent();
        assertTrue(userMessage.contains("LITERATURE_REVIEWER"));
        assertTrue(userMessage.contains("METHODOLOGY_REVIEWER"));
        assertTrue(userMessage.contains("GENERAL_ANALYST"));
        assertTrue(userMessage.contains("Key Findings:"));
        assertTrue(userMessage.contains("Strengths:"));
        assertTrue(userMessage.contains("Weaknesses:"));
        assertTrue(userMessage.contains("Recommendations:"));
    }

    @Test
    void testGenerateConsensus_EmptyReportList() {
        // Arrange
        List<AnalysisReport> emptyReports = new ArrayList<>();
        
        // Act & Assert
        ConsensusGenerationException exception = assertThrows(
            ConsensusGenerationException.class,
            () -> consensusEngine.generateConsensus(emptyReports)
        );
        
        assertTrue(exception.getMessage().contains("empty report list"));
    }

    @Test
    void testGenerateConsensus_NullReportList() {
        // Act & Assert
        ConsensusGenerationException exception = assertThrows(
            ConsensusGenerationException.class,
            () -> consensusEngine.generateConsensus(null)
        );
        
        assertTrue(exception.getMessage().contains("empty report list"));
    }

    @Test
    void testGenerateConsensus_ModelAPIFailure() throws Exception {
        // Arrange
        List<AnalysisReport> reports = createSampleReports();
        
        when(mockModelClient.sendRequest(any(ModelRequest.class)))
            .thenThrow(new APIException("API connection failed", 500, "SERVER_ERROR"));
        
        // Act & Assert
        ConsensusGenerationException exception = assertThrows(
            ConsensusGenerationException.class,
            () -> consensusEngine.generateConsensus(reports)
        );
        
        assertTrue(exception.getMessage().contains("Failed to call NVIDIA model"));
        assertTrue(exception.getCause() instanceof APIException);
    }

    @Test
    void testGenerateConsensus_UnstructuredModelOutput() throws Exception {
        // Arrange
        List<AnalysisReport> reports = createSampleReports();
        String unstructuredOutput = "This is an unstructured response without proper sections.";
        
        ModelResponse mockResponse = ModelResponse.builder()
            .choices(List.of(
                ModelResponse.Choice.builder()
                    .message(ModelResponse.Message.builder()
                        .content(unstructuredOutput)
                        .build())
                    .build()
            ))
            .build();
        
        when(mockModelClient.sendRequest(any(ModelRequest.class))).thenReturn(mockResponse);
        when(mockRepository.save(any(ConsensusReport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        ConsensusReport result = consensusEngine.generateConsensus(reports);
        
        // Assert
        assertNotNull(result);
        // When sections can't be parsed, the implementation uses default values
        assertEquals("Common themes identified across agent reports", result.getCommonThemes());
        // The attributed insights should be built from the reports as fallback
        String insights = result.getAttributedInsights();
        assertNotNull(insights);
        assertFalse(insights.isEmpty());
    }

    @Test
    void testGenerateConsensus_AttributesInsightsToAgentTypes() throws Exception {
        // Arrange
        List<AnalysisReport> reports = createSampleReports();
        String modelOutput = createStructuredModelOutput();
        
        ModelResponse mockResponse = ModelResponse.builder()
            .choices(List.of(
                ModelResponse.Choice.builder()
                    .message(ModelResponse.Message.builder()
                        .content(modelOutput)
                        .build())
                    .build()
            ))
            .build();
        
        when(mockModelClient.sendRequest(any(ModelRequest.class))).thenReturn(mockResponse);
        when(mockRepository.save(any(ConsensusReport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        ConsensusReport result = consensusEngine.generateConsensus(reports);
        
        // Assert
        String insights = result.getAttributedInsights();
        assertTrue(insights.contains("LITERATURE_REVIEWER"));
        assertTrue(insights.contains("METHODOLOGY_REVIEWER"));
        assertTrue(insights.contains("GENERAL_ANALYST"));
    }

    @Test
    void testGenerateConsensus_StoresReportWithCorrectDocumentId() throws Exception {
        // Arrange
        List<AnalysisReport> reports = createSampleReports();
        String modelOutput = createStructuredModelOutput();
        
        ModelResponse mockResponse = ModelResponse.builder()
            .choices(List.of(
                ModelResponse.Choice.builder()
                    .message(ModelResponse.Message.builder()
                        .content(modelOutput)
                        .build())
                    .build()
            ))
            .build();
        
        when(mockModelClient.sendRequest(any(ModelRequest.class))).thenReturn(mockResponse);
        when(mockRepository.save(any(ConsensusReport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        ArgumentCaptor<ConsensusReport> reportCaptor = ArgumentCaptor.forClass(ConsensusReport.class);
        
        // Act
        consensusEngine.generateConsensus(reports);
        
        // Assert
        verify(mockRepository).save(reportCaptor.capture());
        ConsensusReport savedReport = reportCaptor.getValue();
        
        assertEquals("doc-123", savedReport.getDocumentId());
        assertEquals(3, savedReport.getAgentReportsIncluded());
        assertNotNull(savedReport.getReportId());
        assertTrue(savedReport.getReportId().startsWith("consensus-"));
        assertNotNull(savedReport.getGeneratedAt());
    }

    @Test
    void testGenerateConsensus_CompletesWithinTimeLimit() throws Exception {
        // Arrange
        List<AnalysisReport> reports = createSampleReports();
        String modelOutput = createStructuredModelOutput();
        
        ModelResponse mockResponse = ModelResponse.builder()
            .choices(List.of(
                ModelResponse.Choice.builder()
                    .message(ModelResponse.Message.builder()
                        .content(modelOutput)
                        .build())
                    .build()
            ))
            .build();
        
        when(mockModelClient.sendRequest(any(ModelRequest.class))).thenReturn(mockResponse);
        when(mockRepository.save(any(ConsensusReport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        long startTime = System.currentTimeMillis();
        consensusEngine.generateConsensus(reports);
        long duration = System.currentTimeMillis() - startTime;
        
        // Assert - should complete well within 2 minutes (120000ms)
        assertTrue(duration < 120000, "Consensus generation took too long: " + duration + "ms");
    }

    // Helper methods

    private List<AnalysisReport> createSampleReports() {
        List<AnalysisReport> reports = new ArrayList<>();
        
        reports.add(new AnalysisReport(
            "report-1",
            "doc-123",
            "agent-1",
            AgentType.LITERATURE_REVIEWER,
            "The paper cites 45 relevant studies from the past 5 years.",
            "Strong literature review with recent citations.",
            "Missing some seminal works from earlier periods.",
            "Include foundational studies from 2010-2015.",
            LocalDateTime.now(),
            10,
            0
        ));
        
        reports.add(new AnalysisReport(
            "report-2",
            "doc-123",
            "agent-2",
            AgentType.METHODOLOGY_REVIEWER,
            "The methodology uses a randomized controlled trial design.",
            "Rigorous experimental design with proper controls.",
            "Sample size is relatively small (n=50).",
            "Expand sample size to at least 100 participants.",
            LocalDateTime.now(),
            10,
            0
        ));
        
        reports.add(new AnalysisReport(
            "report-3",
            "doc-123",
            "agent-3",
            AgentType.GENERAL_ANALYST,
            "The paper presents novel findings in the field.",
            "Clear presentation and well-structured arguments.",
            "Statistical analysis could be more comprehensive.",
            "Add multivariate analysis and effect size calculations.",
            LocalDateTime.now(),
            10,
            0
        ));
        
        return reports;
    }

    private String createStructuredModelOutput() {
        return """
            ## COMMON THEMES
            - Strong research methodology and experimental design
            - Need for expanded sample sizes and more comprehensive analysis
            - Well-structured presentation with clear arguments
            
            ## AGREEMENTS
            - All agents agree the paper has strong foundational elements
            - Consensus on the need for more comprehensive statistical analysis
            - Agreement that the literature review is current and relevant
            
            ## DISAGREEMENTS
            - LITERATURE_REVIEWER emphasizes missing historical context
            - METHODOLOGY_REVIEWER focuses on sample size concerns
            - GENERAL_ANALYST prioritizes statistical depth over breadth
            
            ## UNIFIED RECOMMENDATIONS
            - Expand sample size to at least 100 participants for statistical power
            - Include foundational studies from 2010-2015 to provide historical context
            - Add multivariate analysis and effect size calculations
            - Maintain the strong experimental design and clear presentation style
            
            ## ATTRIBUTED INSIGHTS
            - LITERATURE_REVIEWER: Identified 45 recent citations and noted missing seminal works
            - METHODOLOGY_REVIEWER: Highlighted rigorous RCT design but flagged small sample size (n=50)
            - GENERAL_ANALYST: Recognized novel findings and clear structure, suggested enhanced statistical analysis
            """;
    }
}
