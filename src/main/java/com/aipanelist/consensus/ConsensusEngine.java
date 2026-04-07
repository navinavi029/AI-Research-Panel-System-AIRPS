package com.aipanelist.consensus;

import com.aipanelist.model.AnalysisReport;
import com.aipanelist.model.ConsensusReport;

import java.util.List;

/**
 * Interface for generating consensus reports from multiple agent analyses.
 * 
 * The ConsensusEngine synthesizes individual agent reports into a unified
 * consensus report that identifies common themes, agreements, disagreements,
 * and provides unified recommendations with proper attribution.
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8
 */
public interface ConsensusEngine {

    /**
     * Generate a consensus report from multiple agent analysis reports.
     * 
     * This method:
     * - Builds a synthesis prompt including all agent reports with agent type labels
     * - Calls NVIDIA model to identify themes, agreements, and disagreements
     * - Parses the model response to extract structured consensus data
     * - Attributes specific insights to source agent types
     * - Stores the consensus report with the document ID
     * 
     * @param reports the list of analysis reports from different agents
     * @return the generated consensus report
     * @throws ConsensusGenerationException if consensus generation fails
     */
    ConsensusReport generateConsensus(List<AnalysisReport> reports) throws ConsensusGenerationException;
}
