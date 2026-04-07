package com.aipanelist.repository;

import com.aipanelist.model.ChunkAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for ChunkAnalysis entity.
 * Provides CRUD operations for chunk-level analysis results.
 */
@Repository
public interface ChunkAnalysisRepository extends JpaRepository<ChunkAnalysis, String> {
    
    /**
     * Find all chunk analyses for a report, ordered by chunk sequence.
     *
     * @param reportId the report identifier
     * @return a list of chunk analyses ordered by chunk sequence
     */
    List<ChunkAnalysis> findByReportIdOrderByChunkSequence(String reportId);
}
