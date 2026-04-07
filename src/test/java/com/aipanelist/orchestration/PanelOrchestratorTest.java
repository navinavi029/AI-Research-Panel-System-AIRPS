package com.aipanelist.orchestration;

import com.aipanelist.agents.AIAgent;
import com.aipanelist.config.NVIDIAConfiguration;
import com.aipanelist.integration.NVIDIAModelClient;
import com.aipanelist.model.*;
import com.aipanelist.repository.AgentProgressRepository;
import com.aipanelist.repository.AnalysisReportRepository;
import com.aipanelist.repository.ChunkAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PanelOrchestratorImpl.
 * 
 * Tests panel creation, agent orchestration, failure handling, and progress tracking.
 */
@ExtendWith(MockitoExtension.class)
class PanelOrchestratorTest {
    
    @Mock
    private NVIDIAModelClient modelClient;
    
    @Mock
    private ChunkAnalysisRepository chunkAnalysisRepository;
    
    @Mock
    private AnalysisReportRepository analysisReportRepository;
    
    @Mock
    private AgentProgressRepository agentProgressRepository;
    
    @Mock
    private NVIDIAConfiguration nvidiaConfig;
    
    private PanelOrchestratorImpl orchestrator;
    
    @BeforeEach
    void setUp() {
        when(nvidiaConfig.getModelName()).thenReturn("meta/llama-3_3-70b-instruct");
        orchestrator = new PanelOrchestratorImpl(
            modelClient,
            chunkAnalysisRepository,
            analysisReportRepository,
            agentProgressRepository,
            nvidiaConfig
        );
    }
    
    @Test
    void createPanel_CreatesExactlySixAgents() {
        // Arrange
        String documentId = "test-doc-123";
        
        // Act
        AnalysisPanel panel = orchestrator.createPanel(documentId);
        
        // Assert
        assertThat(panel).isNotNull();
        assertThat(panel.getDocumentId()).isEqualTo(documentId);
        assertThat(panel.getAgents()).hasSize(6);
    }
    
    @Test
    void createPanel_CreatesOneAgentOfEachType() {
        // Arrange
        String documentId = "test-doc-123";
        
        // Act
        AnalysisPanel panel = orchestrator.createPanel(documentId);
        
        // Assert
        List<AIAgent> agents = panel.getAgents();
        List<AgentType> types = agents.stream()
            .map(AIAgent::getType)
            .toList();
        
        assertThat(types).containsExactlyInAnyOrder(
            AgentType.LEAD_ANALYST,
            AgentType.GENERAL_ANALYST,
            AgentType.METHODOLOGY_REVIEWER,
            AgentType.LITERATURE_REVIEWER,
            AgentType.QUICK_SCREENER,
            AgentType.FACT_EXTRACTOR
        );
    }
    
    @Test
    void createPanel_AssignsUniqueAgentIds() {
        // Arrange
        String documentId = "test-doc-123";
        
        // Act
        AnalysisPanel panel = orchestrator.createPanel(documentId);
        
        // Assert
        List<String> agentIds = panel.getAgents().stream()
            .map(AIAgent::getAgentId)
            .toList();
        
        // All IDs should be unique
        assertThat(agentIds).doesNotHaveDuplicates();
        
        // All IDs should be valid UUIDs
        agentIds.forEach(id -> {
            assertThat(id).isNotNull();
            assertThat(id).isNotEmpty();
            // Verify it's a valid UUID format
            UUID.fromString(id);
        });
    }
    
    @Test
    void createPanel_ConfiguresAgentsWithModelClient() {
        // Arrange
        String documentId = "test-doc-123";
        
        // Act
        AnalysisPanel panel = orchestrator.createPanel(documentId);
        
        // Assert
        // All agents should be properly constructed (no exceptions thrown)
        assertThat(panel.getAgents()).allMatch(agent -> 
            agent.getAgentId() != null && agent.getType() != null
        );
    }
    
    @Test
    void orchestrateAnalysis_InitializesProgressForAllAgents() throws APIException {
        // Arrange
        String documentId = "test-doc-123";
        AnalysisPanel panel = orchestrator.createPanel(documentId);
        
        DocumentChunk chunk = createTestChunk(documentId, 1, 1);
        List<DocumentChunk> chunks = List.of(chunk);
        
        // Mock model client to return responses
        when(modelClient.sendRequest(any())).thenReturn(
            createTestModelResponse("test analysis")
        );
        
        when(agentProgressRepository.findByDocumentIdAndAgentId(any(), any()))
            .thenReturn(Optional.empty());
        
        // Act
        orchestrator.orchestrateAnalysis(panel, chunks);
        
        // Assert
        verify(agentProgressRepository, times(6)).save(any(AgentProgress.class));
    }
    
    @Test
    void orchestrateAnalysis_ReturnsReportsFromSuccessfulAgents() throws APIException {
        // Arrange
        String documentId = "test-doc-123";
        AnalysisPanel panel = orchestrator.createPanel(documentId);
        
        DocumentChunk chunk = createTestChunk(documentId, 1, 1);
        List<DocumentChunk> chunks = List.of(chunk);
        
        // Mock model client to return responses
        when(modelClient.sendRequest(any())).thenReturn(
            createTestModelResponse("test analysis")
        );
        
        when(agentProgressRepository.findByDocumentIdAndAgentId(any(), any()))
            .thenReturn(Optional.empty());
        
        // Act
        List<AnalysisReport> reports = orchestrator.orchestrateAnalysis(panel, chunks);
        
        // Assert
        assertThat(reports).isNotEmpty();
        assertThat(reports).hasSizeLessThanOrEqualTo(6);
    }
    
    @Test
    void orchestrateAnalysis_ContinuesWithRemainingAgentsOnFailure() throws APIException {
        // Arrange
        String documentId = "test-doc-123";
        AnalysisPanel panel = orchestrator.createPanel(documentId);
        
        DocumentChunk chunk = createTestChunk(documentId, 1, 1);
        List<DocumentChunk> chunks = List.of(chunk);
        
        // Mock model client to always fail - this will cause all agents to fail
        // after exhausting their retries
        when(modelClient.sendRequest(any()))
            .thenThrow(new APIException("API Error"));
        
        // Act
        List<AnalysisReport> reports = orchestrator.orchestrateAnalysis(panel, chunks);
        
        // Assert
        // When all agents fail, we should get an empty list (no reports)
        // The orchestrator should handle this gracefully without throwing exceptions
        assertThat(reports).isEmpty();
    }
    
    @Test
    void orchestrateAnalysis_UpdatesProgressAfterCompletion() throws APIException {
        // Arrange
        String documentId = "test-doc-123";
        AnalysisPanel panel = orchestrator.createPanel(documentId);
        
        DocumentChunk chunk = createTestChunk(documentId, 1, 1);
        List<DocumentChunk> chunks = List.of(chunk);
        
        AgentProgress mockProgress = new AgentProgress(
            UUID.randomUUID().toString(),
            documentId,
            panel.getAgents().get(0).getAgentId(),
            AgentType.LEAD_ANALYST,
            0,
            1,
            LocalDateTime.now()
        );
        
        when(modelClient.sendRequest(any())).thenReturn(
            createTestModelResponse("test analysis")
        );
        
        when(agentProgressRepository.findByDocumentIdAndAgentId(any(), any()))
            .thenReturn(Optional.of(mockProgress));
        
        // Act
        orchestrator.orchestrateAnalysis(panel, chunks);
        
        // Assert
        // Progress should be updated for completed agents
        verify(agentProgressRepository, atLeastOnce()).save(argThat(progress ->
            progress.getChunksCompleted() > 0
        ));
    }
    
    /**
     * Helper method to create a test document chunk.
     */
    private DocumentChunk createTestChunk(String documentId, int sequence, int total) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setChunkId(UUID.randomUUID().toString());
        chunk.setDocumentId(documentId);
        chunk.setSequenceNumber(sequence);
        chunk.setTotalChunks(total);
        chunk.setChunkText("This is test chunk content for analysis.");
        chunk.setTokenCount(10);
        chunk.setStartByteOffset(0);
        chunk.setEndByteOffset(100);
        chunk.setOverlapTokens(0);
        return chunk;
    }
    
    /**
     * Helper method to create a test model response.
     */
    private ModelResponse createTestModelResponse(String content) {
        ModelResponse.Message message = ModelResponse.Message.builder()
            .role("assistant")
            .content(content)
            .build();
        
        ModelResponse.Choice choice = ModelResponse.Choice.builder()
            .index(0)
            .message(message)
            .finishReason("stop")
            .build();
        
        return ModelResponse.builder()
            .id("test-response-id")
            .object("chat.completion")
            .created(System.currentTimeMillis())
            .model("meta/llama-3_3-70b-instruct")
            .choices(List.of(choice))
            .build();
    }
}
