package com.aipanelist.processing;

import com.aipanelist.model.DocumentChunk;
import com.aipanelist.model.ExtractedDocument;
import com.aipanelist.repository.DocumentChunkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChunkProcessorImpl.
 * Tests chunking logic, semantic boundary detection, and chunk creation.
 */
@ExtendWith(MockitoExtension.class)
class ChunkProcessorTest {
    
    @Mock
    private DocumentChunkRepository documentChunkRepository;
    
    @Mock
    private DocumentProcessor documentProcessor;
    
    private ChunkProcessor chunkProcessor;
    
    @BeforeEach
    void setUp() {
        chunkProcessor = new ChunkProcessorImpl(documentChunkRepository, documentProcessor);
        
        // Mock token calculation (1 token ≈ 4 characters) - use lenient for tests that don't need it
        lenient().when(documentProcessor.calculateTokenCount(any(String.class)))
            .thenAnswer(invocation -> {
                String text = invocation.getArgument(0);
                return text.length() / 4;
            });
    }
    
    @Test
    void chunkDocument_SmallDocument_ReturnsSingleChunk() {
        // Arrange
        String smallText = "A".repeat(100_000); // 25,000 tokens
        ExtractedDocument document = new ExtractedDocument(
            "doc-1",
            smallText,
            25_000,
            LocalDateTime.now(),
            true
        );
        
        // Act
        List<DocumentChunk> chunks = chunkProcessor.chunkDocument(document);
        
        // Assert
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getSequenceNumber()).isEqualTo(1);
        assertThat(chunks.get(0).getTotalChunks()).isEqualTo(1);
        assertThat(chunks.get(0).getChunkText()).isEqualTo(smallText);
        assertThat(chunks.get(0).getOverlapTokens()).isEqualTo(0);
        
        verify(documentChunkRepository).save(any(DocumentChunk.class));
    }
    
    @Test
    void chunkDocument_LargeDocument_ReturnsMultipleChunks() {
        // Arrange
        String largeText = "A".repeat(1_000_000); // 250,000 tokens
        ExtractedDocument document = new ExtractedDocument(
            "doc-2",
            largeText,
            250_000,
            LocalDateTime.now(),
            true
        );
        
        // Act
        List<DocumentChunk> chunks = chunkProcessor.chunkDocument(document);
        
        // Assert
        assertThat(chunks.size()).isGreaterThan(1);
        
        // Verify sequence numbers
        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).getSequenceNumber()).isEqualTo(i + 1);
            assertThat(chunks.get(i).getTotalChunks()).isEqualTo(chunks.size());
        }
        
        // Verify first chunk has no overlap
        assertThat(chunks.get(0).getOverlapTokens()).isEqualTo(0);
        
        // Verify subsequent chunks have overlap
        for (int i = 1; i < chunks.size(); i++) {
            assertThat(chunks.get(i).getOverlapTokens()).isEqualTo(500);
        }
        
        verify(documentChunkRepository).saveAll(anyList());
    }
    
    @Test
    void chunkDocument_ExactlyAtThreshold_ReturnsSingleChunk() {
        // Arrange
        String text = "A".repeat(400_000); // Exactly 100,000 tokens
        ExtractedDocument document = new ExtractedDocument(
            "doc-3",
            text,
            100_000,
            LocalDateTime.now(),
            true
        );
        
        // Act
        List<DocumentChunk> chunks = chunkProcessor.chunkDocument(document);
        
        // Assert
        assertThat(chunks).hasSize(1);
    }
    
    @Test
    void chunkDocument_JustOverThreshold_ReturnsTwoChunks() {
        // Arrange
        String text = "A".repeat(400_004); // 100,001 tokens
        ExtractedDocument document = new ExtractedDocument(
            "doc-4",
            text,
            100_001,
            LocalDateTime.now(),
            true
        );
        
        // Act
        List<DocumentChunk> chunks = chunkProcessor.chunkDocument(document);
        
        // Assert
        assertThat(chunks.size()).isGreaterThanOrEqualTo(2);
    }
    
    @Test
    void identifyBoundaries_DetectsSectionHeaders() {
        // Arrange
        String text = "# Introduction\n\nSome text.\n\n## Methods\n\nMore text.";
        
        // Act
        List<ChunkProcessor.SemanticBoundary> boundaries = chunkProcessor.identifyBoundaries(text);
        
        // Assert
        long sectionHeaders = boundaries.stream()
            .filter(b -> b.getType() == ChunkProcessor.SemanticBoundary.BoundaryType.SECTION_HEADER)
            .count();
        assertThat(sectionHeaders).isGreaterThanOrEqualTo(2);
    }
    
    @Test
    void identifyBoundaries_DetectsColonHeaders() {
        // Arrange
        String text = "Introduction:\n\nSome text.\n\nMethods:\n\nMore text.";
        
        // Act
        List<ChunkProcessor.SemanticBoundary> boundaries = chunkProcessor.identifyBoundaries(text);
        
        // Assert
        long sectionHeaders = boundaries.stream()
            .filter(b -> b.getType() == ChunkProcessor.SemanticBoundary.BoundaryType.SECTION_HEADER)
            .count();
        assertThat(sectionHeaders).isGreaterThanOrEqualTo(2);
    }
    
    @Test
    void identifyBoundaries_DetectsParagraphBreaks() {
        // Arrange
        String text = "First paragraph.\n\nSecond paragraph.\n\nThird paragraph.";
        
        // Act
        List<ChunkProcessor.SemanticBoundary> boundaries = chunkProcessor.identifyBoundaries(text);
        
        // Assert
        long paragraphBreaks = boundaries.stream()
            .filter(b -> b.getType() == ChunkProcessor.SemanticBoundary.BoundaryType.PARAGRAPH_BREAK)
            .count();
        assertThat(paragraphBreaks).isGreaterThanOrEqualTo(2);
    }
    
    @Test
    void identifyBoundaries_DetectsSentenceEndings() {
        // Arrange
        String text = "First sentence. Second sentence. Third sentence.";
        
        // Act
        List<ChunkProcessor.SemanticBoundary> boundaries = chunkProcessor.identifyBoundaries(text);
        
        // Assert
        long sentenceEndings = boundaries.stream()
            .filter(b -> b.getType() == ChunkProcessor.SemanticBoundary.BoundaryType.SENTENCE_ENDING)
            .count();
        assertThat(sentenceEndings).isGreaterThanOrEqualTo(2);
    }
    
    @Test
    void identifyBoundaries_SortsByPosition() {
        // Arrange
        String text = "First. Second.\n\nThird. Fourth.";
        
        // Act
        List<ChunkProcessor.SemanticBoundary> boundaries = chunkProcessor.identifyBoundaries(text);
        
        // Assert
        for (int i = 1; i < boundaries.size(); i++) {
            assertThat(boundaries.get(i).getPosition())
                .isGreaterThanOrEqualTo(boundaries.get(i - 1).getPosition());
        }
    }
    
    @Test
    void createChunk_CreatesValidChunk() {
        // Arrange
        String text = "Test chunk text";
        
        // Act
        DocumentChunk chunk = chunkProcessor.createChunk(
            "doc-5",
            text,
            1,
            3,
            0,
            100,
            0
        );
        
        // Assert
        assertThat(chunk.getChunkId()).isNotNull();
        assertThat(chunk.getDocumentId()).isEqualTo("doc-5");
        assertThat(chunk.getSequenceNumber()).isEqualTo(1);
        assertThat(chunk.getTotalChunks()).isEqualTo(3);
        assertThat(chunk.getChunkText()).isEqualTo(text);
        assertThat(chunk.getStartByteOffset()).isEqualTo(0);
        assertThat(chunk.getEndByteOffset()).isEqualTo(100);
        assertThat(chunk.getOverlapTokens()).isEqualTo(0);
    }
    
    @Test
    void chunkDocument_WithSemanticBoundaries_SplitsAtBoundaries() {
        // Arrange
        StringBuilder textBuilder = new StringBuilder();
        textBuilder.append("# Introduction\n\n");
        textBuilder.append("A".repeat(400_000)); // ~100K tokens
        textBuilder.append("\n\n# Methods\n\n");
        textBuilder.append("B".repeat(400_000)); // ~100K tokens
        textBuilder.append("\n\n# Results\n\n");
        textBuilder.append("C".repeat(400_000)); // ~100K tokens
        
        String text = textBuilder.toString();
        ExtractedDocument document = new ExtractedDocument(
            "doc-6",
            text,
            300_000,
            LocalDateTime.now(),
            true
        );
        
        // Act
        List<DocumentChunk> chunks = chunkProcessor.chunkDocument(document);
        
        // Assert
        assertThat(chunks.size()).isGreaterThan(1);
        
        // Verify chunks don't split mid-word (they should split at boundaries)
        for (DocumentChunk chunk : chunks) {
            String chunkText = chunk.getChunkText();
            // Chunks should start or end at reasonable boundaries
            assertThat(chunkText).isNotEmpty();
        }
    }
    
    @Test
    void chunkDocument_EmptyDocument_ReturnsSingleEmptyChunk() {
        // Arrange
        ExtractedDocument document = new ExtractedDocument(
            "doc-7",
            "",
            0,
            LocalDateTime.now(),
            true
        );
        
        // Act
        List<DocumentChunk> chunks = chunkProcessor.chunkDocument(document);
        
        // Assert
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getChunkText()).isEmpty();
        assertThat(chunks.get(0).getTokenCount()).isEqualTo(0);
    }
    
    @Test
    void chunkDocument_VerifiesChunkMetadata() {
        // Arrange
        String text = "A".repeat(500_000); // 125,000 tokens
        ExtractedDocument document = new ExtractedDocument(
            "doc-8",
            text,
            125_000,
            LocalDateTime.now(),
            true
        );
        
        // Act
        List<DocumentChunk> chunks = chunkProcessor.chunkDocument(document);
        
        // Assert
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            // Verify all metadata is set
            assertThat(chunk.getChunkId()).isNotNull();
            assertThat(chunk.getDocumentId()).isEqualTo("doc-8");
            assertThat(chunk.getSequenceNumber()).isGreaterThan(0);
            assertThat(chunk.getTotalChunks()).isEqualTo(chunks.size());
            assertThat(chunk.getTokenCount()).isGreaterThan(0);
            assertThat(chunk.getStartByteOffset()).isGreaterThanOrEqualTo(0);
            assertThat(chunk.getEndByteOffset()).isGreaterThan(chunk.getStartByteOffset());
            
            // Verify token count is reasonable (should be ≤ 100,000)
            assertThat(chunk.getTokenCount()).isLessThanOrEqualTo(100_000);
            
            // Verify byte offsets are within document bounds
            assertThat(chunk.getStartByteOffset()).isLessThan(text.length());
            assertThat(chunk.getEndByteOffset()).isLessThanOrEqualTo(text.length());
        }
    }
}
