package com.aipanelist.api;

import com.aipanelist.consensus.ConsensusGenerationException;
import com.aipanelist.model.AllAgentsFailedException;
import com.aipanelist.model.APIException;
import com.aipanelist.model.MaxRetriesExceededException;
import com.aipanelist.processing.ExtractionException;
import com.aipanelist.upload.InvalidFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the AI Panelist System.
 * 
 * Provides centralized exception handling with:
 * - Consistent error response format
 * - Appropriate HTTP status codes
 * - Structured logging with context
 * - Security-conscious error messages (no sensitive data exposure)
 * 
 * Requirements: 9.1, 9.2, 9.5
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle document not found exceptions.
     * Returns 404 Not Found.
     */
    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDocumentNotFound(
            DocumentNotFoundException ex, WebRequest request) {
        
        String documentId = extractDocumentId(ex.getMessage());
        logger.warn("Document not found: documentId={}", documentId);
        
        ErrorResponse error = new ErrorResponse(
            "DOCUMENT_NOT_FOUND",
            ex.getMessage(),
            documentId
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    /**
     * Handle document still processing exceptions.
     * Returns 202 Accepted with current status.
     */
    @ExceptionHandler(DocumentStillProcessingException.class)
    public ResponseEntity<ErrorResponse> handleDocumentStillProcessing(
            DocumentStillProcessingException ex, WebRequest request) {
        
        logger.info("Document still processing: documentId={}, status={}", 
            ex.getDocumentId(), ex.getCurrentStatus());
        
        Map<String, Object> details = new HashMap<>();
        details.put("currentStatus", ex.getCurrentStatus().toString());
        
        ErrorResponse error = new ErrorResponse(
            "DOCUMENT_PROCESSING",
            ex.getMessage(),
            ex.getDocumentId(),
            details
        );
        
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(error);
    }
    
    /**
     * Handle invalid file exceptions.
     * Returns 400 Bad Request.
     */
    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFile(
            InvalidFileException ex, WebRequest request) {
        
        logger.warn("Invalid file upload: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            "INVALID_FILE",
            ex.getMessage()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * Handle file size exceeded exceptions.
     * Returns 413 Payload Too Large.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex, WebRequest request) {
        
        logger.warn("File size exceeded: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            "FILE_TOO_LARGE",
            "File size exceeds maximum limit of 50MB"
        );
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }
    
    /**
     * Handle extraction exceptions.
     * Returns 422 Unprocessable Entity.
     */
    @ExceptionHandler(ExtractionException.class)
    public ResponseEntity<ErrorResponse> handleExtractionException(
            ExtractionException ex, WebRequest request) {
        
        String documentId = extractDocumentId(request.getDescription(false));
        logger.error("Document extraction failed: documentId={}, error={}", 
            documentId, ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
            "EXTRACTION_FAILED",
            ex.getMessage(),
            documentId
        );
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }
    
    /**
     * Handle consensus generation exceptions.
     * Returns 500 Internal Server Error.
     */
    @ExceptionHandler(ConsensusGenerationException.class)
    public ResponseEntity<ErrorResponse> handleConsensusGenerationException(
            ConsensusGenerationException ex, WebRequest request) {
        
        String documentId = extractDocumentId(request.getDescription(false));
        logger.error("Consensus generation failed: documentId={}, error={}", 
            documentId, ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
            "CONSENSUS_GENERATION_FAILED",
            "Failed to generate consensus report: " + ex.getMessage(),
            documentId
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    /**
     * Handle all agents failed exceptions.
     * Returns 500 Internal Server Error.
     */
    @ExceptionHandler(AllAgentsFailedException.class)
    public ResponseEntity<ErrorResponse> handleAllAgentsFailed(
            AllAgentsFailedException ex, WebRequest request) {
        
        logger.error("All agents failed: documentId={}, totalAgents={}", 
            ex.getDocumentId(), ex.getTotalAgents(), ex);
        
        Map<String, Object> details = new HashMap<>();
        details.put("totalAgents", ex.getTotalAgents());
        details.put("successfulAgents", 0);
        
        ErrorResponse error = new ErrorResponse(
            "ALL_AGENTS_FAILED",
            ex.getMessage(),
            ex.getDocumentId(),
            details
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    /**
     * Handle max retries exceeded exceptions.
     * Returns 503 Service Unavailable.
     */
    @ExceptionHandler(MaxRetriesExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxRetriesExceeded(
            MaxRetriesExceededException ex, WebRequest request) {
        
        String documentId = extractDocumentId(request.getDescription(false));
        logger.error("Max retries exceeded: operation={}, maxRetries={}, documentId={}", 
            ex.getOperation(), ex.getMaxRetries(), documentId, ex);
        
        Map<String, Object> details = new HashMap<>();
        details.put("operation", ex.getOperation());
        details.put("maxRetries", ex.getMaxRetries());
        
        ErrorResponse error = new ErrorResponse(
            "MAX_RETRIES_EXCEEDED",
            ex.getMessage(),
            documentId,
            details
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }
    
    /**
     * Handle API exceptions (NVIDIA API errors).
     * Returns 502 Bad Gateway.
     */
    @ExceptionHandler(APIException.class)
    public ResponseEntity<ErrorResponse> handleAPIException(
            APIException ex, WebRequest request) {
        
        String documentId = extractDocumentId(request.getDescription(false));
        logger.error("External API error: statusCode={}, documentId={}, error={}", 
            ex.getStatusCode(), documentId, ex.getMessage(), ex);
        
        Map<String, Object> details = new HashMap<>();
        details.put("apiStatusCode", ex.getStatusCode());
        
        ErrorResponse error = new ErrorResponse(
            "EXTERNAL_API_ERROR",
            "External API request failed: " + ex.getMessage(),
            documentId,
            details
        );
        
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }
    
    /**
     * Handle all other unhandled exceptions.
     * Returns 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        
        String documentId = extractDocumentId(request.getDescription(false));
        logger.error("Unhandled exception: documentId={}, type={}, error={}", 
            documentId, ex.getClass().getSimpleName(), ex.getMessage(), ex);
        
        // Don't expose internal error details to clients
        ErrorResponse error = new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred. Please try again later.",
            documentId
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .body(error);
    }
    
    /**
     * Extract document ID from request path or exception message.
     */
    private String extractDocumentId(String input) {
        if (input == null) {
            return null;
        }
        
        // Try to extract from path like "/api/documents/{documentId}/..."
        if (input.contains("/documents/")) {
            String[] parts = input.split("/documents/");
            if (parts.length > 1) {
                String[] subParts = parts[1].split("/");
                if (subParts.length > 0 && !subParts[0].isEmpty()) {
                    return subParts[0];
                }
            }
        }
        
        // Try to extract from message like "Document ... not found"
        if (input.contains("Document ") && input.contains(" not found")) {
            String[] parts = input.split("Document ");
            if (parts.length > 1) {
                String[] subParts = parts[1].split(" ");
                if (subParts.length > 0) {
                    return subParts[0];
                }
            }
        }
        
        return null;
    }
}
