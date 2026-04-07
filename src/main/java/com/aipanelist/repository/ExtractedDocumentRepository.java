package com.aipanelist.repository;

import com.aipanelist.model.ExtractedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for ExtractedDocument entity.
 * Provides CRUD operations for extracted document text and metadata.
 */
@Repository
public interface ExtractedDocumentRepository extends JpaRepository<ExtractedDocument, String> {
    
    /**
     * Find an extracted document by its document ID.
     *
     * @param documentId the document identifier
     * @return an Optional containing the extracted document if found
     */
    Optional<ExtractedDocument> findByDocumentId(String documentId);
}
