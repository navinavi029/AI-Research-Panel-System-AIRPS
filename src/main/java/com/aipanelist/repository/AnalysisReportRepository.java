package com.aipanelist.repository;

import com.aipanelist.model.AnalysisReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for AnalysisReport entity.
 * Provides CRUD operations for agent analysis reports.
 */
@Repository
public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, String> {
    
    /**
     * Find all analysis reports for a document.
     *
     * @param documentId the document identifier
     * @return a list of analysis reports for the document
     */
    List<AnalysisReport> findByDocumentId(String documentId);
}
