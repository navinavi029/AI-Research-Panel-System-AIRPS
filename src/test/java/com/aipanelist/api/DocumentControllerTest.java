package com.aipanelist.api;

import com.aipanelist.async.AsyncAnalysisService;
import com.aipanelist.export.PDFExportService;
import com.aipanelist.model.ProcessingStatus;
import com.aipanelist.processing.DocumentStatus;
import com.aipanelist.processing.DocumentStatusService;
import com.aipanelist.upload.DocumentUploadResponse;
import com.aipanelist.upload.InvalidFileException;
import com.aipanelist.upload.PDFUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DocumentController REST API endpoints.
 * 
 * Tests upload, status, results, and detailed results endpoints with various scenarios.
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 9.5
 */
class DocumentControllerTest {
    
    private PDFUploadService uploadService;
    private DocumentStatusService statusService;
    private ResultsService resultsService;
    private DocumentController controller;
    
    @BeforeEach
    void setUp() {
        uploadService = mock(PDFUploadService.class);
        statusService = mock(DocumentStatusService.class);
        resultsService = mock(ResultsService.class);
        AsyncAnalysisService asyncAnalysisService = mock(AsyncAnalysisService.class);
        PDFExportService pdfExportService = mock(PDFExportService.class);
        controller = new DocumentController(uploadService, statusService, resultsService, asyncAnalysisService, pdfExportService);
    }
    
    // Upload endpoint tests
    
    @Test
    void uploadDocument_withValidPDF_returnsOkWithDocumentId() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "PDF content".getBytes()
        );
        
        DocumentUploadResponse expectedResponse = new DocumentUploadResponse(
            "doc-123",
            "test.pdf",
            "UPLOADED"
        );
        
        when(uploadService.uploadDocument(any())).thenReturn(expectedResponse);
        
        // Act
        ResponseEntity<?> response = controller.uploadDocument(file);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(DocumentUploadResponse.class, response.getBody());
        DocumentUploadResponse actualResponse = (DocumentUploadResponse) response.getBody();
        assertEquals("doc-123", actualResponse.getDocumentId());
        assertEquals("test.pdf", actualResponse.getFilename());
        verify(uploadService).uploadDocument(file);
    }
    
    @Test
    void uploadDocument_withNullFile_returnsBadRequest() {
        // Act
        ResponseEntity<?> response = controller.uploadDocument(null);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertInstanceOf(ErrorResponse.class, response.getBody());
        ErrorResponse error = (ErrorResponse) response.getBody();
        assertEquals("INVALID_FILE", error.getCode());
        assertTrue(error.getMessage().contains("required"));
        verifyNoInteractions(uploadService);
    }
    
    @Test
    void uploadDocument_withEmptyFile_returnsBadRequest() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            new byte[0]
        );
        
        // Act
        ResponseEntity<?> response = controller.uploadDocument(file);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertInstanceOf(ErrorResponse.class, response.getBody());
        ErrorResponse error = (ErrorResponse) response.getBody();
        assertEquals("INVALID_FILE", error.getCode());
        verifyNoInteractions(uploadService);
    }
    
    @Test
    void uploadDocument_withFileTooLarge_returnsBadRequest() {
        // Arrange - Create file larger than 50MB
        byte[] largeContent = new byte[52_428_801]; // 50MB + 1 byte
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "large.pdf",
            "application/pdf",
            largeContent
        );
        
        // Act
        ResponseEntity<?> response = controller.uploadDocument(file);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertInstanceOf(ErrorResponse.class, response.getBody());
        ErrorResponse error = (ErrorResponse) response.getBody();
        assertEquals("FILE_TOO_LARGE", error.getCode());
        assertTrue(error.getMessage().contains("50MB"));
        verifyNoInteractions(uploadService);
    }
    
    @Test
    void uploadDocument_withNonPDFMimeType_returnsBadRequest() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "Text content".getBytes()
        );
        
        // Act
        ResponseEntity<?> response = controller.uploadDocument(file);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertInstanceOf(ErrorResponse.class, response.getBody());
        ErrorResponse error = (ErrorResponse) response.getBody();
        assertEquals("INVALID_FILE_TYPE", error.getCode());
        assertTrue(error.getMessage().contains("PDF"));
        verifyNoInteractions(uploadService);
    }
    
    @Test
    void uploadDocument_withInvalidPDFStructure_returnsBadRequest() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "corrupted.pdf",
            "application/pdf",
            "Not a real PDF".getBytes()
        );
        
        when(uploadService.uploadDocument(any()))
            .thenThrow(new InvalidFileException("PDF structure is corrupted"));
        
        // Act
        ResponseEntity<?> response = controller.uploadDocument(file);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertInstanceOf(ErrorResponse.class, response.getBody());
        ErrorResponse error = (ErrorResponse) response.getBody();
        assertEquals("INVALID_PDF", error.getCode());
        assertTrue(error.getMessage().contains("corrupted"));
    }
    
    @Test
    void uploadDocument_withServiceException_returnsInternalServerError() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "PDF content".getBytes()
        );
        
        when(uploadService.uploadDocument(any()))
            .thenThrow(new RuntimeException("Database connection failed"));
        
        // Act
        ResponseEntity<?> response = controller.uploadDocument(file);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertInstanceOf(ErrorResponse.class, response.getBody());
        ErrorResponse error = (ErrorResponse) response.getBody();
        assertEquals("UPLOAD_FAILED", error.getCode());
    }
    
    // Status endpoint tests
    
    @Test
    void getDocumentStatus_withExistingDocument_returnsStatus() {
        // Arrange
        String documentId = "doc-123";
        DocumentStatus expectedStatus = new DocumentStatus(
            documentId,
            ProcessingStatus.ANALYZING,
            Collections.emptyList(),
            Collections.emptyList(),
            Optional.of(Duration.ofMinutes(10))
        );
        
        when(statusService.getStatus(documentId)).thenReturn(expectedStatus);
        
        // Act
        ResponseEntity<?> response = controller.getDocumentStatus(documentId);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(DocumentStatus.class, response.getBody());
        DocumentStatus actualStatus = (DocumentStatus) response.getBody();
        assertEquals(documentId, actualStatus.getDocumentId());
        assertEquals(ProcessingStatus.ANALYZING, actualStatus.getStatus());
        verify(statusService).getStatus(documentId);
    }
    
    @Test
    void getDocumentStatus_withNonExistentDocument_returnsNotFound() {
        // Arrange
        String documentId = "non-existent";
        when(statusService.getStatus(documentId))
            .thenThrow(new DocumentNotFoundException("Document not found: " + documentId));
        
        // Act
        ResponseEntity<?> response = controller.getDocumentStatus(documentId);
        
        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertInstanceOf(ErrorResponse.class, response.getBody());
        ErrorResponse error = (ErrorResponse) response.getBody();
        assertEquals("DOCUMENT_NOT_FOUND", error.getCode());
    }
    
    @Test
    void getDocumentStatus_withServiceException_returnsInternalServerError() {
        // Arrange
        String documentId = "doc-123";
        when(statusService.getStatus(documentId))
            .thenThrow(new RuntimeException("Database error"));
        
        // Act
        ResponseEntity<?> response = controller.getDocumentStatus(documentId);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertInstanceOf(ErrorResponse.class, response.getBody());
    }
    
    // Results endpoint tests
    
    @Test
    void getResults_withCompletedDocument_returnsConsensusReport() {
        // Arrange
        String documentId = "doc-123";
        // Create a real ConsensusReport instead of mocking (to avoid Java 26 Mockito issues)
        var mockReport = new com.aipanelist.model.ConsensusReport();
        mockReport.setDocumentId(documentId);
        String expectedJson = "{\"consensus\": \"report data\"}";
        
        when(resultsService.getConsensusReport(documentId)).thenReturn(mockReport);
        when(resultsService.formatAsJSON(mockReport)).thenReturn(expectedJson);
        
        // Act
        ResponseEntity<?> response = controller.getResults(documentId);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedJson, response.getBody());
        verify(resultsService).getConsensusReport(documentId);
        verify(resultsService).formatAsJSON(mockReport);
    }
    
    @Test
    void getResults_withProcessingDocument_returnsStatus() {
        // Arrange
        String documentId = "doc-123";
        DocumentStatus expectedStatus = new DocumentStatus(
            documentId,
            ProcessingStatus.ANALYZING,
            Collections.emptyList(),
            Collections.emptyList(),
            Optional.of(Duration.ofMinutes(5))
        );
        
        when(resultsService.getConsensusReport(documentId))
            .thenThrow(new DocumentStillProcessingException(documentId, expectedStatus));
        when(statusService.getStatus(documentId)).thenReturn(expectedStatus);
        
        // Act
        ResponseEntity<?> response = controller.getResults(documentId);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(DocumentStatus.class, response.getBody());
        DocumentStatus actualStatus = (DocumentStatus) response.getBody();
        assertEquals(ProcessingStatus.ANALYZING, actualStatus.getStatus());
    }
    
    @Test
    void getResults_withNonExistentDocument_returnsNotFound() {
        // Arrange
        String documentId = "non-existent";
        when(resultsService.getConsensusReport(documentId))
            .thenThrow(new DocumentNotFoundException("Document not found"));
        
        // Act
        ResponseEntity<?> response = controller.getResults(documentId);
        
        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertInstanceOf(ErrorResponse.class, response.getBody());
    }
    
    // Detailed results endpoint tests
    
    @Test
    void getDetailedResults_withCompletedDocument_returnsAllReports() {
        // Arrange
        String documentId = "doc-123";
        var mockConsensusReport = new com.aipanelist.model.ConsensusReport();
        mockConsensusReport.setDocumentId(documentId);
        DetailedResultsDTO expectedResults = new DetailedResultsDTO(
            documentId,
            Collections.emptyList(),
            mockConsensusReport
        );
        
        when(resultsService.getDetailedResults(documentId)).thenReturn(expectedResults);
        
        // Act
        ResponseEntity<?> response = controller.getDetailedResults(documentId);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(DetailedResultsDTO.class, response.getBody());
        DetailedResultsDTO actualResults = (DetailedResultsDTO) response.getBody();
        assertEquals(documentId, actualResults.getDocumentId());
        verify(resultsService).getDetailedResults(documentId);
    }
    
    @Test
    void getDetailedResults_withProcessingDocument_returnsStatus() {
        // Arrange
        String documentId = "doc-123";
        DocumentStatus expectedStatus = new DocumentStatus(
            documentId,
            ProcessingStatus.DELIBERATING,
            Collections.emptyList(),
            Collections.emptyList(),
            Optional.of(Duration.ofMinutes(2))
        );
        
        when(resultsService.getDetailedResults(documentId))
            .thenThrow(new DocumentStillProcessingException(documentId, expectedStatus));
        when(statusService.getStatus(documentId)).thenReturn(expectedStatus);
        
        // Act
        ResponseEntity<?> response = controller.getDetailedResults(documentId);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(DocumentStatus.class, response.getBody());
    }
    
    @Test
    void getDetailedResults_withNonExistentDocument_returnsNotFound() {
        // Arrange
        String documentId = "non-existent";
        when(resultsService.getDetailedResults(documentId))
            .thenThrow(new DocumentNotFoundException("Document not found"));
        
        // Act
        ResponseEntity<?> response = controller.getDetailedResults(documentId);
        
        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertInstanceOf(ErrorResponse.class, response.getBody());
    }
}

