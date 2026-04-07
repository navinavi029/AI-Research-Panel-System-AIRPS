package com.aipanelist.async;

import com.aipanelist.agents.AIAgent;
import com.aipanelist.config.NVIDIAConfiguration;
import com.aipanelist.consensus.ConsensusEngine;
import com.aipanelist.model.*;
import com.aipanelist.orchestration.AnalysisPanel;
import com.aipanelist.orchestration.PanelOrchestrator;
import com.aipanelist.processing.ChunkProcessor;
import com.aipanelist.processing.DocumentProcessor;
import com.aipanelist.processing.DocumentStatusService;
import com.aipanelist.processing.ExtractionException;
import com.aipanelist.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AsyncAnalysisServiceImpl.
 * 
 * Tests the complete asynchronous processing pipeline including:
 * - Successful end-to-end processing
 * - Failure at extraction stage
 * - Failure at chunking stage
 * - Failure at analysis stage
 * - Failure at consensus stage
 * - Status updates at each stage
 */
@ExtendWith(MockitoExtension.class)
class AsyncAnalysisServiceTest {
    
    @Mock
    private DocumentProcessor documentProcessor;
    
    @Mock
    private ChunkProcessor chunkProcessor;
    
    @Mock
    private PanelOrchestrator panelOrchestrator;
    
    @Mock
    private ConsensusEngine consensusEngine;
    
    @Mock
    private DocumentStatusService statusService;
    
    @Mock
    private DocumentRepository documentRepository;
    
    @Mock
    private NVIDIAConfiguration nvidiaConfig;
    
    private AsyncAnalysisService asyncAnalysisService;
    
    /**
     * Simple test implementation of AIAgent for creating AnalysisPanel instances.
     */
    private static class TestAgent extends AIAgent {
        public TestAgent(NVIDIAConfiguration nvidiaConfig) {
            super("test-agent", AgentType.LEAD_ANALYST, null, null, nvidiaConfig);
        }
        
        @Override
        public AnalysisReport analyze(List<DocumentChunk> chunks) {
            return null;
        }
        
        @Override
        protected String getSystemPrompt() {
            return "Test prompt";
        }
    }
    
    @BeforeEach
    void setUp() {
        when(nvidiaConfig.getModelName()).thenReturn("meta/llama-3_3-70b-instruct");
        asyncAnalysisService = new AsyncAnalysisServiceImpl(
            documentProcessor,
            chunkProcessor,
            panelOrchestrator,
            consensusEngine,
            statusService,
            documentRepository
        );
    }
    
    @Test
    void testSuccessfulEndToEndProcessing() throws Exception {
        // Arrange
        String documentId = "test-doc-123";
        
        ExtractedDocument extractedDoc = new ExtractedDocument(
            documentId,
            "Test content",
            100,
            LocalDateTime.now(),
            true
        );
        
        DocumentChunk chunk = new DocumentChunk(
            "chunk-1",
            documentId,
            1,
            1,
            "Test content",
            100,
            0,
            100,
            0
        );
        
        AnalysisReport report = new AnalysisReport(
            "report-1",
            documentId,
            "agent-1",
            AgentType.LEAD_ANALYST,
            "Findings",
            "Strengths",
            "Weaknesses",
            "Recommendations",
            LocalDateTime.now(),
            1,
            0
        );
        
        ConsensusReport consensusReport = new ConsensusReport(
            "consensus-1",
            documentId,
            "Themes",
            "Agreements",
            "Disagreements",
            "Recommendations",
            "Insights",
            LocalDateTime.now(),
            1
        );
        
        // Create real AnalysisPanel with 6 test agents
        List<AIAgent> agents = List.of(
            new TestAgent(nvidiaConfig), new TestAgent(nvidiaConfig), new TestAgent(nvidiaConfig),
            new TestAgent(nvidiaConfig), new TestAgent(nvidiaConfig), new TestAgent(nvidiaConfig)
        );
        AnalysisPanel panel = new AnalysisPanel(documentId, agents);
        
        when(documentProcessor.extractText(eq(documentId), any(Path.class)))
            .thenReturn(extractedDoc);
        when(chunkProcessor.chunkDocument(extractedDoc))
            .thenReturn(List.of(chunk));
        when(panelOrchestrator.createPanel(documentId))
            .thenReturn(panel);
        when(panelOrchestrator.orchestrateAnalysis(eq(panel), anyList()))
            .thenReturn(List.of(report));
        when(consensusEngine.generateConsensus(anyList()))
            .thenReturn(consensusReport);
        
        // Act
        asyncAnalysisService.processDocument(documentId);
        
        // Assert
        verify(statusService).updateStatus(documentId, ProcessingStatus.PROCESSING);
        verify(statusService).updateStatus(documentId, ProcessingStatus.ANALYZING);
        verify(statusService).updateStatus(documentId, ProcessingStatus.DELIBERATING);
        verify(statusService).updateStatus(documentId, ProcessingStatus.COMPLETE);
        verify(documentProcessor).extractText(eq(documentId), any(Path.class));
        verify(chunkProcessor).chunkDocument(extractedDoc);
        verify(panelOrchestrator).createPanel(documentId);
        verify(panelOrchestrator).orchestrateAnalysis(eq(panel), anyList());
        verify(consensusEngine).generateConsensus(anyList());
    }
    
    @Test
    void testFailureAtExtractionStage() throws Exception {
        // Arrange
        String documentId = "test-doc-123";
        String errorMessage = "Failed to extract text";
        
        Document document = new Document(
            documentId,
            "test.pdf",
            1000L,
            LocalDateTime.now(),
            ProcessingStatus.UPLOADED,
            null,
            0,
            0
        );
        
        when(documentProcessor.extractText(eq(documentId), any(Path.class)))
            .thenThrow(new ExtractionException(errorMessage));
        when(documentRepository.findById(documentId))
            .thenReturn(Optional.of(document));
        
        // Act
        asyncAnalysisService.processDocument(documentId);
        
        // Assert
        verify(statusService).updateStatus(documentId, ProcessingStatus.PROCESSING);
        verify(statusService).updateStatus(documentId, ProcessingStatus.FAILED);
        verify(documentRepository).findById(documentId);
        verify(documentRepository).save(argThat(doc -> 
            doc.getErrorMessage().equals(errorMessage)
        ));
        verify(chunkProcessor, never()).chunkDocument(any());
    }
    
    @Test
    void testFailureAtChunkingStage() throws Exception {
        // Arrange
        String documentId = "test-doc-123";
        String errorMessage = "Failed to chunk document";
        
        ExtractedDocument extractedDoc = new ExtractedDocument(
            documentId,
            "Test content",
            100,
            LocalDateTime.now(),
            true
        );
        
        Document document = new Document(
            documentId,
            "test.pdf",
            1000L,
            LocalDateTime.now(),
            ProcessingStatus.PROCESSING,
            null,
            0,
            0
        );
        
        when(documentProcessor.extractText(eq(documentId), any(Path.class)))
            .thenReturn(extractedDoc);
        when(chunkProcessor.chunkDocument(extractedDoc))
            .thenThrow(new RuntimeException(errorMessage));
        when(documentRepository.findById(documentId))
            .thenReturn(Optional.of(document));
        
        // Act
        asyncAnalysisService.processDocument(documentId);
        
        // Assert
        verify(statusService).updateStatus(documentId, ProcessingStatus.PROCESSING);
        verify(statusService).updateStatus(documentId, ProcessingStatus.ANALYZING);
        verify(statusService).updateStatus(documentId, ProcessingStatus.FAILED);
        verify(documentRepository).save(argThat(doc -> 
            doc.getErrorMessage().equals(errorMessage)
        ));
        verify(panelOrchestrator, never()).createPanel(any());
    }
    
    @Test
    void testFailureAtAnalysisStage() throws Exception {
        // Arrange
        String documentId = "test-doc-123";
        String errorMessage = "All agents failed";
        
        ExtractedDocument extractedDoc = new ExtractedDocument(
            documentId,
            "Test content",
            100,
            LocalDateTime.now(),
            true
        );
        
        DocumentChunk chunk = new DocumentChunk(
            "chunk-1",
            documentId,
            1,
            1,
            "Test content",
            100,
            0,
            100,
            0
        );
        
        Document document = new Document(
            documentId,
            "test.pdf",
            1000L,
            LocalDateTime.now(),
            ProcessingStatus.ANALYZING,
            null,
            0,
            0
        );
        
        // Create real AnalysisPanel with 6 test agents
        List<AIAgent> agents = List.of(
            new TestAgent(nvidiaConfig), new TestAgent(nvidiaConfig), new TestAgent(nvidiaConfig),
            new TestAgent(nvidiaConfig), new TestAgent(nvidiaConfig), new TestAgent(nvidiaConfig)
        );
        AnalysisPanel panel = new AnalysisPanel(documentId, agents);
        
        when(documentProcessor.extractText(eq(documentId), any(Path.class)))
            .thenReturn(extractedDoc);
        when(chunkProcessor.chunkDocument(extractedDoc))
            .thenReturn(List.of(chunk));
        when(panelOrchestrator.createPanel(documentId))
            .thenReturn(panel);
        when(panelOrchestrator.orchestrateAnalysis(eq(panel), anyList()))
            .thenReturn(List.of()); // No reports returned
        when(documentRepository.findById(documentId))
            .thenReturn(Optional.of(document));
        
        // Act
        asyncAnalysisService.processDocument(documentId);
        
        // Assert
        verify(statusService).updateStatus(documentId, ProcessingStatus.PROCESSING);
        verify(statusService).updateStatus(documentId, ProcessingStatus.ANALYZING);
        verify(statusService).updateStatus(documentId, ProcessingStatus.FAILED);
        verify(documentRepository).save(argThat(doc -> 
            doc.getErrorMessage().contains("All agents failed")
        ));
        verify(consensusEngine, never()).generateConsensus(any());
    }
    
    @Test
    void testFailureAtConsensusStage() throws Exception {
        // Arrange
        String documentId = "test-doc-123";
        String errorMessage = "Failed to generate consensus";
        
        ExtractedDocument extractedDoc = new ExtractedDocument(
            documentId,
            "Test content",
            100,
            LocalDateTime.now(),
            true
        );
        
        DocumentChunk chunk = new DocumentChunk(
            "chunk-1",
            documentId,
            1,
            1,
            "Test content",
            100,
            0,
            100,
            0
        );
        
        AnalysisReport report = new AnalysisReport(
            "report-1",
            documentId,
            "agent-1",
            AgentType.LEAD_ANALYST,
            "Findings",
            "Strengths",
            "Weaknesses",
            "Recommendations",
            LocalDateTime.now(),
            1,
            0
        );
        
        Document document = new Document(
            documentId,
            "test.pdf",
            1000L,
            LocalDateTime.now(),
            ProcessingStatus.DELIBERATING,
            null,
            0,
            0
        );
        
        // Create real AnalysisPanel with 6 test agents
        List<AIAgent> agents = List.of(
            new TestAgent(nvidiaConfig), new TestAgent(nvidiaConfig), new TestAgent(nvidiaConfig),
            new TestAgent(nvidiaConfig), new TestAgent(nvidiaConfig), new TestAgent(nvidiaConfig)
        );
        AnalysisPanel panel = new AnalysisPanel(documentId, agents);
        
        when(documentProcessor.extractText(eq(documentId), any(Path.class)))
            .thenReturn(extractedDoc);
        when(chunkProcessor.chunkDocument(extractedDoc))
            .thenReturn(List.of(chunk));
        when(panelOrchestrator.createPanel(documentId))
            .thenReturn(panel);
        when(panelOrchestrator.orchestrateAnalysis(eq(panel), anyList()))
            .thenReturn(List.of(report));
        when(consensusEngine.generateConsensus(anyList()))
            .thenThrow(new RuntimeException(errorMessage));
        when(documentRepository.findById(documentId))
            .thenReturn(Optional.of(document));
        
        // Act
        asyncAnalysisService.processDocument(documentId);
        
        // Assert
        verify(statusService).updateStatus(documentId, ProcessingStatus.PROCESSING);
        verify(statusService).updateStatus(documentId, ProcessingStatus.ANALYZING);
        verify(statusService).updateStatus(documentId, ProcessingStatus.DELIBERATING);
        verify(statusService).updateStatus(documentId, ProcessingStatus.FAILED);
        verify(documentRepository).save(argThat(doc -> 
            doc.getErrorMessage().equals(errorMessage)
        ));
    }
}
