package com.aipanelist.export;

import com.aipanelist.api.DetailedResultsDTO;
import com.aipanelist.model.ConsensusReport;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.List;
import com.itextpdf.layout.element.ListItem;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Advanced PDF export service with professional styling using iText.
 */
@Slf4j
@Service
@Primary
public class StyledPDFExportServiceImpl implements PDFExportService {
    
    // Color scheme
    private static final Color PRIMARY_COLOR = new DeviceRgb(41, 98, 255);
    private static final Color SECONDARY_COLOR = new DeviceRgb(100, 100, 100);
    private static final Color ACCENT_COLOR = new DeviceRgb(245, 247, 250);
    private static final Color SUCCESS_COLOR = new DeviceRgb(16, 185, 129);
    private static final Color WARNING_COLOR = new DeviceRgb(245, 158, 11);
    private static final Color TEXT_COLOR = new DeviceRgb(33, 33, 33);
    private static final Color LIGHT_GRAY = new DeviceRgb(229, 231, 235);
    
    @Override
    public byte[] generatePDFReport(DetailedResultsDTO results) throws PDFGenerationException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(40, 40, 60, 40);
            
            ConsensusReport consensus = results.getConsensusReport();
            
            // Add cover page
            addCoverPage(document, consensus, results.getDocumentId());
            document.add(new AreaBreak());
            
            // Add table of contents
            addTableOfContents(document);
            document.add(new AreaBreak());
            
            // Add sections
            addStyledSection(document, "Common Themes", consensus.getCommonThemes(),
                           "Themes identified across all agent analyses", PRIMARY_COLOR, "📊");
            
            addStyledSection(document, "Agreements", consensus.getAgreements(),
                           "Points where all agents reached consensus", SUCCESS_COLOR, "✓");
            
            addStyledSection(document, "Disagreements", consensus.getDisagreements(),
                           "Areas where agents had differing perspectives", WARNING_COLOR, "⚠");
            
            addStyledSection(document, "Unified Recommendations", consensus.getUnifiedRecommendations(),
                           "Synthesized recommendations from all agents", PRIMARY_COLOR, "💡");
            
            addStyledSection(document, "Attributed Insights", consensus.getAttributedInsights(),
                           "Unique contributions from each specialized agent", SECONDARY_COLOR, "👥");
            
            // Add footer to all pages
            addPageFooters(pdfDoc);
            
            document.close();
            log.info("Styled PDF report generated successfully for document {}", results.getDocumentId());
            
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            log.error("Failed to generate styled PDF report", e);
            throw new PDFGenerationException("Failed to generate styled PDF report", e);
        }
    }
    
    private void addCoverPage(Document document, ConsensusReport consensus, String documentId) {
        // Header banner
        Table headerTable = new Table(1);
        headerTable.setWidth(UnitValue.createPercentValue(100));
        headerTable.setBackgroundColor(PRIMARY_COLOR);
        
        Cell headerCell = new Cell()
            .add(new Paragraph("AI PANELIST SYSTEM")
                .setFontSize(14)
                .setBold()
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER))
            .setBorder(Border.NO_BORDER)
            .setPadding(20);
        
        headerTable.addCell(headerCell);
        document.add(headerTable);
        
        // Spacer
        document.add(new Paragraph("\n\n"));
        
        // Main title
        Paragraph title = new Paragraph("Multi-Agent Analysis Report")
            .setFontSize(32)
            .setBold()
            .setFontColor(PRIMARY_COLOR)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(40);
        document.add(title);
        
        // Subtitle
        Paragraph subtitle = new Paragraph("Consensus Report from AI Expert Panel")
            .setFontSize(16)
            .setItalic()
            .setFontColor(SECONDARY_COLOR)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(60);
        document.add(subtitle);
        
        // Metadata box
        Table metadataTable = new Table(2);
        metadataTable.setWidth(UnitValue.createPercentValue(80));
        metadataTable.setHorizontalAlignment(HorizontalAlignment.CENTER);
        metadataTable.setBackgroundColor(ACCENT_COLOR);
        metadataTable.setBorder(new SolidBorder(PRIMARY_COLOR, 2));
        metadataTable.setMarginTop(40);
        
        addMetadataRow(metadataTable, "Document ID", documentId);
        addMetadataRow(metadataTable, "Report ID", consensus.getReportId());
        
        if (consensus.getGeneratedAt() != null) {
            String formattedDate = DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm")
                .format(consensus.getGeneratedAt());
            addMetadataRow(metadataTable, "Generated", formattedDate);
        }
        
        addMetadataRow(metadataTable, "AI Agents", consensus.getAgentReportsIncluded() + " specialized agents");
        
        document.add(metadataTable);
        
        // Agent list
        document.add(new Paragraph("\n\n"));
        Paragraph agentTitle = new Paragraph("Contributing AI Agents")
            .setFontSize(14)
            .setBold()
            .setFontColor(PRIMARY_COLOR)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(40);
        document.add(agentTitle);
        
        String[] agents = {
            "Lead Analyst", "General Analyst", "Methodology Reviewer",
            "Literature Reviewer", "Quick Screener", "Fact Extractor"
        };
        
        List agentList = new List()
            .setSymbolIndent(20)
            .setListSymbol("•")
            .setFontSize(11);
        
        for (String agent : agents) {
            ListItem item = new ListItem(agent);
            item.setFontColor(TEXT_COLOR);
            agentList.add(item);
        }
        
        Table agentTable = new Table(1);
        agentTable.setWidth(UnitValue.createPercentValue(60));
        agentTable.setHorizontalAlignment(HorizontalAlignment.CENTER);
        agentTable.addCell(new Cell()
            .add(agentList)
            .setBorder(Border.NO_BORDER));
        
        document.add(agentTable);
    }
    
    private void addMetadataRow(Table table, String label, String value) {
        table.addCell(new Cell()
            .add(new Paragraph(label).setBold().setFontSize(10))
            .setBorder(Border.NO_BORDER)
            .setPadding(8)
            .setBackgroundColor(ACCENT_COLOR));
        
        table.addCell(new Cell()
            .add(new Paragraph(value).setFontSize(10))
            .setBorder(Border.NO_BORDER)
            .setPadding(8)
            .setBackgroundColor(ACCENT_COLOR));
    }
    
    private void addTableOfContents(Document document) {
        Paragraph tocTitle = new Paragraph("Table of Contents")
            .setFontSize(24)
            .setBold()
            .setFontColor(PRIMARY_COLOR)
            .setMarginBottom(20);
        document.add(tocTitle);
        
        String[] sections = {
            "1. Common Themes",
            "2. Agreements",
            "3. Disagreements",
            "4. Unified Recommendations",
            "5. Attributed Insights"
        };
        
        for (String section : sections) {
            Paragraph tocItem = new Paragraph(section)
                .setFontSize(12)
                .setMarginLeft(20)
                .setMarginBottom(10);
            document.add(tocItem);
        }
    }
    
    private void addStyledSection(Document document, String title, String content,
                                 String description, Color accentColor, String icon) {
        // Section header with colored bar
        Table headerTable = new Table(1);
        headerTable.setWidth(UnitValue.createPercentValue(100));
        headerTable.setMarginTop(20);
        headerTable.setMarginBottom(15);
        
        Cell headerCell = new Cell()
            .add(new Paragraph(title)
                .setFontSize(18)
                .setBold()
                .setFontColor(ColorConstants.WHITE))
            .setBackgroundColor(accentColor)
            .setBorder(Border.NO_BORDER)
            .setPadding(12);
        
        headerTable.addCell(headerCell);
        document.add(headerTable);
        
        // Description
        Paragraph desc = new Paragraph(description)
            .setFontSize(10)
            .setItalic()
            .setFontColor(SECONDARY_COLOR)
            .setMarginBottom(15);
        document.add(desc);
        
        // Content box
        if (content != null && !content.isEmpty()) {
            Div contentBox = new Div()
                .setBackgroundColor(ACCENT_COLOR)
                .setBorder(new SolidBorder(LIGHT_GRAY, 1))
                .setPadding(15)
                .setMarginBottom(20);
            
            // Parse and format content
            String[] paragraphs = content.split("\n");
            for (String para : paragraphs) {
                if (para.trim().isEmpty()) {
                    continue;
                }
                
                Paragraph p = new Paragraph(para)
                    .setFontSize(10)
                    .setMarginBottom(8);
                
                // Highlight numbered items
                if (para.trim().matches("^\\d+\\..*")) {
                    p.setBold().setFontColor(accentColor);
                }
                
                contentBox.add(p);
            }
            
            document.add(contentBox);
        }
        
        // Section separator
        LineSeparator separator = new LineSeparator(new SolidLine(0.5f));
        separator.setMarginTop(10);
        separator.setMarginBottom(10);
        document.add(separator);
    }
    
    private void addPageFooters(PdfDocument pdfDoc) {
        int totalPages = pdfDoc.getNumberOfPages();
        
        for (int i = 1; i <= totalPages; i++) {
            Document footerDoc = new Document(pdfDoc);
            
            // Footer line
            LineSeparator line = new LineSeparator(new SolidLine(0.5f));
            line.setFixedPosition(i, 40, 30, 515);
            footerDoc.add(line);
            
            // Page number
            Paragraph pageNum = new Paragraph("Page " + i + " of " + totalPages)
                .setFontSize(8)
                .setFontColor(SECONDARY_COLOR)
                .setFixedPosition(i, 250, 20, 100)
                .setTextAlignment(TextAlignment.CENTER);
            footerDoc.add(pageNum);
            
            // Footer text
            Paragraph footerText = new Paragraph("AI Panelist System • Powered by NVIDIA AI")
                .setFontSize(8)
                .setFontColor(SECONDARY_COLOR)
                .setFixedPosition(i, 350, 20, 200)
                .setTextAlignment(TextAlignment.RIGHT);
            footerDoc.add(footerText);
            
            footerDoc.close();
        }
    }
}
