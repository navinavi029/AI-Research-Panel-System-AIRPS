package com.aipanelist.api;

import com.aipanelist.model.AgentType;
import com.aipanelist.model.AnalysisReport;
import com.aipanelist.model.ConsensusReport;
import com.aipanelist.model.ProcessingStatus;
import com.aipanelist.processing.DocumentStatus;
import com.aipanelist.processing.DocumentStatusService;
import com.aipanelist.repository.AnalysisReportRepository;
import com.aipanelist.repository.ConsensusReportRepository;
import com.aipanelist.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ResultsServiceImpl.
 * Tests retrieval of consensus reports, detailed results, status checking, and JSON formatting.
 */
@ExtendWith(MockitoExtension.class)
class ResultsServiceTest {
    
    @Mock
    private ConsensusReportRepository consensusReportRepository;
    
    @Mock
    private AnalysisReportRepository analysisReportRepository;
    
    @Mock
    private DocumentRepository documentRepository;
    
    @Mock
    private DocumentStatusService documentStatusService;
    
    private ResultsServiceImpl resultsService;
    
    private static final String TEST_DOCUMENT_ID = "doc-123";
    private static final String TEST_REPORT_ID = "report-456";
    
    @BeforeEach
    void setUp() {
        resultsService = new ResultsServiceImpl(
                consensusReportRepository,
                analysisReportRepository,
                documentRepository,
                documentStatusService
        );
    }
    
    @Test
    void getConsensusReport_WhenDocumentExists_ReturnsReport() {
        // Arrange
        ConsensusReport expectedReport = createTestConsensusReport();
        DocumentStatus status = createCompletedStatus();
        
        when(documentRepository.existsById(TEST_DOCUMENT_ID)).thenReturn(true);
        when(documentStatusService.getStatus(TEST_DOCUMENT_ID)).thenReturn(status);
        when(consensusReportRepository.findByDocumentId(TEST_DOCUMENT_ID))
                .thenReturn(Optional.of(expectedReport));
        
        // Act
        ConsensusReport result = resultsService.getConsensusReport(TEST_DOCUMENT_ID);
        
        // Assert
        assertNotNull(result);
        assertEquals(TEST_REPORT_ID, result.getReportId());
        assertEquals(TEST_DOCUMENT_ID, result.getDocumentId());
        verify(documentRepository).existsById(TEST_DOCUMENT_ID);
        verify(documentStatusService).getStatus(TEST_DOCUMENT_ID);
        verify(consensusReportRepository).findByDocumentId(TEST_DOCUMENT_ID);
    }
    
    @Test
    void getConsensusReport_WhenDocumentNotFound_ThrowsException() {
        // Arrange
        when(documentRepository.existsById(TEST_DOCUMENT_ID)).thenReturn(false);
        
        // Act & Assert
        assertThrows(DocumentNotFoundException.class, 
                () -> resultsService.getConsensusReport(TEST_DOCUMENT_ID));
        verify(documentRepository).existsById(TEST_DOCUMENT_ID);
        verifyNoInteractions(documentStatusService);
        verifyNoInteractions(consensusReportRepository);
    }
    
    @Test
    void getConsensusReport_WhenDocumentStillProcessing_ThrowsException() {
        // Arrange
        DocumentStatus processingStatus = createProcessingStatus();
        
        when(documentRepository.existsById(TEST_DOCUMENT_ID)).thenReturn(true);
        when(documentStatusService.getStatus(TEST_DOCUMENT_ID)).thenReturn(processingStatus);
        
        // Act & Assert
        DocumentStillProcessingException exception = assertThrows(
                DocumentStillProcessingException.class,
                () -> resultsService.getConsensusReport(TEST_DOCUMENT_ID)
        );
        
        assertEquals(processingStatus, exception.getCurrentStatus());
        verify(documentRepository).existsById(TEST_DOCUMENT_ID);
        verify(documentStatusService).getStatus(TEST_DOCUMENT_ID);
        verifyNoInteractions(consensusReportRepository);
    }
    
    @Test
    void getConsensusReport_WhenReportNotFound_ThrowsException() {
        // Arrange
        DocumentStatus status = createCompletedStatus();
        
        when(documentRepository.existsById(TEST_DOCUMENT_ID)).thenReturn(true);
        when(documentStatusService.getStatus(TEST_DOCUMENT_ID)).thenReturn(status);
        when(consensusReportRepository.findByDocumentId(TEST_DOCUMENT_ID))
                .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(DocumentNotFoundException.class,
                () -> resultsService.getConsensusReport(TEST_DOCUMENT_ID));
        verify(consensusReportRepository).findByDocumentId(TEST_DOCUMENT_ID);
    }
    
    @Test
    void getDetailedResults_WhenDocumentExists_ReturnsAllReports() {
        // Arrange
        ConsensusReport consensusReport = createTestConsensusReport();
        List<AnalysisReport> agentReports = createTestAgentReports();
        DocumentStatus status = createCompletedStatus();
        
        when(documentRepository.existsById(TEST_DOCUMENT_ID)).thenReturn(true);
        when(documentStatusService.getStatus(TEST_DOCUMENT_ID)).thenReturn(status);
        when(analysisReportRepository.findByDocumentId(TEST_DOCUMENT_ID))
                .thenReturn(agentReports);
        when(consensusReportRepository.findByDocumentId(TEST_DOCUMENT_ID))
                .thenReturn(Optional.of(consensusReport));
        
        // Act
        DetailedResultsDTO result = resultsService.getDetailedResults(TEST_DOCUMENT_ID);
        
        // Assert
        assertNotNull(result);
        assertEquals(TEST_DOCUMENT_ID, result.getDocumentId());
        assertEquals(3, result.getAgentReports().size());
        assertNotNull(result.getConsensusReport());
        assertEquals(TEST_REPORT_ID, result.getConsensusReport().getReportId());
        
        verify(documentRepository).existsById(TEST_DOCUMENT_ID);
        verify(documentStatusService).getStatus(TEST_DOCUMENT_ID);
        verify(analysisReportRepository).findByDocumentId(TEST_DOCUMENT_ID);
        verify(consensusReportRepository).findByDocumentId(TEST_DOCUMENT_ID);
    }
    
    @Test
    void getDetailedResults_WhenDocumentNotFound_ThrowsException() {
        // Arrange
        when(documentRepository.existsById(TEST_DOCUMENT_ID)).thenReturn(false);
        
        // Act & Assert
        assertThrows(DocumentNotFoundException.class,
                () -> resultsService.getDetailedResults(TEST_DOCUMENT_ID));
        verify(documentRepository).existsById(TEST_DOCUMENT_ID);
        verifyNoInteractions(documentStatusService);
        verifyNoInteractions(analysisReportRepository);
        verifyNoInteractions(consensusReportRepository);
    }
    
    @Test
    void getDetailedResults_WhenDocumentStillAnalyzing_ThrowsException() {
        // Arrange
        DocumentStatus analyzingStatus = createAnalyzingStatus();
        
        when(documentRepository.existsById(TEST_DOCUMENT_ID)).thenReturn(true);
        when(documentStatusService.getStatus(TEST_DOCUMENT_ID)).thenReturn(analyzingStatus);
        
        // Act & Assert
        DocumentStillProcessingException exception = assertThrows(
                DocumentStillProcessingException.class,
                () -> resultsService.getDetailedResults(TEST_DOCUMENT_ID)
        );
        
        assertEquals(ProcessingStatus.ANALYZING, exception.getCurrentStatus().getStatus());
        verify(documentRepository).existsById(TEST_DOCUMENT_ID);
        verify(documentStatusService).getStatus(TEST_DOCUMENT_ID);
        verifyNoInteractions(analysisReportRepository);
        verifyNoInteractions(consensusReportRepository);
    }
    
    @Test
    void getDetailedResults_WhenNoAgentReports_ReturnsEmptyList() {
        // Arrange
        ConsensusReport consensusReport = createTestConsensusReport();
        DocumentStatus status = createCompletedStatus();
        
        when(documentRepository.existsById(TEST_DOCUMENT_ID)).thenReturn(true);
        when(documentStatusService.getStatus(TEST_DOCUMENT_ID)).thenReturn(status);
        when(analysisReportRepository.findByDocumentId(TEST_DOCUMENT_ID))
                .thenReturn(Collections.emptyList());
        when(consensusReportRepository.findByDocumentId(TEST_DOCUMENT_ID))
                .thenReturn(Optional.of(consensusReport));
        
        // Act
        DetailedResultsDTO result = resultsService.getDetailedResults(TEST_DOCUMENT_ID);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.getAgentReports().isEmpty());
        assertNotNull(result.getConsensusReport());
    }
    
    @Test
    void getDocumentStatus_WhenDocumentExists_ReturnsStatus() {
        // Arrange
        DocumentStatus expectedStatus = createProcessingStatus();
        
        when(documentRepository.existsById(TEST_DOCUMENT_ID)).thenReturn(true);
        when(documentStatusService.getStatus(TEST_DOCUMENT_ID)).thenReturn(expectedStatus);
        
        // Act
        DocumentStatus result = resultsService.getDocumentStatus(TEST_DOCUMENT_ID);
        
        // Assert
        assertNotNull(result);
        assertEquals(TEST_DOCUMENT_ID, result.getDocumentId());
        assertEquals(ProcessingStatus.PROCESSING, result.getStatus());
        verify(documentRepository).existsById(TEST_DOCUMENT_ID);
        verify(documentStatusService).getStatus(TEST_DOCUMENT_ID);
    }
    
    @Test
    void getDocumentStatus_WhenDocumentNotFound_ThrowsException() {
        // Arrange
        when(documentRepository.existsById(TEST_DOCUMENT_ID)).thenReturn(false);
        
        // Act & Assert
        assertThrows(DocumentNotFoundException.class,
                () -> resultsService.getDocumentStatus(TEST_DOCUMENT_ID));
        verify(documentRepository).existsById(TEST_DOCUMENT_ID);
        verifyNoInteractions(documentStatusService);
    }
    
    @Test
    void formatAsJSON_ConsensusReport_ReturnsValidJSON() {
        // Arrange
        ConsensusReport report = createTestConsensusReport();
        
        // Act
        String json = resultsService.formatAsJSON(report);
        
        // Assert
        assertNotNull(json);
        assertTrue(json.contains("\"reportId\":\"" + TEST_REPORT_ID + "\""));
        assertTrue(json.contains("\"documentId\":\"" + TEST_DOCUMENT_ID + "\""));
        assertTrue(json.contains("\"commonThemes\""));
    }
    
    @Test
    void formatAsJSON_DetailedResults_ReturnsValidJSON() {
        // Arrange
        ConsensusReport consensusReport = createTestConsensusReport();
        List<AnalysisReport> agentReports = createTestAgentReports();
        DetailedResultsDTO results = new DetailedResultsDTO(TEST_DOCUMENT_ID, agentReports, consensusReport);
        
        // Act
        String json = resultsService.formatAsJSON(results);
        
        // Assert
        assertNotNull(json);
        assertTrue(json.contains("\"documentId\":\"" + TEST_DOCUMENT_ID + "\""));
        assertTrue(json.contains("\"agentReports\""));
        assertTrue(json.contains("\"consensusReport\""));
    }
    
    @Test
    void getConsensusReport_WhenDocumentDeliberating_ThrowsException() {
        // Arrange
        DocumentStatus deliberatingStatus = createDeliberatingStatus();
        
        when(documentRepository.existsById(TEST_DOCUMENT_ID)).thenReturn(true);
        when(documentStatusService.getStatus(TEST_DOCUMENT_ID)).thenReturn(deliberatingStatus);
        
        // Act & Assert
        DocumentStillProcessingException exception = assertThrows(
                DocumentStillProcessingException.class,
                () -> resultsService.getConsensusReport(TEST_DOCUMENT_ID)
        );
        
        assertEquals(ProcessingStatus.DELIBERATING, exception.getCurrentStatus().getStatus());
    }
    
    @Test
    void getConsensusReport_WhenDocumentFailed_ReturnsReport() {
        // Arrange
        DocumentStatus failedStatus = createFailedStatus();
        
        when(documentRepository.existsById(TEST_DOCUMENT_ID)).thenReturn(true);
        when(documentStatusService.getStatus(TEST_DOCUMENT_ID)).thenReturn(failedStatus);
        
        // Act & Assert
        // Failed status should throw exception since it's not COMPLETE
        assertThrows(DocumentStillProcessingException.class,
                () -> resultsService.getConsensusReport(TEST_DOCUMENT_ID));
    }
    
    // Helper methods to create test data
    
    private ConsensusReport createTestConsensusReport() {
        return new ConsensusReport(
                TEST_REPORT_ID,
                TEST_DOCUMENT_ID,
                "Common themes identified across all agents",
                "All agents agree on methodology quality",
                "Some disagreement on statistical significance",
                "Recommend further validation studies",
                "Lead Analyst: Strong methodology; Literature Reviewer: Comprehensive citations",
                LocalDateTime.now(),
                5
        );
    }
    
    private List<AnalysisReport> createTestAgentReports() {
        AnalysisReport report1 = new AnalysisReport(
                "report-1",
                TEST_DOCUMENT_ID,
                "agent-1",
                AgentType.LEAD_ANALYST,
                "Strong methodology and clear conclusions",
                "Well-structured research design",
                "Limited sample size",
                "Consider expanding sample",
                LocalDateTime.now(),
                10,
                0
        );
        
        AnalysisReport report2 = new AnalysisReport(
                "report-2",
                TEST_DOCUMENT_ID,
                "agent-2",
                AgentType.LITERATURE_REVIEWER,
                "Comprehensive literature review",
                "Thorough citation coverage",
                "Some outdated references",
                "Update recent literature",
                LocalDateTime.now(),
                10,
                0
        );
        
        AnalysisReport report3 = new AnalysisReport(
                "report-3",
                TEST_DOCUMENT_ID,
                "agent-3",
                AgentType.METHODOLOGY_REVIEWER,
                "Sound statistical approach",
                "Appropriate statistical tests",
                "Could benefit from additional controls",
                "Add control groups",
                LocalDateTime.now(),
                10,
                1
        );
        
        return Arrays.asList(report1, report2, report3);
    }
    
    private DocumentStatus createCompletedStatus() {
        return new DocumentStatus(
                TEST_DOCUMENT_ID,
                ProcessingStatus.COMPLETE,
                Collections.emptyList(),
                Collections.emptyList(),
                Optional.empty()
        );
    }
    
    private DocumentStatus createProcessingStatus() {
        return new DocumentStatus(
                TEST_DOCUMENT_ID,
                ProcessingStatus.PROCESSING,
                Collections.emptyList(),
                Collections.emptyList(),
                Optional.of(Duration.ofMinutes(5))
        );
    }
    
    private DocumentStatus createAnalyzingStatus() {
        List<DocumentStatus.AgentProgressInfo> progress = Arrays.asList(
                new DocumentStatus.AgentProgressInfo("agent-1", "LEAD_ANALYST", 5, 10),
                new DocumentStatus.AgentProgressInfo("agent-2", "LITERATURE_REVIEWER", 3, 10)
        );
        
        return new DocumentStatus(
                TEST_DOCUMENT_ID,
                ProcessingStatus.ANALYZING,
                progress,
                Collections.emptyList(),
                Optional.of(Duration.ofMinutes(10))
        );
    }
    
    private DocumentStatus createDeliberatingStatus() {
        return new DocumentStatus(
                TEST_DOCUMENT_ID,
                ProcessingStatus.DELIBERATING,
                Collections.emptyList(),
                Collections.emptyList(),
                Optional.of(Duration.ofMinutes(2))
        );
    }
    
    private DocumentStatus createFailedStatus() {
        return new DocumentStatus(
                TEST_DOCUMENT_ID,
                ProcessingStatus.FAILED,
                Collections.emptyList(),
                Arrays.asList("Processing error occurred"),
                Optional.empty()
        );
    }
}
