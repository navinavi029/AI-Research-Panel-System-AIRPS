package com.aipanelist.processing;

import com.aipanelist.model.AgentProgress;
import com.aipanelist.model.AgentType;
import com.aipanelist.model.Document;
import com.aipanelist.model.ProcessingStatus;
import com.aipanelist.repository.AgentProgressRepository;
import com.aipanelist.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentStatusServiceTest {
    
    @Mock
    private DocumentRepository documentRepository;
    
    @Mock
    private AgentProgressRepository agentProgressRepository;
    
    private DocumentStatusService service;
    
    @BeforeEach
    void setUp() {
        service = new DocumentStatusServiceImpl(documentRepository, agentProgressRepository);
    }
    
    @Test
    void updateStatus_shouldUpdateDocumentStatus() {
        String documentId = "doc-123";
        Document document = new Document(documentId, "test.pdf", 1000L, 
            LocalDateTime.now(), ProcessingStatus.UPLOADED, null, 0, 0);
        
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        
        service.updateStatus(documentId, ProcessingStatus.ANALYZING);
        
        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(captor.capture());
        
        Document savedDocument = captor.getValue();
        assertEquals(ProcessingStatus.ANALYZING, savedDocument.getStatus());
    }
    
    @Test
    void updateStatus_shouldThrowExceptionWhenDocumentNotFound() {
        String documentId = "nonexistent";
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, 
            () -> service.updateStatus(documentId, ProcessingStatus.ANALYZING));
    }
    
    @Test
    void updateChunkProgress_shouldUpdateAgentProgress() {
        String documentId = "doc-123";
        String agentId = "agent-456";
        AgentProgress progress = new AgentProgress("prog-1", documentId, agentId, 
            AgentType.FACT_EXTRACTOR, 5, 10, LocalDateTime.now());
        
        when(agentProgressRepository.findByDocumentIdAndAgentId(documentId, agentId))
            .thenReturn(Optional.of(progress));
        when(agentProgressRepository.save(any(AgentProgress.class))).thenReturn(progress);
        
        service.updateChunkProgress(documentId, agentId, 7);
        
        ArgumentCaptor<AgentProgress> captor = ArgumentCaptor.forClass(AgentProgress.class);
        verify(agentProgressRepository).save(captor.capture());
        
        AgentProgress savedProgress = captor.getValue();
        assertEquals(7, savedProgress.getChunksCompleted());
        assertNotNull(savedProgress.getLastUpdated());
    }
    
    @Test
    void updateChunkProgress_shouldThrowExceptionWhenProgressNotFound() {
        String documentId = "doc-123";
        String agentId = "agent-456";
        
        when(agentProgressRepository.findByDocumentIdAndAgentId(documentId, agentId))
            .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, 
            () -> service.updateChunkProgress(documentId, agentId, 7));
    }
    
    @Test
    void getStatus_shouldReturnCompleteDocumentStatus() {
        String documentId = "doc-123";
        Document document = new Document(documentId, "test.pdf", 1000L, 
            LocalDateTime.now(), ProcessingStatus.ANALYZING, null, 0, 0);
        
        AgentProgress progress1 = new AgentProgress("prog-1", documentId, "agent-1", 
            AgentType.FACT_EXTRACTOR, 5, 10, LocalDateTime.now().minusMinutes(5));
        AgentProgress progress2 = new AgentProgress("prog-2", documentId, "agent-2", 
            AgentType.LITERATURE_REVIEWER, 3, 10, LocalDateTime.now().minusMinutes(3));
        
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(agentProgressRepository.findAll()).thenReturn(Arrays.asList(progress1, progress2));
        
        DocumentStatus status = service.getStatus(documentId);
        
        assertNotNull(status);
        assertEquals(documentId, status.getDocumentId());
        assertEquals(ProcessingStatus.ANALYZING, status.getStatus());
        assertEquals(2, status.getAgentProgress().size());
        assertTrue(status.getErrorMessages().isEmpty());
    }
    
    @Test
    void getStatus_shouldIncludeErrorMessages() {
        String documentId = "doc-123";
        String errorMessage = "Processing failed due to timeout";
        Document document = new Document(documentId, "test.pdf", 1000L, 
            LocalDateTime.now(), ProcessingStatus.FAILED, errorMessage, 0, 0);
        
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(agentProgressRepository.findAll()).thenReturn(Arrays.asList());
        
        DocumentStatus status = service.getStatus(documentId);
        
        assertNotNull(status);
        assertEquals(1, status.getErrorMessages().size());
        assertEquals(errorMessage, status.getErrorMessages().get(0));
    }
    
    @Test
    void getStatus_shouldCalculateEstimatedTimeRemaining() {
        String documentId = "doc-123";
        Document document = new Document(documentId, "test.pdf", 1000L, 
            LocalDateTime.now(), ProcessingStatus.ANALYZING, null, 0, 0);
        
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(10);
        AgentProgress progress1 = new AgentProgress("prog-1", documentId, "agent-1", 
            AgentType.FACT_EXTRACTOR, 5, 10, startTime);
        AgentProgress progress2 = new AgentProgress("prog-2", documentId, "agent-2", 
            AgentType.LITERATURE_REVIEWER, 3, 10, startTime);
        
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(agentProgressRepository.findAll()).thenReturn(Arrays.asList(progress1, progress2));
        
        DocumentStatus status = service.getStatus(documentId);
        
        assertNotNull(status);
        assertTrue(status.getEstimatedTimeRemaining().isPresent());
        
        Duration estimated = status.getEstimatedTimeRemaining().get();
        assertTrue(estimated.toSeconds() > 0);
    }
    
    @Test
    void getStatus_shouldReturnEmptyEstimateWhenNoProgressExists() {
        String documentId = "doc-123";
        Document document = new Document(documentId, "test.pdf", 1000L, 
            LocalDateTime.now(), ProcessingStatus.UPLOADED, null, 0, 0);
        
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(agentProgressRepository.findAll()).thenReturn(Arrays.asList());
        
        DocumentStatus status = service.getStatus(documentId);
        
        assertNotNull(status);
        assertFalse(status.getEstimatedTimeRemaining().isPresent());
    }
    
    @Test
    void getStatus_shouldReturnEmptyEstimateWhenNoChunksCompleted() {
        String documentId = "doc-123";
        Document document = new Document(documentId, "test.pdf", 1000L, 
            LocalDateTime.now(), ProcessingStatus.ANALYZING, null, 0, 0);
        
        AgentProgress progress = new AgentProgress("prog-1", documentId, "agent-1", 
            AgentType.FACT_EXTRACTOR, 0, 10, LocalDateTime.now());
        
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(agentProgressRepository.findAll()).thenReturn(Arrays.asList(progress));
        
        DocumentStatus status = service.getStatus(documentId);
        
        assertNotNull(status);
        assertFalse(status.getEstimatedTimeRemaining().isPresent());
    }
    
    @Test
    void getStatus_shouldThrowExceptionWhenDocumentNotFound() {
        String documentId = "nonexistent";
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, 
            () -> service.getStatus(documentId));
    }
    
    @Test
    void agentProgressInfo_shouldCalculateProgressPercentage() {
        DocumentStatus.AgentProgressInfo info = new DocumentStatus.AgentProgressInfo(
            "agent-1", "FACT_EXTRACTOR", 5, 10);
        
        assertEquals(50.0, info.getProgressPercentage(), 0.01);
    }
    
    @Test
    void agentProgressInfo_shouldHandleZeroTotalChunks() {
        DocumentStatus.AgentProgressInfo info = new DocumentStatus.AgentProgressInfo(
            "agent-1", "FACT_EXTRACTOR", 0, 0);
        
        assertEquals(0.0, info.getProgressPercentage(), 0.01);
    }
}
