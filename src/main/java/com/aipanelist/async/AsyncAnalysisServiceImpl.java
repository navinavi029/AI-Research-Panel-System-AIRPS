package com.aipanelist.async;

import com.aipanelist.consensus.ConsensusEngine;
import com.aipanelist.model.*;
import com.aipanelist.orchestration.AnalysisPanel;
import com.aipanelist.orchestration.PanelOrchestrator;
import com.aipanelist.processing.ChunkProcessor;
import com.aipanelist.processing.DocumentProcessor;
import com.aipanelist.processing.DocumentStatusService;
import com.aipanelist.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Implementation of AsyncAnalysisService for asynchronous document processing.
 * 
 * Orchestrates the complete analysis pipeline in a background thread pool.
 * Updates document status at each stage and handles errors gracefully.
 * 
 * Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6
 */
@Service
public class AsyncAnalysisServiceImpl implements AsyncAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncAnalysisServiceImpl.class);
    
    private final DocumentProcessor documentProcessor;
    private final ChunkProcessor chunkProcessor;
    private final PanelOrchestrator panelOrchestrator;
    private final ConsensusEngine consensusEngine;
    private final DocumentStatusService statusService;
    private final DocumentRepository documentRepository;
    
    public AsyncAnalysisServiceImpl(DocumentProcessor documentProcessor,
                                   ChunkProcessor chunkProcessor,
                                   PanelOrchestrator panelOrchestrator,
                                   ConsensusEngine consensusEngine,
                                   DocumentStatusService statusService,
                                   DocumentRepository documentRepository) {
        this.documentProcessor = documentProcessor;
        this.chunkProcessor = chunkProcessor;
        this.panelOrchestrator = panelOrchestrator;
        this.consensusEngine = consensusEngine;
        this.statusService = statusService;
        this.documentRepository = documentRepository;
    }
    
    @Override
    @Async
    public void processDocument(String documentId) {
        logger.info("Starting asynchronous processing for document: {}", documentId);
        
        try {
            // Stage 1: Extract text from PDF
            logger.info("Stage 1: Extracting text for document {}", documentId);
            statusService.updateStatus(documentId, ProcessingStatus.PROCESSING);
            
            Path pdfPath = Paths.get("/app/data/uploads", documentId + ".pdf");
            ExtractedDocument extractedDocument = documentProcessor.extractText(documentId, pdfPath);
            
            // Stage 2: Chunk document and run panel analysis
            logger.info("Stage 2: Chunking and analyzing document {}", documentId);
            statusService.updateStatus(documentId, ProcessingStatus.ANALYZING);
            
            List<DocumentChunk> chunks = chunkProcessor.chunkDocument(extractedDocument);
            AnalysisPanel panel = panelOrchestrator.createPanel(documentId);
            List<AnalysisReport> agentReports = panelOrchestrator.orchestrateAnalysis(panel, chunks);
            
            // Check if we have any successful agent reports
            if (agentReports.isEmpty()) {
                throw new RuntimeException("All agents failed to produce reports");
            }
            
            // Stage 3: Generate consensus report
            logger.info("Stage 3: Generating consensus for document {}", documentId);
            statusService.updateStatus(documentId, ProcessingStatus.DELIBERATING);
            
            ConsensusReport consensusReport = consensusEngine.generateConsensus(agentReports);
            
            // Stage 4: Mark as complete
            logger.info("Stage 4: Marking document {} as complete", documentId);
            statusService.updateStatus(documentId, ProcessingStatus.COMPLETE);
            
            logger.info("Successfully completed processing for document: {}", documentId);
            
        } catch (Exception e) {
            logger.error("Error processing document {}: {}", documentId, e.getMessage(), e);
            
            // Update status to FAILED with error message
            try {
                statusService.updateStatus(documentId, ProcessingStatus.FAILED);
                
                // Store error message in document
                documentRepository.findById(documentId).ifPresent(doc -> {
                    doc.setErrorMessage(e.getMessage());
                    documentRepository.save(doc);
                });
                
            } catch (Exception updateError) {
                logger.error("Failed to update error status for document {}", documentId, updateError);
            }
        }
    }
}
