# PDF Export

The PDF report is generated using iText7 and contains a professionally styled multi-page document based on the consensus report.

---

## Accessing the PDF

```bash
curl -OJ http://localhost:8080/api/documents/{documentId}/results/pdf
```

The file is returned as `analysis-report-{documentId}.pdf`.

The document must be in `COMPLETE` status. If still processing, the endpoint returns HTTP 409.

---

## Report Structure

| Page(s) | Content |
|---------|---------|
| 1 | Cover page |
| 2 | Table of contents |
| 3+ | Content sections (one per consensus section) |
| All | Page footers |

### Cover Page

- Header banner in primary blue
- Title: **"Multi-Agent Analysis Report"**
- Subtitle: *"Consensus Report from AI Expert Panel"*
- Metadata table:
  - Document ID
  - Report ID
  - Generated timestamp (formatted as "April 26, 2026 at 09:15")
  - Number of AI agents included
- Contributing agent list (all 6 agents)

### Table of Contents

Lists all 5 content sections:
1. Common Themes
2. Agreements
3. Disagreements
4. Unified Recommendations
5. Attributed Insights

### Content Sections

Each section has:
- **Colored header bar** with section title in white text
- **Description subtitle** in italic gray
- **Content box** with light gray background and border
- Numbered items highlighted in the section's accent color
- Line separator at the bottom

### Page Footers

Every page includes:
- Horizontal separator line
- Page number centered: "Page X of Y"
- Footer text right-aligned: "AI Panelist System • Powered by NVIDIA AI"

---

## Color Scheme

| Color | Hex | Used for |
|-------|-----|---------|
| Primary | `#2962FF` | Cover banner, Common Themes header, Recommendations header |
| Success | `#10B981` | Agreements header |
| Warning | `#F59E0B` | Disagreements header |
| Secondary | `#646464` | Attributed Insights header, subtitles, footers |
| Accent | `#F5F7FA` | Content box backgrounds |
| Light Gray | `#E5E7EB` | Content box borders |
| Text | `#212121` | Body text |

---

## Technical Details

**Library:** iText7 (`com.itextpdf:itext7-core:7.2.5`)

**Page size:** A4

**Margins:** 40pt top/right/left, 60pt bottom (to accommodate footer)

**Immediate flush disabled:** `new Document(pdfDoc, PageSize.A4, false)` — keeps all pages in memory until `document.close()` so the footer pass can write to every page without triggering a "Cannot draw elements on already flushed pages" error.

**Smart mode enabled:** `writer.setSmartMode(true)` — deduplicates repeated content streams for smaller file size.
