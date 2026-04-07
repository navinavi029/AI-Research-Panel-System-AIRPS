package com.aipanelist.export;

import com.aipanelist.api.DetailedResultsDTO;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

/**
 * Service for exporting analysis results to PDF format.
 */
@Service
public interface PDFExportService {
    
    /**
     * Generates a PDF report from the analysis results.
     *
     * @param results the detailed analysis results
     * @return byte array containing the PDF document
     * @throws PDFGenerationException if PDF generation fails
     */
    byte[] generatePDFReport(DetailedResultsDTO results) throws PDFGenerationException;
}
