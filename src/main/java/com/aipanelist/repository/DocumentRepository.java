package com.aipanelist.repository;

import com.aipanelist.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Document entity.
 * Provides CRUD operations for document metadata and processing status.
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {
}
