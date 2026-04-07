package com.aipanelist.upload;

import com.aipanelist.config.StorageConfiguration;
import com.aipanelist.model.Document;
import com.aipanelist.model.ProcessingStatus;
import com.aipanelist.repository.DocumentRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PDFUploadServiceImpl.
 * 
 * Tests validation, storage, and upload functionality for PDF documents.
 */
class PDFUploadServiceTest {
    
    @TempDir
    Path tempDir;
    
    private PDFUploadService uploadService;
    private StorageConfiguration storageConfiguration;
    private DocumentRepository documentRepository;
    
    @BeforeEach
    void setUp() throws IOException {
        // Create real StorageConfiguration instead of mocking (Java 26 compatibility)
        storageConfiguration = new StorageConfiguration();
        // Use reflection to set the uploadPath field
        try {
            var uploadPathField = StorageConfiguration.class.getDeclaredField("uploadPath");
            uploadPathField.setAccessible(true);
            uploadPathField.set(storageConfiguration, tempDir);
            
            var uploadDirField = StorageConfiguration.class.getDeclaredField("uploadDir");
            uploadDirField.setAccessible(true);
            uploadDirField.set(storageConfiguration, tempDir.toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure StorageConfiguration", e);
        }
        
        documentRepository = mock(DocumentRepository.class);
        
        uploadService = new PDFUploadServiceImpl(storageConfiguration, documentRepository);
    }
    
    @Test
    void uploadDocument_WithValidPDF_ReturnsDocumentUploadResponse() throws Exception {
        // Arrange
        MultipartFile validPDF = createValidPDF("test.pdf", 1024 * 1024); // 1MB
        when(documentRepository.save(any(Document.class))).thenAnswer(i -> i.getArgument(0));
        
        // Act
        DocumentUploadResponse response = uploadService.uploadDocument(validPDF);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getDocumentId()).isNotNull();
        assertThat(response.getFilename()).isEqualTo("test.pdf");
        assertThat(response.getStatus()).isEqualTo("uploaded");
        
        // Verify document was saved
        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(documentCaptor.capture());
        
        Document savedDocument = documentCaptor.getValue();
        assertThat(savedDocument.getDocumentId()).isEqualTo(response.getDocumentId());
        assertThat(savedDocument.getFilename()).isEqualTo("test.pdf");
        assertThat(savedDocument.getStatus()).isEqualTo(ProcessingStatus.UPLOADED);
    }
    
    @Test
    void uploadDocument_WithOversizedFile_ThrowsInvalidFileException() {
        // Arrange - Create a mock file that reports size > 50MB
        MultipartFile oversizedPDF = new MockMultipartFile(
            "file",
            "large.pdf",
            "application/pdf",
            new byte[1024] // Small actual content, but we'll override getSize()
        ) {
            @Override
            public long getSize() {
                return 51L * 1024 * 1024; // Report 51MB
            }
        };
        
        // Act & Assert
        assertThatThrownBy(() -> uploadService.uploadDocument(oversizedPDF))
            .isInstanceOf(InvalidFileException.class)
            .hasMessageContaining("validation failed");
    }
    
    @Test
    void uploadDocument_WithNonPDFFile_ThrowsInvalidFileException() {
        // Arrange
        MultipartFile nonPDF = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "This is not a PDF".getBytes()
        );
        
        // Act & Assert
        assertThatThrownBy(() -> uploadService.uploadDocument(nonPDF))
            .isInstanceOf(InvalidFileException.class);
    }
    
    @Test
    void validatePDF_WithValidPDF_ReturnsTrue() {
        // Arrange
        MultipartFile validPDF = createValidPDF("test.pdf", 1024);
        
        // Act
        boolean result = uploadService.validatePDF(validPDF);
        
        // Assert
        assertThat(result).isTrue();
    }
    
    @Test
    void validatePDF_WithEmptyFile_ReturnsFalse() {
        // Arrange
        MultipartFile emptyFile = new MockMultipartFile(
            "file",
            "empty.pdf",
            "application/pdf",
            new byte[0]
        );
        
        // Act
        boolean result = uploadService.validatePDF(emptyFile);
        
        // Assert
        assertThat(result).isFalse();
    }
    
    @Test
    void validatePDF_WithOversizedFile_ReturnsFalse() {
        // Arrange - Create a mock file that reports size > 50MB
        MultipartFile oversizedPDF = new MockMultipartFile(
            "file",
            "large.pdf",
            "application/pdf",
            new byte[1024]
        ) {
            @Override
            public long getSize() {
                return 51L * 1024 * 1024; // Report 51MB
            }
        };
        
        // Act
        boolean result = uploadService.validatePDF(oversizedPDF);
        
        // Assert
        assertThat(result).isFalse();
    }
    
    @Test
    void validatePDF_WithInvalidMimeType_ReturnsFalse() {
        // Arrange
        MultipartFile invalidMimeType = new MockMultipartFile(
            "file",
            "test.pdf",
            "text/plain",
            "Not a PDF".getBytes()
        );
        
        // Act
        boolean result = uploadService.validatePDF(invalidMimeType);
        
        // Assert
        assertThat(result).isFalse();
    }
    
    @Test
    void validatePDF_WithCorruptedPDF_ReturnsFalse() {
        // Arrange
        MultipartFile corruptedPDF = new MockMultipartFile(
            "file",
            "corrupted.pdf",
            "application/pdf",
            "This is not a valid PDF structure".getBytes()
        );
        
        // Act
        boolean result = uploadService.validatePDF(corruptedPDF);
        
        // Assert
        assertThat(result).isFalse();
    }
    
    @Test
    void validatePDF_WithFileAtSizeLimit_ReturnsTrue() {
        // Arrange - exactly 50MB
        MultipartFile atLimit = createValidPDF("limit.pdf", 50 * 1024 * 1024);
        
        // Act
        boolean result = uploadService.validatePDF(atLimit);
        
        // Assert
        assertThat(result).isTrue();
    }
    
    @Test
    void storeFile_WithValidFile_StoresFileSuccessfully() throws IOException {
        // Arrange
        MultipartFile validPDF = createValidPDF("test.pdf", 1024);
        String documentId = "test-doc-id";
        
        // Act
        Path storedPath = uploadService.storeFile(validPDF, documentId);
        
        // Assert
        assertThat(storedPath).exists();
        assertThat(storedPath.getFileName().toString()).isEqualTo(documentId + ".pdf");
        assertThat(Files.size(storedPath)).isGreaterThan(0);
    }
    
    @Test
    void storeFile_WithExistingFile_ReplacesFile() throws IOException {
        // Arrange
        MultipartFile firstPDF = createValidPDF("first.pdf", 1024);
        MultipartFile secondPDF = createValidPDF("second.pdf", 4096); // Significantly larger
        String documentId = "test-doc-id";
        
        // Act
        Path firstPath = uploadService.storeFile(firstPDF, documentId);
        long firstSize = Files.size(firstPath);
        
        Path secondPath = uploadService.storeFile(secondPDF, documentId);
        long secondSize = Files.size(secondPath);
        
        // Assert
        assertThat(firstPath).isEqualTo(secondPath);
        assertThat(secondSize).isGreaterThan(firstSize);
    }
    
    /**
     * Helper method to create a valid PDF file for testing.
     * 
     * @param filename the name of the file
     * @param targetSize approximate target size in bytes
     * @return MockMultipartFile containing a valid PDF
     */
    private MockMultipartFile createValidPDF(String filename, long targetSize) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            // Add pages with content to reach approximately the target size
            int pageCount = Math.max(1, (int)(targetSize / 1000)); // Rough estimate
            for (int i = 0; i < pageCount; i++) {
                PDPage page = new PDPage();
                document.addPage(page);
            }
            
            document.save(baos);
            byte[] pdfBytes = baos.toByteArray();
            
            return new MockMultipartFile(
                "file",
                filename,
                "application/pdf",
                pdfBytes
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to create test PDF", e);
        }
    }
}
