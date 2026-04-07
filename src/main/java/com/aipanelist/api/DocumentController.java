package com.aipanelist.api;

import com.aipanelist.async.AsyncAnalysisService;
import com.aipanelist.export.PDFExportService;
import com.aipanelist.export.PDFGenerationException;
import com.aipanelist.processing.DocumentStatus;
import com.aipanelist.processing.DocumentStatusService;
import com.aipanelist.upload.DocumentUploadResponse;
import com.aipanelist.upload.InvalidFileException;
import com.aipanelist.upload.PDFUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST API controller for document upload, status tracking, and results retrieval.
 * 
 * Provides endpoints for:
 * - Uploading PDF documents for analysis
 * - Checking document processing status
 * - Retrieving consensus reports
 * - Retrieving detailed analysis results
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 9.5
 */
@RestController
@RequestMapping("/api/documents")
@Tag(name = "Document Analysis", description = "APIs for uploading documents and retrieving AI analysis results")
public class DocumentController {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
    
    private final PDFUploadService uploadService;
    private final DocumentStatusService statusService;
    private final ResultsService resultsService;
    private final AsyncAnalysisService asyncAnalysisService;
    private final PDFExportService pdfExportService;
    
    public DocumentController(PDFUploadService uploadService,
                            DocumentStatusService statusService,
                            ResultsService resultsService,
                            AsyncAnalysisService asyncAnalysisService,
                            PDFExportService pdfExportService) {
        this.uploadService = uploadService;
        this.statusService = statusService;
        this.resultsService = resultsService;
        this.asyncAnalysisService = asyncAnalysisService;
        this.pdfExportService = pdfExportService;
    }
    
    /**
     * Upload a PDF document for analysis.
     * 
     * Validates file size (≤50MB), MIME type (application/pdf), and PDF structure.
     * Returns 400 Bad Request with specific error messages for validation failures.
     * Returns 200 OK with DocumentUploadResponse containing documentId on success.
     * 
     * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 9.5
     */
    @Operation(
        summary = "Upload PDF document for analysis",
        description = "Upload a research paper PDF (max 50MB, 500 pages) for multi-agent AI analysis. " +
                     "Returns a document ID for tracking processing status and retrieving results."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Document uploaded successfully",
            content = @Content(schema = @Schema(implementation = DocumentUploadResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid file (wrong type, too large, corrupted PDF)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Server error during upload",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocument(
            @Parameter(description = "PDF file to analyze (max 50MB)", required = true)
            @RequestParam("file") MultipartFile file) {
        // Validate file is not empty
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_FILE", "File is required and cannot be empty"));
        }
        
        // Validate file size (50MB = 52,428,800 bytes)
        if (file.getSize() > 52_428_800) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("FILE_TOO_LARGE", 
                    "File size exceeds maximum limit of 50MB"));
        }
        
        // Validate MIME type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_FILE_TYPE", 
                    "Only PDF files are accepted"));
        }
        
        try {
            DocumentUploadResponse response = uploadService.uploadDocument(file);
            
            // Trigger async processing
            logger.info("Triggering async processing for document: {}", response.getDocumentId());
            asyncAnalysisService.processDocument(response.getDocumentId());
            logger.info("Async processing triggered successfully for document: {}", response.getDocumentId());
            
            return ResponseEntity.ok(response);
        } catch (InvalidFileException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_PDF", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("UPLOAD_FAILED", 
                    "Failed to upload document: " + e.getMessage()));
        }
    }
    
    /**
     * Get the current processing status of a document.
     * 
     * Returns current DocumentStatus with processing stage and chunk progress.
     * Returns 404 Not Found for non-existent document IDs.
     * 
     * Requirements: 7.2, 10.7, 10.8, 10.9
     */
    @Operation(
        summary = "Get document processing status",
        description = "Check the current processing status of an uploaded document, including progress of each AI agent."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Status retrieved successfully",
            content = @Content(schema = @Schema(implementation = DocumentStatus.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Document not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Server error retrieving status",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{documentId}/status")
    public ResponseEntity<?> getDocumentStatus(
            @Parameter(description = "Document ID returned from upload", required = true)
            @PathVariable String documentId) {
        try {
            DocumentStatus status = statusService.getStatus(documentId);
            return ResponseEntity.ok(status);
        } catch (DocumentNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("DOCUMENT_NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("STATUS_RETRIEVAL_FAILED", 
                    "Failed to retrieve status: " + e.getMessage()));
        }
    }
    
    /**
     * Get the consensus report for a completed document analysis.
     * 
     * Returns ConsensusReport if complete, or current status if still processing.
     * Returns 404 Not Found for non-existent document IDs.
     * 
     * Requirements: 7.1, 7.2, 7.3, 7.5
     */
    @Operation(
        summary = "Get consensus analysis report",
        description = "Retrieve the final consensus report synthesized from all AI agent analyses. " +
                     "Returns processing status if analysis is not yet complete."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Consensus report retrieved successfully or document still processing",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Document not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Server error retrieving results",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{documentId}/results")
    public ResponseEntity<?> getResults(
            @Parameter(description = "Document ID returned from upload", required = true)
            @PathVariable String documentId) {
        try {
            // Get consensus report and format as JSON
            var consensusReport = resultsService.getConsensusReport(documentId);
            String jsonReport = resultsService.formatAsJSON(consensusReport);
            return ResponseEntity.ok(jsonReport);
        } catch (DocumentNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("DOCUMENT_NOT_FOUND", e.getMessage()));
        } catch (DocumentStillProcessingException e) {
            // Return current status if document is still processing
            try {
                DocumentStatus status = statusService.getStatus(documentId);
                return ResponseEntity.ok(status);
            } catch (Exception statusEx) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("STATUS_RETRIEVAL_FAILED", 
                        "Document is still processing but status could not be retrieved"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("RESULTS_RETRIEVAL_FAILED", 
                    "Failed to retrieve results: " + e.getMessage()));
        }
    }
    
    /**
     * Get detailed results including all individual agent reports and consensus.
     * 
     * Returns all 6 AnalysisReports plus ConsensusReport.
     * Returns 404 Not Found for non-existent document IDs.
     * 
     * Requirements: 7.4, 7.5
     */
    @Operation(
        summary = "Get detailed analysis results",
        description = "Retrieve detailed results including individual reports from all 6 AI agents " +
                     "(Lead Analyst, General Analyst, Methodology Reviewer, Literature Reviewer, " +
                     "Quick Screener, Fact Extractor) plus the consensus report."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Detailed results retrieved successfully or document still processing",
            content = @Content(schema = @Schema(implementation = DetailedResultsDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Document not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Server error retrieving detailed results",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{documentId}/results/detailed")
    public ResponseEntity<?> getDetailedResults(
            @Parameter(description = "Document ID returned from upload", required = true)
            @PathVariable String documentId) {
        try {
            DetailedResultsDTO detailedResults = resultsService.getDetailedResults(documentId);
            return ResponseEntity.ok(detailedResults);
        } catch (DocumentNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("DOCUMENT_NOT_FOUND", e.getMessage()));
        } catch (DocumentStillProcessingException e) {
            // Return current status if document is still processing
            try {
                DocumentStatus status = statusService.getStatus(documentId);
                return ResponseEntity.ok(status);
            } catch (Exception statusEx) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("STATUS_RETRIEVAL_FAILED", 
                        "Document is still processing but status could not be retrieved"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("RESULTS_RETRIEVAL_FAILED", 
                    "Failed to retrieve detailed results: " + e.getMessage()));
        }
    }
    
    /**
     * Export analysis results as a PDF document.
     * 
     * Returns a formatted PDF report containing the consensus analysis.
     * Returns 404 Not Found for non-existent document IDs.
     * 
     * Requirements: 7.5
     */
    @Operation(
        summary = "Export analysis results as PDF",
        description = "Download the consensus analysis report as a formatted PDF document."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "PDF report generated successfully",
            content = @Content(mediaType = "application/pdf")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Document not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Document still processing",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Server error generating PDF",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping(value = "/{documentId}/results/pdf", produces = "application/pdf")
    public ResponseEntity<?> exportResultsAsPDF(
            @Parameter(description = "Document ID returned from upload", required = true)
            @PathVariable String documentId) {
        try {
            DetailedResultsDTO detailedResults = resultsService.getDetailedResults(documentId);
            byte[] pdfBytes = pdfExportService.generatePDFReport(detailedResults);
            
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"analysis-report-" + documentId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
                
        } catch (DocumentNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("DOCUMENT_NOT_FOUND", e.getMessage()));
        } catch (DocumentStillProcessingException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DOCUMENT_PROCESSING", 
                    "Document is still being processed. Please wait until analysis is complete."));
        } catch (PDFGenerationException e) {
            logger.error("Failed to generate PDF for document {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("PDF_GENERATION_FAILED", 
                    "Failed to generate PDF report: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error exporting PDF for document {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("EXPORT_FAILED", 
                    "Failed to export results: " + e.getMessage()));
        }
    }
    
}
