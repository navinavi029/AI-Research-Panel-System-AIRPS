package com.aipanelist.processing;

import com.aipanelist.model.AgentProgress;
import com.aipanelist.model.Document;
import com.aipanelist.model.ProcessingStatus;
import com.aipanelist.repository.AgentProgressRepository;
import com.aipanelist.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of DocumentStatusService for managing document processing status.
 * Tracks overall document status, per-agent progress, and calculates estimated completion time.
 */
@Service
public class DocumentStatusServiceImpl implements DocumentStatusService {
    
    private final DocumentRepository documentRepository;
    private final AgentProgressRepository agentProgressRepository;
    
    public DocumentStatusServiceImpl(DocumentRepository documentRepository,
                                    AgentProgressRepository agentProgressRepository) {
        this.documentRepository = documentRepository;
        this.agentProgressRepository = agentProgressRepository;
    }
    
    @Override
    @Transactional
    public void updateStatus(String documentId, ProcessingStatus status) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        
        document.setStatus(status);
        documentRepository.save(document);
    }
    
    @Override
    @Transactional
    public void updateChunkProgress(String documentId, String agentId, int chunksCompleted) {
        AgentProgress progress = agentProgressRepository.findByDocumentIdAndAgentId(documentId, agentId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Agent progress not found for document: " + documentId + ", agent: " + agentId));
        
        progress.setChunksCompleted(chunksCompleted);
        progress.setLastUpdated(LocalDateTime.now());
        agentProgressRepository.save(progress);
    }
    
    @Override
    @Transactional(readOnly = true)
    public DocumentStatus getStatus(String documentId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        
        List<AgentProgress> allProgress = agentProgressRepository.findAll().stream()
            .filter(p -> p.getDocumentId().equals(documentId))
            .collect(Collectors.toList());
        
        List<DocumentStatus.AgentProgressInfo> agentProgressInfos = allProgress.stream()
            .map(p -> new DocumentStatus.AgentProgressInfo(
                p.getAgentId(),
                p.getAgentType().name(),
                p.getChunksCompleted(),
                p.getTotalChunks()
            ))
            .collect(Collectors.toList());
        
        List<String> errorMessages = new ArrayList<>();
        if (document.getErrorMessage() != null && !document.getErrorMessage().isEmpty()) {
            errorMessages.add(document.getErrorMessage());
        }
        
        Optional<Duration> estimatedTimeRemaining = calculateEstimatedTimeRemaining(allProgress);
        
        return new DocumentStatus(
            documentId,
            document.getStatus(),
            agentProgressInfos,
            errorMessages,
            estimatedTimeRemaining
        );
    }
    
    /**
     * Calculates estimated time remaining based on average chunk processing time.
     * Uses the progress of all agents to estimate when processing will complete.
     *
     * @param allProgress list of agent progress records
     * @return optional duration representing estimated time remaining
     */
    private Optional<Duration> calculateEstimatedTimeRemaining(List<AgentProgress> allProgress) {
        if (allProgress.isEmpty()) {
            return Optional.empty();
        }
        
        int totalChunksCompleted = 0;
        int totalChunksRemaining = 0;
        LocalDateTime earliestStartTime = null;
        
        for (AgentProgress progress : allProgress) {
            totalChunksCompleted += progress.getChunksCompleted();
            totalChunksRemaining += (progress.getTotalChunks() - progress.getChunksCompleted());
            
            if (earliestStartTime == null || progress.getLastUpdated().isBefore(earliestStartTime)) {
                earliestStartTime = progress.getLastUpdated();
            }
        }
        
        if (totalChunksCompleted == 0 || earliestStartTime == null) {
            return Optional.empty();
        }
        
        LocalDateTime now = LocalDateTime.now();
        Duration elapsedTime = Duration.between(earliestStartTime, now);
        
        double averageTimePerChunk = (double) elapsedTime.toSeconds() / totalChunksCompleted;
        
        long estimatedSecondsRemaining = (long) (averageTimePerChunk * totalChunksRemaining);
        
        return Optional.of(Duration.ofSeconds(estimatedSecondsRemaining));
    }
}
