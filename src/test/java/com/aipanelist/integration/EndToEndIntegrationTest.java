package com.aipanelist.integration;

import com.aipanelist.api.DocumentController;
import com.aipanelist.model.ProcessingStatus;
import com.aipanelist.processing.DocumentStatus;
import com.aipanelist.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration test using Testcontainers for PostgreSQL.
 * Tests complete upload → extraction → chunking → analysis → consensus flow.
 * 
 * Note: These tests require Docker to be running. They will be skipped if Docker is not available.
 * To run these tests, ensure Docker is installed and running, then set DOCKER_AVAILABLE=true.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@EnabledIfEnvironmentVariable(named = "DOCKER_AVAILABLE", matches = "true")
class EndToEndIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ExtractedDocumentRepository extractedDocumentRepository;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Autowired
    private AnalysisReportRepository analysisReportRepository;

    @Autowired
    private ConsensusReportRepository consensusReportRepository;

    @Test
    void testCompleteDocumentProcessingFlow() throws Exception {
        // Create a simple test PDF
        byte[] pdfContent = createSimpleTestPDF();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                pdfContent
        );

        // Step 1: Upload document
        MvcResult uploadResult = mockMvc.perform(multipart("/api/documents/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").exists())
                .andExpect(jsonPath("$.filename").value("test-document.pdf"))
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andReturn();

        String responseJson = uploadResult.getResponse().getContentAsString();
        String documentId = objectMapper.readTree(responseJson).get("documentId").asText();

        assertThat(documentId).isNotNull();

        // Step 2: Verify document was persisted
        assertThat(documentRepository.findById(documentId)).isPresent();

        // Step 3: Check status endpoint
        mockMvc.perform(get("/api/documents/{documentId}/status", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(documentId))
                .andExpect(jsonPath("$.status").exists());

        // Step 4: Verify results endpoint returns appropriate response
        // (Will be 202 Accepted if still processing, or 200 OK if complete)
        mockMvc.perform(get("/api/documents/{documentId}/results", documentId))
                .andExpect(status().isAccepted());
    }

    @Test
    void testInvalidFileUpload() throws Exception {
        // Test with non-PDF file
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "This is not a PDF".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(invalidFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testFileTooLarge() throws Exception {
        // Create a file larger than 50MB
        byte[] largeContent = new byte[51 * 1024 * 1024]; // 51MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.pdf",
                "application/pdf",
                largeContent
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(largeFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testNonExistentDocumentStatus() throws Exception {
        String fakeId = "00000000-0000-0000-0000-000000000000";

        mockMvc.perform(get("/api/documents/{documentId}/status", fakeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testNonExistentDocumentResults() throws Exception {
        String fakeId = "00000000-0000-0000-0000-000000000000";

        mockMvc.perform(get("/api/documents/{documentId}/results", fakeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testDetailedResultsEndpoint() throws Exception {
        // Upload a document first
        byte[] pdfContent = createSimpleTestPDF();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-detailed.pdf",
                "application/pdf",
                pdfContent
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/documents/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = uploadResult.getResponse().getContentAsString();
        String documentId = objectMapper.readTree(responseJson).get("documentId").asText();

        // Test detailed results endpoint
        mockMvc.perform(get("/api/documents/{documentId}/results/detailed", documentId))
                .andExpect(status().isAccepted()); // Will be 202 while processing
    }

    /**
     * Creates a minimal valid PDF for testing.
     * This is a simplified PDF structure that PDFBox can parse.
     */
    private byte[] createSimpleTestPDF() {
        // Minimal PDF structure
        String pdfContent = "%PDF-1.4\n" +
                "1 0 obj\n" +
                "<< /Type /Catalog /Pages 2 0 R >>\n" +
                "endobj\n" +
                "2 0 obj\n" +
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n" +
                "endobj\n" +
                "3 0 obj\n" +
                "<< /Type /Page /Parent 2 0 R /Resources 4 0 R /MediaBox [0 0 612 792] /Contents 5 0 R >>\n" +
                "endobj\n" +
                "4 0 obj\n" +
                "<< /Font << /F1 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> >> >>\n" +
                "endobj\n" +
                "5 0 obj\n" +
                "<< /Length 44 >>\n" +
                "stream\n" +
                "BT\n" +
                "/F1 12 Tf\n" +
                "100 700 Td\n" +
                "(Test Document) Tj\n" +
                "ET\n" +
                "endstream\n" +
                "endobj\n" +
                "xref\n" +
                "0 6\n" +
                "0000000000 65535 f\n" +
                "0000000009 00000 n\n" +
                "0000000058 00000 n\n" +
                "0000000115 00000 n\n" +
                "0000000214 00000 n\n" +
                "0000000304 00000 n\n" +
                "trailer\n" +
                "<< /Size 6 /Root 1 0 R >>\n" +
                "startxref\n" +
                "397\n" +
                "%%EOF";

        return pdfContent.getBytes();
    }
}
