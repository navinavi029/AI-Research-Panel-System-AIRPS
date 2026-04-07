package com.aipanelist.export;

import com.aipanelist.api.DetailedResultsDTO;
import com.aipanelist.model.ConsensusReport;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of PDF export service using Apache PDFBox.
 */
@Slf4j
@Service
public class PDFExportServiceImpl implements PDFExportService {
    
    private static final float MARGIN = 50;
    private static final float FONT_SIZE_TITLE = 22;
    private static final float FONT_SIZE_SUBTITLE = 12;
    private static final float FONT_SIZE_HEADING = 16;
    private static final float FONT_SIZE_SUBHEADING = 13;
    private static final float FONT_SIZE_BODY = 10;
    private static final float LINE_SPACING = 1.5f;
    
    // Colors
    private static final Color PRIMARY_COLOR = new Color(41, 98, 255); // Blue
    private static final Color SECONDARY_COLOR = new Color(100, 100, 100); // Gray
    private static final Color ACCENT_COLOR = new Color(245, 247, 250); // Light gray background
    private static final Color TEXT_COLOR = new Color(33, 33, 33); // Dark gray
    
    @Override
    public byte[] generatePDFReport(DetailedResultsDTO results) throws PDFGenerationException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            PDPage currentPage = new PDPage(PDRectangle.A4);
            document.addPage(currentPage);
            
            float yPosition = currentPage.getMediaBox().getHeight() - MARGIN;
            PDPageContentStream contentStream = new PDPageContentStream(document, currentPage);
            
            ConsensusReport consensus = results.getConsensusReport();
            
            // Header with colored background
            yPosition = drawHeader(contentStream, currentPage, yPosition);
            
            // Title
            yPosition = writeTitle(contentStream, "AI Panelist Analysis Report", yPosition);
            yPosition -= 10;
            
            // Subtitle
            contentStream.setNonStrokingColor(SECONDARY_COLOR);
            yPosition = writeText(contentStream, "Multi-Agent Consensus Report", FONT_SIZE_SUBTITLE, yPosition, 
                                new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE));
            contentStream.setNonStrokingColor(TEXT_COLOR);
            yPosition -= 25;
            
            // Metadata box
            yPosition = drawMetadataBox(contentStream, currentPage, consensus, results.getDocumentId(), yPosition);
            yPosition -= 30;
            
            contentStream.close();
            
            // Table of Contents
            yPosition = addTableOfContents(document, yPosition);
            yPosition -= 20;
            
            // Common Themes
            yPosition = addFormattedSection(document, "Common Themes", consensus.getCommonThemes(), 
                                          "Themes identified across all agent analyses", yPosition, "📊");
            
            // Agreements
            yPosition = addFormattedSection(document, "Agreements", consensus.getAgreements(), 
                                          "Points where all agents reached consensus", yPosition, "✓");
            
            // Disagreements
            yPosition = addFormattedSection(document, "Disagreements", consensus.getDisagreements(), 
                                          "Areas where agents had differing perspectives", yPosition, "⚠");
            
            // Unified Recommendations
            yPosition = addFormattedSection(document, "Unified Recommendations", 
                                          consensus.getUnifiedRecommendations(),
                                          "Synthesized recommendations from all agents", yPosition, "💡");
            
            // Attributed Insights
            yPosition = addFormattedSection(document, "Attributed Insights", 
                                          consensus.getAttributedInsights(),
                                          "Unique contributions from each specialized agent", yPosition, "👥");
            
            // Footer on last page
            addFooter(document);
            
            document.save(outputStream);
            log.info("PDF report generated successfully for document {}", results.getDocumentId());
            
            return outputStream.toByteArray();
            
        } catch (IOException e) {
            log.error("Failed to generate PDF report", e);
            throw new PDFGenerationException("Failed to generate PDF report", e);
        }
    }
    
    private float drawHeader(PDPageContentStream contentStream, PDPage page, float yPosition) 
            throws IOException {
        float pageWidth = page.getMediaBox().getWidth();
        float headerHeight = 60;
        
        // Draw colored header rectangle
        contentStream.setNonStrokingColor(PRIMARY_COLOR);
        contentStream.addRect(0, yPosition - headerHeight, pageWidth, headerHeight);
        contentStream.fill();
        
        contentStream.setNonStrokingColor(Color.WHITE);
        return yPosition - 20;
    }
    
    private float drawMetadataBox(PDPageContentStream contentStream, PDPage page, 
                                  ConsensusReport consensus, String documentId, float yPosition) 
            throws IOException {
        float pageWidth = page.getMediaBox().getWidth() - (2 * MARGIN);
        float boxHeight = 80;
        float boxY = yPosition - boxHeight;
        
        // Draw background box
        contentStream.setNonStrokingColor(ACCENT_COLOR);
        contentStream.addRect(MARGIN, boxY, pageWidth, boxHeight);
        contentStream.fill();
        
        // Draw border
        contentStream.setStrokingColor(PRIMARY_COLOR);
        contentStream.setLineWidth(2);
        contentStream.addRect(MARGIN, boxY, pageWidth, boxHeight);
        contentStream.stroke();
        
        // Reset color for text
        contentStream.setNonStrokingColor(TEXT_COLOR);
        
        float textY = yPosition - 20;
        PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        
        // Document ID
        contentStream.beginText();
        contentStream.setFont(boldFont, FONT_SIZE_BODY);
        contentStream.newLineAtOffset(MARGIN + 10, textY);
        contentStream.showText("Document ID: ");
        contentStream.setFont(regularFont, FONT_SIZE_BODY);
        contentStream.showText(documentId);
        contentStream.endText();
        textY -= 15;
        
        // Report ID
        contentStream.beginText();
        contentStream.setFont(boldFont, FONT_SIZE_BODY);
        contentStream.newLineAtOffset(MARGIN + 10, textY);
        contentStream.showText("Report ID: ");
        contentStream.setFont(regularFont, FONT_SIZE_BODY);
        contentStream.showText(consensus.getReportId());
        contentStream.endText();
        textY -= 15;
        
        // Generated date
        if (consensus.getGeneratedAt() != null) {
            String formattedDate = DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm")
                .format(consensus.getGeneratedAt());
            contentStream.beginText();
            contentStream.setFont(boldFont, FONT_SIZE_BODY);
            contentStream.newLineAtOffset(MARGIN + 10, textY);
            contentStream.showText("Generated: ");
            contentStream.setFont(regularFont, FONT_SIZE_BODY);
            contentStream.showText(formattedDate);
            contentStream.endText();
            textY -= 15;
        }
        
        // Agents included
        contentStream.beginText();
        contentStream.setFont(boldFont, FONT_SIZE_BODY);
        contentStream.newLineAtOffset(MARGIN + 10, textY);
        contentStream.showText("AI Agents: ");
        contentStream.setFont(regularFont, FONT_SIZE_BODY);
        contentStream.showText(consensus.getAgentReportsIncluded() + " specialized agents");
        contentStream.endText();
        
        return boxY - 10;
    }
    
    private float addTableOfContents(PDDocument document, float yPosition) throws IOException {
        PDPage currentPage = document.getPage(document.getNumberOfPages() - 1);
        
        if (yPosition < MARGIN + 150) {
            currentPage = new PDPage(PDRectangle.A4);
            document.addPage(currentPage);
            yPosition = currentPage.getMediaBox().getHeight() - MARGIN;
        }
        
        PDPageContentStream contentStream = new PDPageContentStream(document, currentPage, 
                                                                    PDPageContentStream.AppendMode.APPEND, true);
        
        // Section title
        yPosition = writeSectionHeading(contentStream, "Table of Contents", yPosition);
        yPosition -= 15;
        
        // TOC items
        String[] sections = {
            "1. Common Themes",
            "2. Agreements",
            "3. Disagreements",
            "4. Unified Recommendations",
            "5. Attributed Insights"
        };
        
        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        for (String section : sections) {
            contentStream.beginText();
            contentStream.setFont(font, FONT_SIZE_BODY);
            contentStream.newLineAtOffset(MARGIN + 20, yPosition);
            contentStream.showText(section);
            contentStream.endText();
            yPosition -= 18;
        }
        
        contentStream.close();
        return yPosition;
    }
    
    private float addFormattedSection(PDDocument document, String heading, String content, 
                                     String description, float yPosition, String icon) throws IOException {
        
        PDPage currentPage = document.getPage(document.getNumberOfPages() - 1);
        
        // Check if we need a new page for the heading
        if (yPosition < MARGIN + 150) {
            currentPage = new PDPage(PDRectangle.A4);
            document.addPage(currentPage);
            yPosition = currentPage.getMediaBox().getHeight() - MARGIN;
        }
        
        PDPageContentStream contentStream = new PDPageContentStream(document, currentPage, 
                                                                    PDPageContentStream.AppendMode.APPEND, true);
        
        // Draw section separator line
        contentStream.setStrokingColor(PRIMARY_COLOR);
        contentStream.setLineWidth(1);
        contentStream.moveTo(MARGIN, yPosition);
        contentStream.lineTo(currentPage.getMediaBox().getWidth() - MARGIN, yPosition);
        contentStream.stroke();
        yPosition -= 20;
        
        // Write heading with icon (simplified - just text)
        yPosition = writeSectionHeading(contentStream, heading, yPosition);
        yPosition -= 5;
        
        // Write description
        contentStream.setNonStrokingColor(SECONDARY_COLOR);
        yPosition = writeText(contentStream, description, FONT_SIZE_BODY - 1, yPosition,
                            new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE));
        contentStream.setNonStrokingColor(TEXT_COLOR);
        yPosition -= 15;
        
        contentStream.close();
        
        // Write content with better formatting
        yPosition = writeFormattedContent(document, content, yPosition);
        yPosition -= 25;
        
        return yPosition;
    }
    
    private float writeFormattedContent(PDDocument document, String text, float yPosition) throws IOException {
        if (text == null || text.isEmpty()) {
            return yPosition;
        }
        
        PDPage currentPage = document.getPage(document.getNumberOfPages() - 1);
        float pageWidth = currentPage.getMediaBox().getWidth() - (2 * MARGIN) - 20;
        
        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        
        // Split into paragraphs and numbered items
        String[] paragraphs = text.split("\n");
        
        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                yPosition -= 10;
                continue;
            }
            
            // Check if it's a numbered item
            boolean isNumberedItem = paragraph.trim().matches("^\\d+\\..*");
            float leftMargin = isNumberedItem ? MARGIN + 10 : MARGIN + 20;
            float availableWidth = pageWidth - (isNumberedItem ? 10 : 20);
            
            List<String> lines = wrapText(paragraph, font, FONT_SIZE_BODY, availableWidth);
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                
                // Check if we need a new page
                if (yPosition < MARGIN + 30) {
                    currentPage = new PDPage(PDRectangle.A4);
                    document.addPage(currentPage);
                    yPosition = currentPage.getMediaBox().getHeight() - MARGIN;
                }
                
                PDPageContentStream contentStream = new PDPageContentStream(document, currentPage, 
                                                                            PDPageContentStream.AppendMode.APPEND, true);
                
                // Highlight numbered items
                if (i == 0 && isNumberedItem) {
                    contentStream.setNonStrokingColor(PRIMARY_COLOR);
                    PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                    yPosition = writeText(contentStream, line, FONT_SIZE_BODY, yPosition, boldFont);
                    contentStream.setNonStrokingColor(TEXT_COLOR);
                } else {
                    yPosition = writeText(contentStream, line, FONT_SIZE_BODY, yPosition, font);
                }
                
                contentStream.close();
            }
            
            yPosition -= 8; // Space between paragraphs
        }
        
        return yPosition;
    }
    
    private void addFooter(PDDocument document) throws IOException {
        int totalPages = document.getNumberOfPages();
        
        for (int i = 0; i < totalPages; i++) {
            PDPage page = document.getPage(i);
            PDPageContentStream contentStream = new PDPageContentStream(document, page, 
                                                                        PDPageContentStream.AppendMode.APPEND, true);
            
            float pageWidth = page.getMediaBox().getWidth();
            float footerY = 30;
            
            // Draw footer line
            contentStream.setStrokingColor(SECONDARY_COLOR);
            contentStream.setLineWidth(0.5f);
            contentStream.moveTo(MARGIN, footerY + 10);
            contentStream.lineTo(pageWidth - MARGIN, footerY + 10);
            contentStream.stroke();
            
            // Page number
            contentStream.setNonStrokingColor(SECONDARY_COLOR);
            String pageText = "Page " + (i + 1) + " of " + totalPages;
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float textWidth = font.getStringWidth(pageText) / 1000 * 8;
            
            contentStream.beginText();
            contentStream.setFont(font, 8);
            contentStream.newLineAtOffset((pageWidth - textWidth) / 2, footerY);
            contentStream.showText(pageText);
            contentStream.endText();
            
            // Footer text
            String footerText = "AI Panelist System - Powered by NVIDIA AI";
            float footerTextWidth = font.getStringWidth(footerText) / 1000 * 8;
            
            contentStream.beginText();
            contentStream.setFont(font, 8);
            contentStream.newLineAtOffset(pageWidth - MARGIN - footerTextWidth, footerY);
            contentStream.showText(footerText);
            contentStream.endText();
            
            contentStream.close();
        }
    }
    
    private float writeTitle(PDPageContentStream contentStream, String text, float yPosition) 
            throws IOException {
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_TITLE);
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText(text);
        contentStream.endText();
        return yPosition - (FONT_SIZE_TITLE * LINE_SPACING);
    }
    
    private float writeSectionHeading(PDPageContentStream contentStream, String text, float yPosition) 
            throws IOException {
        contentStream.setNonStrokingColor(PRIMARY_COLOR);
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_HEADING);
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText(text);
        contentStream.endText();
        contentStream.setNonStrokingColor(TEXT_COLOR);
        return yPosition - (FONT_SIZE_HEADING * LINE_SPACING);
    }
    
    private float writeText(PDPageContentStream contentStream, String text, float fontSize, 
                          float yPosition, PDType1Font font) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText(text);
        contentStream.endText();
        return yPosition - (fontSize * LINE_SPACING);
    }
    
    private float writeText(PDPageContentStream contentStream, String text, float fontSize, float yPosition) 
            throws IOException {
        return writeText(contentStream, text, fontSize, yPosition, 
                       new PDType1Font(Standard14Fonts.FontName.HELVETICA));
    }
    
    private float writeWrappedText(PDDocument document, String text, float yPosition) throws IOException {
        if (text == null || text.isEmpty()) {
            return yPosition;
        }
        
        PDPage currentPage = document.getPage(document.getNumberOfPages() - 1);
        float pageWidth = currentPage.getMediaBox().getWidth() - (2 * MARGIN);
        
        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        
        // Split text into lines that fit the page width
        List<String> lines = wrapText(text, font, FONT_SIZE_BODY, pageWidth);
        
        for (String line : lines) {
            // Check if we need a new page
            if (yPosition < MARGIN + 20) {
                currentPage = new PDPage(PDRectangle.A4);
                document.addPage(currentPage);
                yPosition = currentPage.getMediaBox().getHeight() - MARGIN;
            }
            
            PDPageContentStream contentStream = new PDPageContentStream(document, currentPage, 
                                                                        PDPageContentStream.AppendMode.APPEND, true);
            yPosition = writeText(contentStream, line, FONT_SIZE_BODY, yPosition);
            contentStream.close();
        }
        
        return yPosition;
    }
    
    private List<String> wrapText(String text, PDType1Font font, float fontSize, float maxWidth) 
            throws IOException {
        List<String> lines = new ArrayList<>();
        
        String[] paragraphs = text.split("\n");
        
        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                lines.add("");
                continue;
            }
            
            String[] words = paragraph.split(" ");
            StringBuilder currentLine = new StringBuilder();
            
            for (String word : words) {
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                float width = font.getStringWidth(testLine) / 1000 * fontSize;
                
                if (width > maxWidth && currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine = new StringBuilder(testLine);
                }
            }
            
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
        }
        
        return lines;
    }
}
