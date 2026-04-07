package com.aipanelist.agents;

import com.aipanelist.config.NVIDIAConfiguration;
import com.aipanelist.integration.NVIDIAModelClient;
import com.aipanelist.model.*;
import com.aipanelist.repository.ChunkAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AIAgent abstract base class.
 * Tests common functionality including chunk analysis, retry logic, and synthesis.
 */
@ExtendWith(MockitoExtension.class)
class AIAgentTest {
    
    @Mock
    private NVIDIAModelClient modelClient;
    
    @Mock
    private ChunkAnalysisRepository chunkAnalysisRepository;
    
    @Mock
    private NVIDIAConfiguration nvidiaConfig;
    
    private TestAgent agent;
    
    @BeforeEach
    void setUp() {
        when(nvidiaConfig.getModelName()).thenReturn("meta/llama-3_3-70b-instruct");
        agent = new TestAgent("test-agent-1", AgentType.GENERAL_ANALYST, 
            modelClient, chunkAnalysisRepository, nvidiaConfig);
    }
    
    @Test
    void analyzeChunk_WithNoContext_SendsRequestWithSystemPrompt() throws APIException {
        // Arrange
        DocumentChunk chunk = createChunk(1, 1, "Test content");
        ModelResponse response = createModelResponse("Analysis result");
        when(modelClient.sendRequest(any(ModelRequest.class))).thenReturn(response);
        
        // Act
        agent.analyzeChunk(chunk, List.of());
        
        // Assert
        ArgumentCaptor<ModelRequest> requestCaptor = ArgumentCaptor.forClass(ModelRequest.class);
        verify(modelClient).sendRequest(requestCaptor.capture());
        
        ModelRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getModel()).isEqualTo("meta/llama-3_3-70b-instruct");
        assertThat(capturedRequest.getMessages()).hasSize(2);
        assertThat(capturedRequest.getMessages().get(0).getRole()).isEqualTo("system");
        assertThat(capturedRequest.getMessages().get(0).getContent()).isEqualTo("Test system prompt");
        assertThat(capturedRequest.getMessages().get(1).getRole()).isEqualTo("user");
        assertThat(capturedRequest.getMessages().get(1).getContent()).contains("Test content");
    }
    
    @Test
    void analyzeChunk_WithPreviousAnalyses_IncludesContextSummary() throws APIException {
        // Arrange
        DocumentChunk chunk = createChunk(2, 3, "Second chunk content");
        ChunkAnalysis previousAnalysis = createChunkAnalysis(1, "Previous findings");
        ModelResponse response = createModelResponse("Analysis result");
        when(modelClient.sendRequest(any(ModelRequest.class))).thenReturn(response);
        
        // Act
        agent.analyzeChunk(chunk, List.of(previousAnalysis));
        
        // Assert
        ArgumentCaptor<ModelRequest> requestCaptor = ArgumentCaptor.forClass(ModelRequest.class);
        verify(modelClient).sendRequest(requestCaptor.capture());
        
        String userMessage = requestCaptor.getValue().getMessages().get(1).getContent();
        assertThat(userMessage).contains("Previous findings from earlier chunks");
        assertThat(userMessage).contains("Chunk 1");
        assertThat(userMessage).contains("Previous findings");
    }
    
    @Test
    void analyzeChunk_OnAPIFailure_RetriesWithExponentialBackoff() throws APIException {
        // Arrange
        DocumentChunk chunk = createChunk(1, 1, "Test content");
        when(modelClient.sendRequest(any(ModelRequest.class)))
            .thenThrow(new APIException("Temporary failure"))
            .thenThrow(new APIException("Temporary failure"))
            .thenReturn(createModelResponse("Success on third attempt"));
        
        // Act
        ChunkAnalysis result = agent.analyzeChunk(chunk, List.of());
        
        // Assert
        verify(modelClient, times(3)).sendRequest(any(ModelRequest.class));
        assertThat(result.getFindings()).isEqualTo("Success on third attempt");
    }
    
    @Test
    void analyzeChunk_AfterMaxRetries_ThrowsChunkAnalysisException() throws APIException {
        // Arrange
        DocumentChunk chunk = createChunk(1, 1, "Test content");
        when(modelClient.sendRequest(any(ModelRequest.class)))
            .thenThrow(new APIException("Persistent failure"));
        
        // Act & Assert
        assertThatThrownBy(() -> agent.analyzeChunk(chunk, List.of()))
            .isInstanceOf(AIAgent.ChunkAnalysisException.class)
            .hasMessageContaining("Failed to analyze chunk 1 after 3 attempts");
        
        verify(modelClient, times(3)).sendRequest(any(ModelRequest.class));
    }
    
    @Test
    void analyzeChunk_ReturnsChunkAnalysisWithCorrectMetadata() throws APIException {
        // Arrange
        DocumentChunk chunk = createChunk(2, 5, "Test content");
        ModelResponse response = createModelResponse("Analysis findings");
        when(modelClient.sendRequest(any(ModelRequest.class))).thenReturn(response);
        
        // Act
        ChunkAnalysis result = agent.analyzeChunk(chunk, List.of());
        
        // Assert
        assertThat(result.getChunkId()).isEqualTo(chunk.getChunkId());
        assertThat(result.getChunkSequence()).isEqualTo(2);
        assertThat(result.getFindings()).isEqualTo("Analysis findings");
        assertThat(result.getAnalyzedAt()).isNotNull();
    }
    
    @Test
    void synthesizeChunkAnalyses_CombinesMultipleAnalyses() throws APIException {
        // Arrange
        List<ChunkAnalysis> analyses = List.of(
            createChunkAnalysis(1, "Findings from chunk 1"),
            createChunkAnalysis(2, "Findings from chunk 2"),
            createChunkAnalysis(3, "Findings from chunk 3")
        );
        
        String synthesisResponse = """
            KEY FINDINGS:
            Combined findings
            
            STRENGTHS:
            Document strengths
            
            WEAKNESSES:
            Document weaknesses
            
            RECOMMENDATIONS:
            Recommendations
            """;
        
        when(modelClient.sendRequest(any(ModelRequest.class)))
            .thenReturn(createModelResponse(synthesisResponse));
        
        // Act
        AnalysisReport report = agent.synthesizeChunkAnalyses(analyses, "doc-123", 3, 0);
        
        // Assert
        assertThat(report.getDocumentId()).isEqualTo("doc-123");
        assertThat(report.getAgentId()).isEqualTo("test-agent-1");
        assertThat(report.getAgentType()).isEqualTo(AgentType.GENERAL_ANALYST);
        assertThat(report.getKeyFindings()).contains("Combined findings");
        assertThat(report.getStrengths()).contains("Document strengths");
        assertThat(report.getWeaknesses()).contains("Document weaknesses");
        assertThat(report.getRecommendations()).contains("Recommendations");
        assertThat(report.getChunksAnalyzed()).isEqualTo(3);
        assertThat(report.getChunksFailed()).isEqualTo(0);
    }
    
    @Test
    void synthesizeChunkAnalyses_WithFailedChunks_IncludesFailureCount() throws APIException {
        // Arrange
        List<ChunkAnalysis> analyses = List.of(
            createChunkAnalysis(1, "Findings from chunk 1"),
            createChunkAnalysis(3, "Findings from chunk 3")
        );
        
        String synthesisResponse = """
            KEY FINDINGS:
            Partial findings
            
            STRENGTHS:
            Some strengths
            
            WEAKNESSES:
            Some weaknesses
            
            RECOMMENDATIONS:
            Limited recommendations
            """;
        
        when(modelClient.sendRequest(any(ModelRequest.class)))
            .thenReturn(createModelResponse(synthesisResponse));
        
        // Act
        AnalysisReport report = agent.synthesizeChunkAnalyses(analyses, "doc-123", 2, 1);
        
        // Assert
        assertThat(report.getChunksAnalyzed()).isEqualTo(2);
        assertThat(report.getChunksFailed()).isEqualTo(1);
        
        ArgumentCaptor<ModelRequest> requestCaptor = ArgumentCaptor.forClass(ModelRequest.class);
        verify(modelClient).sendRequest(requestCaptor.capture());
        
        String prompt = requestCaptor.getValue().getMessages().get(1).getContent();
        assertThat(prompt).contains("1 chunk(s) failed analysis");
    }
    
    @Test
    void synthesizeChunkAnalyses_PersistsChunkAnalysesWithReportId() throws APIException {
        // Arrange
        List<ChunkAnalysis> analyses = new ArrayList<>(List.of(
            createChunkAnalysis(1, "Findings 1"),
            createChunkAnalysis(2, "Findings 2")
        ));
        
        when(modelClient.sendRequest(any(ModelRequest.class)))
            .thenReturn(createModelResponse("KEY FINDINGS:\nTest\nSTRENGTHS:\nTest\nWEAKNESSES:\nTest\nRECOMMENDATIONS:\nTest"));
        
        // Act
        AnalysisReport report = agent.synthesizeChunkAnalyses(analyses, "doc-123", 2, 0);
        
        // Assert
        verify(chunkAnalysisRepository, times(2)).save(any(ChunkAnalysis.class));
        
        ArgumentCaptor<ChunkAnalysis> analysisCaptor = ArgumentCaptor.forClass(ChunkAnalysis.class);
        verify(chunkAnalysisRepository, times(2)).save(analysisCaptor.capture());
        
        List<ChunkAnalysis> savedAnalyses = analysisCaptor.getAllValues();
        assertThat(savedAnalyses).allMatch(a -> a.getReportId() != null);
        assertThat(savedAnalyses).allMatch(a -> a.getReportId().equals(report.getReportId()));
    }
    
    @Test
    void synthesizeChunkAnalyses_OnAPIFailure_ThrowsSynthesisException() throws APIException {
        // Arrange
        List<ChunkAnalysis> analyses = List.of(createChunkAnalysis(1, "Findings"));
        when(modelClient.sendRequest(any(ModelRequest.class)))
            .thenThrow(new APIException("Synthesis failed"));
        
        // Act & Assert
        assertThatThrownBy(() -> agent.synthesizeChunkAnalyses(analyses, "doc-123", 1, 0))
            .isInstanceOf(AIAgent.SynthesisException.class)
            .hasMessageContaining("Failed to synthesize chunk analyses");
    }
    
    @Test
    void getAgentId_ReturnsCorrectId() {
        assertThat(agent.getAgentId()).isEqualTo("test-agent-1");
    }
    
    @Test
    void getType_ReturnsCorrectType() {
        assertThat(agent.getType()).isEqualTo(AgentType.GENERAL_ANALYST);
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
            text.length() / 4, // Approximate token count
            0,
            text.length(),
            0
        );
    }
    
    private ChunkAnalysis createChunkAnalysis(int sequence, String findings) {
        return new ChunkAnalysis(
            "analysis-" + sequence,
            null,
            "chunk-" + sequence,
            sequence,
            findings,
            "Context summary",
            java.time.LocalDateTime.now()
        );
    }
    
    /**
     * Concrete test implementation of AIAgent for testing.
     */
    private static class TestAgent extends AIAgent {
        
        public TestAgent(String agentId, AgentType type, NVIDIAModelClient modelClient,
                        ChunkAnalysisRepository chunkAnalysisRepository, NVIDIAConfiguration nvidiaConfig) {
            super(agentId, type, modelClient, chunkAnalysisRepository, nvidiaConfig);
        }
        
        @Override
        public AnalysisReport analyze(List<DocumentChunk> chunks) {
            // Simple implementation for testing
            List<ChunkAnalysis> analyses = new ArrayList<>();
            int failed = 0;
            
            for (DocumentChunk chunk : chunks) {
                try {
                    ChunkAnalysis analysis = analyzeChunk(chunk, analyses);
                    analyses.add(analysis);
                } catch (ChunkAnalysisException e) {
                    failed++;
                }
            }
            
            return synthesizeChunkAnalyses(analyses, chunks.get(0).getDocumentId(), 
                analyses.size(), failed);
        }
        
        @Override
        protected String getSystemPrompt() {
            return "Test system prompt";
        }
    }
}
