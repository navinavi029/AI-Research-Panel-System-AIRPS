# PDF Export Feature

## Overview
The AI Panelist System now supports exporting analysis results as professionally formatted PDF documents.

## New Endpoint

### Export Results as PDF
```
GET /api/documents/{documentId}/results/pdf
```

**Description**: Download the consensus analysis report as a formatted PDF document.

**Parameters**:
- `documentId` (path, required): Document ID returned from upload

**Response**:
- **200 OK**: PDF report generated successfully
  - Content-Type: `application/pdf`
  - Content-Disposition: `attachment; filename="analysis-report-{documentId}.pdf"`
- **404 Not Found**: Document not found
- **409 Conflict**: Document still processing
- **500 Internal Server Error**: PDF generation failed

## Usage Example

### Using curl (Windows PowerShell)
```powershell
curl "http://localhost:8080/api/documents/{documentId}/results/pdf" -UseBasicParsing -OutFile "report.pdf"
```

### Using curl (Linux/Mac)
```bash
curl "http://localhost:8080/api/documents/{documentId}/results/pdf" -o report.pdf
```

### Using Browser
Simply navigate to:
```
http://localhost:8080/api/documents/{documentId}/results/pdf
```

## PDF Report Contents

The generated PDF includes:

1. **Header Section**
   - Report title
   - Document ID
   - Report ID
   - Generation timestamp
   - Number of agents included

2. **Common Themes**
   - Themes identified across all agent analyses

3. **Agreements**
   - Points where all agents reached consensus

4. **Disagreements**
   - Areas where agents had differing perspectives

5. **Unified Recommendations**
   - Synthesized recommendations from all agents

6. **Attributed Insights**
   - Unique contributions from each specialized agent:
     - Lead Analyst
     - General Analyst
     - Methodology Reviewer
     - Literature Reviewer
     - Quick Screener
     - Fact Extractor

## Technical Implementation

### Components Added

1. **PDFExportService** (`src/main/java/com/aipanelist/export/PDFExportService.java`)
   - Interface for PDF generation

2. **PDFExportServiceImpl** (`src/main/java/com/aipanelist/export/PDFExportServiceImpl.java`)
   - Implementation using Apache PDFBox
   - Features:
     - Professional formatting with proper margins
     - Automatic page breaks
     - Text wrapping for long content
     - Section headings and body text styling

3. **PDFGenerationException** (`src/main/java/com/aipanelist/export/PDFGenerationException.java`)
   - Custom exception for PDF generation errors

4. **Updated DocumentController**
   - New endpoint: `GET /api/documents/{documentId}/results/pdf`
   - Integrated with existing results service

### Dependencies
- Apache PDFBox 3.0.1 (already included in pom.xml)

## Testing

### Test with Existing Document
```powershell
# Download PDF for the test document
curl "http://localhost:8080/api/documents/2ddd8d30-159f-4c64-b3c8-aae2e886ff86/results/pdf" -UseBasicParsing -OutFile "analysis_report.pdf"
```

### Verify PDF
- Check file size (should be ~12KB for typical report)
- Open in PDF reader to verify formatting
- Verify all sections are present and readable

## API Documentation

The new endpoint is automatically documented in:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Error Handling

The endpoint handles the following scenarios:
- **Document not found**: Returns 404 with error message
- **Document still processing**: Returns 409 with status message
- **PDF generation failure**: Returns 500 with error details
- **Unexpected errors**: Returns 500 with generic error message

All errors are logged for debugging purposes.

## Future Enhancements

Potential improvements:
1. Add individual agent reports to PDF (not just consensus)
2. Include charts and visualizations
3. Support custom PDF templates
4. Add watermarks or branding
5. Support multiple export formats (DOCX, HTML)
6. Add PDF encryption/password protection
7. Include document metadata and statistics
