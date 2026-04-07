package com.aipanelist.repository;

import com.aipanelist.model.ConsensusReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for ConsensusReport entity.
 * Provides CRUD operations for consensus reports generated from panel deliberation.
 */
@Repository
public interface ConsensusReportRepository extends JpaRepository<ConsensusReport, String> {
    
    /**
     * Find a consensus report by its document ID.
     *
     * @param documentId the document identifier
     * @return an Optional containing the consensus report if found
     */
    Optional<ConsensusReport> findByDocumentId(String documentId);
}
