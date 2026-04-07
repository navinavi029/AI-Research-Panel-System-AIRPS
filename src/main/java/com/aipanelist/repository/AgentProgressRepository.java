package com.aipanelist.repository;

import com.aipanelist.model.AgentProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for AgentProgress entity.
 * Provides CRUD operations for tracking agent processing progress.
 */
@Repository
public interface AgentProgressRepository extends JpaRepository<AgentProgress, String> {
    
    /**
     * Find agent progress by document ID and agent ID.
     *
     * @param documentId the document identifier
     * @param agentId the agent identifier
     * @return an Optional containing the agent progress if found
     */
    Optional<AgentProgress> findByDocumentIdAndAgentId(String documentId, String agentId);
}
