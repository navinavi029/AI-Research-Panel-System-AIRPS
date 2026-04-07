package com.aipanelist.repository;

import com.aipanelist.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for DocumentChunk entity.
 * Provides CRUD operations for document chunks and retrieval by document ID.
 */
@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, String> {
    
    /**
     * Find all chunks for a document, ordered by sequence number.
     *
     * @param documentId the document identifier
     * @return a list of document chunks ordered by sequence number
     */
    List<DocumentChunk> findByDocumentIdOrderBySequenceNumber(String documentId);
}
