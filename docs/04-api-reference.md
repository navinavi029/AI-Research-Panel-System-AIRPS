# API Reference

**Base URL:** `http://localhost:8080/api/documents`

**Interactive docs:** `http://localhost:8080/swagger-ui.html`

All responses are JSON. All error responses follow the [standard error format](#error-response-format).

---

## Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/upload` | Upload a PDF for analysis |
| `GET` | `/{id}/status` | Poll processing status |
| `GET` | `/{id}/results` | Get consensus report (JSON) |
| `GET` | `/{id}/results/detailed` | Get all 6 agent reports + consensus |
| `GET` | `/{id}/results/pdf` | Download report as PDF |

---

## POST `/upload`

Upload a PDF document. Processing starts immediately in the background.

### Request

`Content-Type: multipart/form-data`

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `file` | File | Yes | PDF only · max 50MB · max 500 pages |

**Validation steps performed:**
1. File must not be empty
2. MIME type must be `application/pdf`
3. File size must not exceed 50MB (52,428,800 bytes)
4. PDF structure validated with Apache PDFBox — rejects corrupted or password-protected files

### Response `200 OK`

```json
{
  "documentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "filename": "research-paper.pdf",
  "status": "uploaded"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `documentId` | string (UUID) | Use this to poll status and retrieve results |
| `filename` | string | Original filename as uploaded |
| `status` | string | Always `"uploaded"` on success |

### Error Responses

| HTTP | Code | Cause |
|------|------|-------|
| 400 | `INVALID_FILE` | File is empty or missing |
| 400 | `INVALID_FILE_TYPE` | MIME type is not `application/pdf` |
| 400 | `INVALID_PDF` | File is corrupted, password-protected, or not a valid PDF |
| 413 | `FILE_TOO_LARGE` | File exceeds 50MB |
| 500 | `UPLOAD_FAILED` | Server-side storage error |

---

## GET `/{documentId}/status`

Poll the processing status of a document. Call this repeatedly until `status` is `COMPLETE` or `FAILED`.

### Response `200 OK`

```json
{
  "documentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "ANALYZING",
  "agentProgress": [
    {
      "agentId": "550e8400-e29b-41d4-a716-446655440000",
      "agentType": "LEAD_ANALYST",
      "chunksCompleted": 2,
      "totalChunks": 3
    },
    {
      "agentId": "550e8400-e29b-41d4-a716-446655440001",
      "agentType": "GENERAL_ANALYST",
      "chunksCompleted": 3,
      "totalChunks": 3
    },
    {
      "agentId": "550e8400-e29b-41d4-a716-446655440002",
      "agentType": "METHODOLOGY_REVIEWER",
      "chunksCompleted": 1,
      "totalChunks": 3
    }
  ]
}
```

### Status Values

| Status | Meaning |
|--------|---------|
| `UPLOADED` | File received and stored, queued for processing |
| `PROCESSING` | Text extraction from PDF in progress |
| `ANALYZING` | 6 AI agents analyzing in parallel |
| `DELIBERATING` | Consensus engine synthesizing agent reports |
| `COMPLETE` | Analysis complete — results ready |
| `FAILED` | Processing failed — check `errorMessage` field |

### Error Responses

| HTTP | Code | Cause |
|------|------|-------|
| 404 | `DOCUMENT_NOT_FOUND` | No document with that ID exists |
| 500 | `STATUS_RETRIEVAL_FAILED` | Server error |

---

## GET `/{documentId}/results`

Get the consensus report as JSON. If the document is still processing, returns the current `DocumentStatus` object with HTTP 200.

### Response `200 OK` — Complete

```json
{
  "reportId": "consensus-7f3a9b2c-1234-5678-abcd-ef0123456789",
  "documentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "commonThemes": "Both the Lead Analyst and Methodology Reviewer identified a consistent theme of...",
  "agreements": "All six agents agreed that the sample size of 450 participants was adequate for...",
  "disagreements": "The Literature Reviewer noted significant gaps in citation coverage of post-2020 work, while the General Analyst considered the literature review sufficient for the scope...",
  "unifiedRecommendations": "1. Extend the follow-up period from 6 to 12 months to strengthen longitudinal validity.\n2. Include a control group to improve causal inference...",
  "attributedInsights": "Lead Analyst: The research design demonstrates strong internal validity through rigorous randomization...\nMethodology Reviewer: The statistical power analysis is well-justified...",
  "generatedAt": "2026-04-26T09:15:32",
  "agentReportsIncluded": 6
}
```

| Field | Type | Description |
|-------|------|-------------|
| `reportId` | string | Unique ID of this consensus report |
| `documentId` | string | The document this report belongs to |
| `commonThemes` | string | Themes identified across multiple agent analyses |
| `agreements` | string | Points where agents reached consensus |
| `disagreements` | string | Areas where agents had differing perspectives |
| `unifiedRecommendations` | string | Synthesized recommendations from the full panel |
| `attributedInsights` | string | Unique contributions attributed to specific agents |
| `generatedAt` | ISO 8601 datetime | When the consensus was generated |
| `agentReportsIncluded` | integer | Number of agent reports used (max 6) |

### Error Responses

| HTTP | Code | Cause |
|------|------|-------|
| 404 | `DOCUMENT_NOT_FOUND` | No document with that ID exists |
| 500 | `RESULTS_RETRIEVAL_FAILED` | Server error |

---

## GET `/{documentId}/results/detailed`

Get all 6 individual agent reports plus the consensus report in a single response. Returns current `DocumentStatus` if still processing.

### Response `200 OK`

```json
{
  "documentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "agentReports": [
    {
      "reportId": "report-uuid-1",
      "documentId": "a1b2c3d4-...",
      "agentId": "agent-uuid-1",
      "agentType": "LEAD_ANALYST",
      "keyFindings": "The study demonstrates strong internal validity through rigorous randomization...",
      "strengths": "1. Well-defined research question with clear hypotheses\n2. Appropriate sample size with power analysis justification\n3. Robust randomization procedure...",
      "weaknesses": "1. Limited external validity due to single-site recruitment\n2. Short 6-month follow-up period\n3. Self-reported outcome measures introduce potential bias...",
      "recommendations": "1. Extend follow-up to 12 months\n2. Include multi-site recruitment\n3. Add objective outcome measures...",
      "completedAt": "2026-04-26T09:12:15",
      "chunksAnalyzed": 3,
      "chunksFailed": 0
    }
  ],
  "consensusReport": {
    "reportId": "consensus-uuid",
    "commonThemes": "...",
    "agreements": "...",
    "disagreements": "...",
    "unifiedRecommendations": "...",
    "attributedInsights": "...",
    "generatedAt": "2026-04-26T09:15:32",
    "agentReportsIncluded": 6
  }
}
```

**Agent types in `agentReports`:**

| `agentType` | Agent |
|-------------|-------|
| `LEAD_ANALYST` | Lead Analyst |
| `GENERAL_ANALYST` | General Analyst |
| `METHODOLOGY_REVIEWER` | Methodology Reviewer |
| `LITERATURE_REVIEWER` | Literature Reviewer |
| `QUICK_SCREENER` | Quick Screener |
| `FACT_EXTRACTOR` | Fact Extractor |

### Error Responses

| HTTP | Code | Cause |
|------|------|-------|
| 404 | `DOCUMENT_NOT_FOUND` | No document with that ID exists |
| 409 | `DOCUMENT_PROCESSING` | Still processing — poll `/status` |
| 500 | `RESULTS_RETRIEVAL_FAILED` | Server error |

---

## GET `/{documentId}/results/pdf`

Download the consensus report as a professionally styled multi-page PDF.

### Response `200 OK`

- `Content-Type: application/pdf`
- `Content-Disposition: attachment; filename="analysis-report-{documentId}.pdf"`
- Body: Binary PDF

### Error Responses

| HTTP | Code | Cause |
|------|------|-------|
| 404 | `DOCUMENT_NOT_FOUND` | No document with that ID exists |
| 409 | `DOCUMENT_PROCESSING` | Still processing — poll `/status` |
| 500 | `PDF_GENERATION_FAILED` | PDF rendering error |

---

## Error Response Format

All error responses use this consistent structure:

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable description of what went wrong",
  "timestamp": "2026-04-26T09:03:47",
  "documentId": "a1b2c3d4-...",
  "details": {
    "key": "additional context"
  }
}
```

`documentId` and `details` are omitted when not applicable (`@JsonInclude(NON_NULL)`).

---

## Complete Example Workflow

```bash
# 1. Upload a PDF
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@research-paper.pdf"

# Response:
# { "documentId": "abc-123", "filename": "research-paper.pdf", "status": "uploaded" }

# 2. Poll status until COMPLETE
curl http://localhost:8080/api/documents/abc-123/status

# 3. Get consensus report as JSON
curl http://localhost:8080/api/documents/abc-123/results

# 4. Get all 6 agent reports + consensus
curl http://localhost:8080/api/documents/abc-123/results/detailed

# 5. Download PDF report
curl -OJ http://localhost:8080/api/documents/abc-123/results/pdf
```
