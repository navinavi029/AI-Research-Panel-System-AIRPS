package com.aipanelist.processing;

import com.aipanelist.model.Document;
import com.aipanelist.model.ExtractedDocument;
import com.aipanelist.model.ProcessingStatus;
import com.aipanelist.repository.DocumentRepository;
import com.aipanelist.repository.ExtractedDocumentRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DocumentProcessorImpl.
 * Tests text extraction, token counting, and document size validation.
 */
class DocumentProcessorTest {
    
    @TempDir
    Path tempDir;
    
    private DocumentProcessor documentProcessor;
    private ExtractedDocumentRepository extractedDocumentRepository;
    private DocumentRepository documentRepository;
    
    @BeforeEach
    void setUp() {
        extractedDocumentRepository = mock(ExtractedDocumentRepository.class);
        documentRepository = mock(DocumentRepository.class);
        documentProcessor = new DocumentProcessorImpl(extractedDocumentRepository, documentRepository);
    }
    
    @Test
    void extractText_ValidPDF_ExtractsTextSuccessfully() throws Exception {
        // Arrange
        String documentId = "test-doc-1";
        String expectedText = "This is a test document with some content.";
        Path pdfPath = createTestPDF(expectedText, 1);
        
        Document mockDocument = new Document(
            documentId, "test.pdf", 1024L, LocalDateTime.now(),
            ProcessingStatus.UPLOADED, null, 0, 0
        );
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(mockDocument));
        when(extractedDocumentRepository.save(any(ExtractedDocument.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        ExtractedDocument result = documentProcessor.extractText(documentId, pdfPath);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDocumentId()).isEqualTo(documentId);
        assertThat(result.getExtractedText()).contains("test document");
        assertThat(result.getTokenCount()).isGreaterThan(0);
        assertThat(result.isReadingOrderPreserved()).isTrue();
        assertThat(result.getExtractedAt()).isNotNull();
        
        // Verify repository interactions
        verify(extractedDocumentRepository).save(any(ExtractedDocument.class));
        verify(documentRepository).findById(documentId);
        verify(documentRepository).save(mockDocument);
    }
    
    @Test
    void extractText_NonExistentFile_ThrowsExtractionException() {
        // Arrange
        String documentId = "test-doc-2";
        Path nonExistentPath = tempDir.resolve("nonexistent.pdf");
        
        // Act & Assert
        assertThatThrownBy(() -> documentProcessor.extractText(documentId, nonExistentPath))
            .isInstanceOf(ExtractionException.class)
            .hasMessageContaining("PDF file not found");
    }
    
    @Test
    void extractText_TooManyPages_ThrowsExtractionException() throws Exception {
        // Arrange
        String documentId = "test-doc-3";
        Path pdfPath = createTestPDF("Test content", 501); // Exceeds 500 page limit
        
        // Act & Assert
        assertThatThrownBy(() -> documentProcessor.extractText(documentId, pdfPath))
            .isInstanceOf(ExtractionException.class)
            .hasMessageContaining("exceeds maximum page limit")
            .hasMessageContaining("501 pages");
    }
    
    @Test
    void extractText_TooManyTokens_ThrowsExtractionException() throws Exception {
        // Arrange
        String documentId = "test-doc-4";
        // Create text with > 1,000,000 tokens (4,000,001 characters)
        String largeText = "a".repeat(4_000_001);
        Path pdfPath = createTestPDF(largeText, 1);
        
        // Act & Assert
        assertThatThrownBy(() -> documentProcessor.extractText(documentId, pdfPath))
            .isInstanceOf(ExtractionException.class)
            .hasMessageContaining("exceeds maximum token limit");
    }
    
    @Test
    void extractText_EmptyPDF_ReturnsEmptyText() throws Exception {
        // Arrange
        String documentId = "test-doc-5";
        Path pdfPath = createTestPDF("", 1); // Empty content
        
        Document mockDocument = new Document(
            documentId, "empty.pdf", 1024L, LocalDateTime.now(),
            ProcessingStatus.UPLOADED, null, 0, 0
        );
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(mockDocument));
        when(extractedDocumentRepository.save(any(ExtractedDocument.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        ExtractedDocument result = documentProcessor.extractText(documentId, pdfPath);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getExtractedText()).isEmpty();
        assertThat(result.getTokenCount()).isEqualTo(0);
    }
    
    @Test
    void extractText_UpdatesDocumentMetadata() throws Exception {
        // Arrange
        String documentId = "test-doc-6";
        Path pdfPath = createTestPDF("Test content for metadata", 3);
        
        Document mockDocument = new Document(
            documentId, "test.pdf", 1024L, LocalDateTime.now(),
            ProcessingStatus.UPLOADED, null, 0, 0
        );
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(mockDocument));
        when(extractedDocumentRepository.save(any(ExtractedDocument.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        documentProcessor.extractText(documentId, pdfPath);
        
        // Assert
        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(documentCaptor.capture());
        
        Document savedDocument = documentCaptor.getValue();
        assertThat(savedDocument.getTotalPages()).isEqualTo(3);
        assertThat(savedDocument.getTotalTokens()).isGreaterThan(0);
    }
    
    @Test
    void calculateTokenCount_NullText_ReturnsZero() {
        // Act
        int result = documentProcessor.calculateTokenCount(null);
        
        // Assert
        assertThat(result).isEqualTo(0);
    }
    
    @Test
    void calculateTokenCount_EmptyText_ReturnsZero() {
        // Act
        int result = documentProcessor.calculateTokenCount("");
        
        // Assert
        assertThat(result).isEqualTo(0);
    }
    
    @Test
    void calculateTokenCount_ValidText_ReturnsApproximateCount() {
        // Arrange
        String text = "This is a test"; // 14 characters = ~3.5 tokens
        
        // Act
        int result = documentProcessor.calculateTokenCount(text);
        
        // Assert
        assertThat(result).isEqualTo(3); // 14 / 4 = 3
    }
    
    @Test
    void calculateTokenCount_LargeText_ReturnsCorrectCount() {
        // Arrange
        String text = "a".repeat(400); // 400 characters = 100 tokens
        
        // Act
        int result = documentProcessor.calculateTokenCount(text);
        
        // Assert
        assertThat(result).isEqualTo(100);
    }
    
    @Test
    void extractText_CorruptedPDF_ThrowsExtractionException() throws Exception {
        // Arrange
        String documentId = "test-doc-7";
        Path corruptedPath = tempDir.resolve("corrupted.pdf");
        Files.writeString(corruptedPath, "This is not a valid PDF file");
        
        // Act & Assert
        assertThatThrownBy(() -> documentProcessor.extractText(documentId, corruptedPath))
            .isInstanceOf(ExtractionException.class)
            .hasMessageContaining("Failed to extract text from PDF");
    }
    
    /**
     * Helper method to create a test PDF with specified content and page count.
     */
    private Path createTestPDF(String content, int pageCount) throws IOException {
        Path pdfPath = tempDir.resolve("test-" + System.nanoTime() + ".pdf");
        
        try (PDDocument document = new PDDocument()) {
            for (int i = 0; i < pageCount; i++) {
                PDPage page = new PDPage();
                document.addPage(page);
                
                // Only add content to first page to avoid excessive processing
                if (i == 0 && content != null && !content.isEmpty()) {
                    try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                        contentStream.beginText();
                        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        contentStream.newLineAtOffset(50, 700);
                        
                        // Split content into lines to avoid exceeding page width
                        String[] lines = content.split("(?<=\\G.{80})");
                        for (String line : lines) {
                            contentStream.showText(line);
                            contentStream.newLineAtOffset(0, -15);
                        }
                        
                        contentStream.endText();
                    }
                }
            }
            
            document.save(pdfPath.toFile());
        }
        
        return pdfPath;
    }
}
