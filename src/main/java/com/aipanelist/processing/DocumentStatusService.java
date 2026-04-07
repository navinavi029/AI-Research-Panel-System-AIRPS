package com.aipanelist.processing;

import com.aipanelist.model.ProcessingStatus;

/**
 * Service interface for managing and querying document processing status.
 * Provides methods to update status, track agent progress, and retrieve current status.
 */
public interface DocumentStatusService {
    
    /**
     * Updates the processing status of a document.
     *
     * @param documentId the document identifier
     * @param status the new processing status
     */
    void updateStatus(String documentId, ProcessingStatus status);
    
    /**
     * Updates the chunk processing progress for a specific agent working on a document.
     *
     * @param documentId the document identifier
     * @param agentId the agent identifier
     * @param chunksCompleted the number of chunks completed by this agent
     */
    void updateChunkProgress(String documentId, String agentId, int chunksCompleted);
    
    /**
     * Retrieves the current status of a document including overall status,
     * per-agent progress, error messages, and estimated time remaining.
     *
     * @param documentId the document identifier
     * @return the current document status
     */
    DocumentStatus getStatus(String documentId);
}
