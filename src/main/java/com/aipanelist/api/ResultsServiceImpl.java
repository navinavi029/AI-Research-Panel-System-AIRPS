package com.aipanelist.api;

import com.aipanelist.model.AnalysisReport;
import com.aipanelist.model.ConsensusReport;
import com.aipanelist.model.ProcessingStatus;
import com.aipanelist.processing.DocumentStatus;
import com.aipanelist.processing.DocumentStatusService;
import com.aipanelist.repository.AnalysisReportRepository;
import com.aipanelist.repository.ConsensusReportRepository;
import com.aipanelist.repository.DocumentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of ResultsService for retrieving and formatting analysis results.
 * Handles querying of consensus reports, agent reports, and status checking.
 */
@Service
public class ResultsServiceImpl implements ResultsService {
    
    private final ConsensusReportRepository consensusReportRepository;
    private final AnalysisReportRepository analysisReportRepository;
    private final DocumentRepository documentRepository;
    private final DocumentStatusService documentStatusService;
    private final ObjectMapper objectMapper;
    
    public ResultsServiceImpl(ConsensusReportRepository consensusReportRepository,
                             AnalysisReportRepository analysisReportRepository,
                             DocumentRepository documentRepository,
                             DocumentStatusService documentStatusService) {
        this.consensusReportRepository = consensusReportRepository;
        this.analysisReportRepository = analysisReportRepository;
        this.documentRepository = documentRepository;
        this.documentStatusService = documentStatusService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    @Override
    @Transactional(readOnly = true)
    public ConsensusReport getConsensusReport(String documentId) {
        // Check if document exists
        if (!documentRepository.existsById(documentId)) {
            throw new DocumentNotFoundException(documentId);
        }
        
        // Check document status
        DocumentStatus status = documentStatusService.getStatus(documentId);
        if (status.getStatus() != ProcessingStatus.COMPLETE) {
            throw new DocumentStillProcessingException(documentId, status);
        }
        
        // Retrieve consensus report
        return consensusReportRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
    }
    
    @Override
    @Transactional(readOnly = true)
    public DetailedResultsDTO getDetailedResults(String documentId) {
        // Check if document exists
        if (!documentRepository.existsById(documentId)) {
            throw new DocumentNotFoundException(documentId);
        }
        
        // Check document status
        DocumentStatus status = documentStatusService.getStatus(documentId);
        if (status.getStatus() != ProcessingStatus.COMPLETE) {
            throw new DocumentStillProcessingException(documentId, status);
        }
        
        // Retrieve all agent reports
        List<AnalysisReport> agentReports = analysisReportRepository.findByDocumentId(documentId);
        
        // Retrieve consensus report
        ConsensusReport consensusReport = consensusReportRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        
        return new DetailedResultsDTO(documentId, agentReports, consensusReport);
    }
    
    @Override
    public DocumentStatus getDocumentStatus(String documentId) {
        // Check if document exists
        if (!documentRepository.existsById(documentId)) {
            throw new DocumentNotFoundException(documentId);
        }
        
        return documentStatusService.getStatus(documentId);
    }
    
    @Override
    public String formatAsJSON(ConsensusReport report) {
        try {
            return objectMapper.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize ConsensusReport to JSON", e);
        }
    }
    
    @Override
    public String formatAsJSON(DetailedResultsDTO results) {
        try {
            return objectMapper.writeValueAsString(results);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize DetailedResultsDTO to JSON", e);
        }
    }
}
