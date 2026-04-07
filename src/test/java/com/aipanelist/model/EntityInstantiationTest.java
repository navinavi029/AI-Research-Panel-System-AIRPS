package com.aipanelist.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying that all entity classes can be instantiated correctly.
 * Tests basic entity creation and field access.
 */
class EntityInstantiationTest {

    @Test
    void documentEntity_CanBeInstantiated() {
        // Arrange & Act
        Document document = new Document();
        document.setDocumentId("doc-123");
        document.setFilename("test.pdf");
        document.setFileSizeBytes(1024000L);
        document.setUploadedAt(LocalDateTime.now());
        document.setStatus(ProcessingStatus.UPLOADED);
        document.setTotalTokens(0);
        document.setTotalPages(0);

        // Assert
        assertThat(document.getDocumentId()).isEqualTo("doc-123");
        assertThat(document.getFilename()).isEqualTo("test.pdf");
        assertThat(document.getStatus()).isEqualTo(ProcessingStatus.UPLOADED);
    }

    @Test
    void extractedDocumentEntity_CanBeInstantiated() {
        // Arrange & Act
        ExtractedDocument extracted = new ExtractedDocument();
        extracted.setDocumentId("doc-123");
        extracted.setExtractedText("Sample extracted text");
        extracted.setTokenCount(1000);
        extracted.setExtractedAt(LocalDateTime.now());
        extracted.setReadingOrderPreserved(true);

        // Assert
        assertThat(extracted.getDocumentId()).isEqualTo("doc-123");
        assertThat(extracted.getExtractedText()).isEqualTo("Sample extracted text");
        assertThat(extracted.isReadingOrderPreserved()).isTrue();
    }

    @Test
    void documentChunkEntity_CanBeInstantiated() {
        // Arrange & Act
        DocumentChunk chunk = new DocumentChunk();
        chunk.setChunkId("chunk-1");
        chunk.setDocumentId("doc-123");
        chunk.setSequenceNumber(1);
        chunk.setTotalChunks(3);
        chunk.setChunkText("Chunk text content");
        chunk.setTokenCount(5000);
        chunk.setStartByteOffset(0L);
        chunk.setEndByteOffset(20000L);
        chunk.setOverlapTokens(500);

        // Assert
        assertThat(chunk.getChunkId()).isEqualTo("chunk-1");
        assertThat(chunk.getSequenceNumber()).isEqualTo(1);
        assertThat(chunk.getTotalChunks()).isEqualTo(3);
        assertThat(chunk.getOverlapTokens()).isEqualTo(500);
    }

    @Test
    void analysisReportEntity_CanBeInstantiated() {
        // Arrange & Act
        AnalysisReport report = new AnalysisReport();
        report.setReportId("report-1");
        report.setDocumentId("doc-123");
        report.setAgentId("agent-1");
        report.setAgentType(AgentType.LEAD_ANALYST);
        report.setKeyFindings("Key findings");
        report.setStrengths("Strengths");
        report.setWeaknesses("Weaknesses");
        report.setRecommendations("Recommendations");
        report.setCompletedAt(LocalDateTime.now());
        report.setChunksAnalyzed(3);
        report.setChunksFailed(0);

        // Assert
        assertThat(report.getReportId()).isEqualTo("report-1");
        assertThat(report.getAgentType()).isEqualTo(AgentType.LEAD_ANALYST);
        assertThat(report.getChunksAnalyzed()).isEqualTo(3);
    }

    @Test
    void chunkAnalysisEntity_CanBeInstantiated() {
        // Arrange & Act
        ChunkAnalysis analysis = new ChunkAnalysis();
        analysis.setAnalysisId("analysis-1");
        analysis.setReportId("report-1");
        analysis.setChunkId("chunk-1");
        analysis.setChunkSequence(1);
        analysis.setFindings("Chunk findings");
        analysis.setContextSummary("Context summary");
        analysis.setAnalyzedAt(LocalDateTime.now());

        // Assert
        assertThat(analysis.getAnalysisId()).isEqualTo("analysis-1");
        assertThat(analysis.getChunkSequence()).isEqualTo(1);
    }

    @Test
    void consensusReportEntity_CanBeInstantiated() {
        // Arrange & Act
        ConsensusReport consensus = new ConsensusReport(
            "consensus-1",
            "doc-123",
            "Common themes",
            "Agreements",
            "Disagreements",
            "Recommendations",
            "Attributed insights",
            LocalDateTime.now(),
            6
        );

        // Assert
        assertThat(consensus.getReportId()).isEqualTo("consensus-1");
        assertThat(consensus.getAgentReportsIncluded()).isEqualTo(6);
    }

    @Test
    void agentProgressEntity_CanBeInstantiated() {
        // Arrange & Act
        AgentProgress progress = new AgentProgress(
            "progress-1",
            "doc-123",
            "agent-1",
            AgentType.GENERAL_ANALYST,
            2,
            5,
            LocalDateTime.now()
        );

        // Assert
        assertThat(progress.getProgressId()).isEqualTo("progress-1");
        assertThat(progress.getChunksCompleted()).isEqualTo(2);
        assertThat(progress.getTotalChunks()).isEqualTo(5);
    }

    @Test
    void processingStatusEnum_HasAllExpectedValues() {
        // Assert
        assertThat(ProcessingStatus.values()).containsExactlyInAnyOrder(
            ProcessingStatus.UPLOADED,
            ProcessingStatus.PROCESSING,
            ProcessingStatus.ANALYZING,
            ProcessingStatus.DELIBERATING,
            ProcessingStatus.COMPLETE,
            ProcessingStatus.FAILED
        );
    }

    @Test
    void agentTypeEnum_HasAllExpectedValues() {
        // Assert
        assertThat(AgentType.values()).containsExactlyInAnyOrder(
            AgentType.LEAD_ANALYST,
            AgentType.GENERAL_ANALYST,
            AgentType.METHODOLOGY_REVIEWER,
            AgentType.LITERATURE_REVIEWER,
            AgentType.QUICK_SCREENER,
            AgentType.FACT_EXTRACTOR
        );
    }
}
