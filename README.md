# AI Research Panel System

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)

A Spring Boot application that orchestrates multi-agent AI analysis of research documents using NVIDIA models. Upload a PDF and receive a comprehensive consensus report from 6 specialized AI agents running in parallel — exportable as JSON or a professionally styled PDF.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Quick Start](#quick-start)
3. [Configuration](#configuration)
4. [API Reference](#api-reference)
5. [Processing Pipeline](#processing-pipeline)
6. [AI Agents](#ai-agents)
7. [Consensus Engine](#consensus-engine)
8. [PDF Export](#pdf-export)
9. [Architecture](#architecture)
10. [Database Schema](#database-schema)
11. [Error Handling](#error-handling)
12. [Docker Management](#docker-management)
13. [Troubleshooting](#troubleshooting)

---

## Prerequisites

Only two things required:

1. **Docker Desktop** — [Download here](https://www.docker.com/products/docker-desktop)
2. **NVIDIA API Key** (free) — [Get one at build.nvidia.com](https://build.nvidia.com/)

No local Java, Maven, or PostgreSQL installation needed. Everything runs inside Docker containers.

---

## Quick Start

```cmd
start.bat
```

The script handles the full setup automatically:

1. Checks Docker is running
2. Creates `.env` from `.env.example` if it doesn't exist
3. Prompts for your NVIDIA API key if not configured
4. Builds and starts all containers
5. Waits for the health check to pass
6. Opens Swagger UI in your browser

To stop the project:

```cmd
stop.bat
```

This stops the containers without removing them, so the next start is faster.

**Access points once running:**

| URL | Purpose |
|-----|---------|
| `http://localhost:8080/swagger-ui.html` | Interactive API documentation |
| `http://localhost:8080/actuator/health` | Application health check |
| `http://localhost:8080/api/documents/upload` | Upload endpoint |

---

## Configuration

All configuration lives in the `.env` file. Copy `.env.example` to `.env` to get started.

### Required

| Variable | Description |
|----------|-------------|
| `NVIDIA_API_KEY` | Your NVIDIA API key from [build.nvidia.com](https://build.nvidia.com/) |

### Optional (with defaults)

| Variable | Default | Description |
|----------|---------|-------------|
| `NVIDIA_API_ENDPOINT` | `https://integrate.api.nvidia.com/v1` | NVIDIA API base URL |
| `NVIDIA_API_MODEL` | `meta/llama-3.3-70b-instruct` | Model used for all agents and consensus |
| `POSTGRES_PASSWORD` | `changeme` | PostgreSQL database password |
| `POSTGRES_PORT` | `5432` | PostgreSQL host port |
| `SERVER_PORT` | `8080` | Application HTTP port |
| `LOG_LEVEL_ROOT` | `INFO` | Root logging level (`TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`) |
| `LOG_LEVEL_APP` | `DEBUG` | Application-level logging |
| `JAVA_OPTS` | `-Xms512m -Xmx2048m -XX:+UseG1GC` | JVM memory and GC settings |

### Advanced (set directly in `docker-compose.yml`)

| Variable | Default | Description |
|----------|---------|-------------|
| `NVIDIA_API_RATE_LIMIT` | `60` | Max requests per minute to NVIDIA API |
| `NVIDIA_API_MAX_CONNECTIONS` | `10` | Max concurrent HTTP connections to NVIDIA |
| `PROCESSING_CHUNK_SIZE` | `100000` | Token target per document chunk |
| `PROCESSING_MAX_RETRIES` | `3` | Retry attempts per failed chunk |
| `ASYNC_CORE_POOL_SIZE` | `10` | Core async thread pool size |
| `ASYNC_MAX_POOL_SIZE` | `20` | Max async thread pool size |
| `ASYNC_QUEUE_CAPACITY` | `100` | Async task queue capacity |

---

## API Reference

**Base URL:** `http://localhost:8080/api/documents`

All endpoints return JSON. Errors follow a consistent structure (see [Error Handling](#error-handling)).

---

### `POST /upload`

Upload a PDF document for analysis. Triggers async processing immediately after upload.

**Request:** `multipart/form-data`

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `file` | File | Yes | PDF only, max 50MB, max 500 pages |

**Validation performed:**
- File must not be empty
- MIME type must be `application/pdf`
- File size must not exceed 50MB
- PDF structure validated using Apache PDFBox (rejects corrupted files)

**Response `200 OK`:**
```json
{
  "documentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "filename": "research-paper.pdf",
  "status": "uploaded"
}
```

**Error responses:**

| HTTP | Code | Cause |
|------|------|-------|
| 400 | `INVALID_FILE` | Empty file or missing |
| 400 | `INVALID_FILE_TYPE` | Not a PDF |
| 400 | `INVALID_PDF` | Corrupted or unreadable PDF structure |
| 413 | `FILE_TOO_LARGE` | File exceeds 50MB |
| 500 | `UPLOAD_FAILED` | Server-side storage error |

---

### `GET /{documentId}/status`

Poll the processing status of a document. Use this to track progress while the document is being analyzed.

**Response `200 OK`:**
```json
{
  "documentId": "a1b2c3d4-...",
  "status": "ANALYZING",
  "agentProgress": [
    {
      "agentId": "lead-analyst-uuid",
      "agentType": "LEAD_ANALYST",
      "chunksCompleted": 2,
      "totalChunks": 3
    },
    {
      "agentId": "general-analyst-uuid",
      "agentType": "GENERAL_ANALYST",
      "chunksCompleted": 3,
      "totalChunks": 3
    }
  ]
}
```

**Document statuses:**

| Status | Meaning |
|--------|---------|
| `UPLOADED` | File received and stored, queued for processing |
| `PROCESSING` | Text extraction from PDF in progress |
| `ANALYZING` | 6 AI agents analyzing in parallel |
| `DELIBERATING` | Consensus engine synthesizing agent reports |
| `COMPLETE` | Analysis done — results available |
| `FAILED` | Processing failed — `errorMessage` field contains details |

**Error responses:**

| HTTP | Code | Cause |
|------|------|-------|
| 404 | `DOCUMENT_NOT_FOUND` | No document with that ID |
| 500 | `STATUS_RETRIEVAL_FAILED` | Server error |

---

### `GET /{documentId}/results`

Get the consensus report as JSON. Returns the current `DocumentStatus` (HTTP 200) if still processing.

**Response `200 OK` (complete):**
```json
{
  "reportId": "consensus-uuid",
  "documentId": "a1b2c3d4-...",
  "commonThemes": "Both the Lead Analyst and Methodology Reviewer identified...",
  "agreements": "All agents agreed that the sample size was adequate...",
  "disagreements": "The Literature Reviewer noted gaps in citation coverage that...",
  "unifiedRecommendations": "1. Expand the discussion section to address...",
  "attributedInsights": "Lead Analyst: The research design shows strong internal validity...",
  "generatedAt": "2026-04-26T09:15:00",
  "agentReportsIncluded": 6
}
```

**Error responses:**

| HTTP | Code | Cause |
|------|------|-------|
| 404 | `DOCUMENT_NOT_FOUND` | No document with that ID |
| 500 | `RESULTS_RETRIEVAL_FAILED` | Server error |

---

### `GET /{documentId}/results/detailed`

Get all 6 individual agent reports plus the consensus report in a single response.

**Response `200 OK`:**
```json
{
  "documentId": "a1b2c3d4-...",
  "agentReports": [
    {
      "reportId": "report-uuid",
      "documentId": "a1b2c3d4-...",
      "agentId": "lead-analyst-uuid",
      "agentType": "LEAD_ANALYST",
      "keyFindings": "The study demonstrates strong internal validity...",
      "strengths": "1. Well-defined research question\n2. Appropriate sample size...",
      "weaknesses": "1. Limited external validity\n2. Short follow-up period...",
      "recommendations": "1. Extend the follow-up period\n2. Include a control group...",
      "completedAt": "2026-04-26T09:12:00",
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
    "generatedAt": "2026-04-26T09:15:00",
    "agentReportsIncluded": 6
  }
}
```

**Error responses:**

| HTTP | Code | Cause |
|------|------|-------|
| 404 | `DOCUMENT_NOT_FOUND` | No document with that ID |
| 409 | `DOCUMENT_PROCESSING` | Still processing — poll `/status` |
| 500 | `RESULTS_RETRIEVAL_FAILED` | Server error |

---

### `GET /{documentId}/results/pdf`

Download the consensus report as a professionally styled PDF.

**Response `200 OK`:**
- `Content-Type: application/pdf`
- `Content-Disposition: attachment; filename="analysis-report-{documentId}.pdf"`
- Body: Binary PDF file

**Error responses:**

| HTTP | Code | Cause |
|------|------|-------|
| 404 | `DOCUMENT_NOT_FOUND` | No document with that ID |
| 409 | `DOCUMENT_PROCESSING` | Still processing — poll `/status` |
| 500 | `PDF_GENERATION_FAILED` | PDF rendering error |

---

### Example Workflow

```bash
# 1. Upload a PDF
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@research-paper.pdf"
# Response: { "documentId": "abc-123", "status": "uploaded" }

# 2. Poll until status is COMPLETE
curl http://localhost:8080/api/documents/abc-123/status

# 3. Get the consensus report
curl http://localhost:8080/api/documents/abc-123/results

# 4. Get all 6 agent reports
curl http://localhost:8080/api/documents/abc-123/results/detailed

# 5. Download the PDF report
curl -OJ http://localhost:8080/api/documents/abc-123/results/pdf
```

---

## Processing Pipeline

Once a document is uploaded, processing runs entirely in the background. The client polls `/status` to track progress.

```
┌─────────────────────────────────────────────────────────────────┐
│  POST /upload                                                   │
│  • Validate: not empty, ≤50MB, MIME=application/pdf, valid PDF  │
│  • Generate UUID as documentId                                  │
│  • Store to /app/data/uploads/{documentId}.pdf                  │
│  • Create Document record (status=UPLOADED)                     │
│  • Trigger async pipeline                                       │
└───────────────────────────┬─────────────────────────────────────┘
                            │ (background thread)
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  Stage 1 — Text Extraction                    status=PROCESSING │
│  • Load PDF with Apache PDFBox                                  │
│  • Extract text with setSortByPosition(true)                    │
│    (preserves logical reading order)                            │
│  • Validate: ≤500 pages, ≤1,000,000 tokens                     │
│  • Token count = text.length() / 4 (approximation)             │
│  • Store ExtractedDocument entity                               │
└───────────────────────────┬─────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  Stage 2 — Document Chunking                                    │
│  • If ≤100,000 tokens → single chunk                            │
│  • If >100,000 tokens → semantic chunking:                      │
│    - Find boundaries: section headers > paragraph breaks        │
│      > sentence endings                                         │
│    - Target: 100,000 tokens per chunk                           │
│    - Overlap: 500 tokens between consecutive chunks             │
│  • Store DocumentChunk entities with sequence numbers           │
└───────────────────────────┬─────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  Stage 3 — Parallel Agent Analysis            status=ANALYZING  │
│  • 6 agents submitted to ExecutorService (30-min timeout each)  │
│  • Each agent independently:                                    │
│    - Processes chunks sequentially                              │
│    - Carries context summary forward between chunks             │
│    - Retries failed chunks (3×, backoff: 1s → 2s → 4s)         │
│    - Synthesizes chunk analyses into AnalysisReport             │
│    - Persists report to database                                │
│  • AgentProgress updated after each chunk                       │
└───────────────────────────┬─────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  Stage 4 — Consensus Generation           status=DELIBERATING   │
│  • Collect all available AnalysisReports                        │
│  • Build synthesis prompt with all agent findings               │
│  • Call NVIDIA model (temp=0.7, max_tokens=4000)                │
│  • Parse response into 5 sections                               │
│  • Store ConsensusReport entity                                 │
└───────────────────────────┬─────────────────────────────────────┘
                            ▼
                    status = COMPLETE
                  Results available via API
```

**On any failure:** status → `FAILED`, error message stored on the Document entity.

---

## AI Agents

Six specialized agents analyze each document in parallel. All agents share the same base behavior: process chunks sequentially with context carryover, retry on failure, then synthesize a unified report.

### Base Agent Behavior (`AIAgent`)

- Processes document chunks one at a time in sequence
- Passes a 800-character context summary from previous chunks into each new chunk prompt
- On chunk failure: retries up to 3 times with exponential backoff (1s, 2s, 4s)
- Retryable errors: 5xx responses, network timeouts, 429 rate limits
- Non-retryable errors: 4xx responses (except 429)
- Failed chunks are noted in the report; analysis continues with remaining chunks
- After all chunks: synthesizes into a single `AnalysisReport` with key findings, strengths, weaknesses, and recommendations

---

### Agent 1 — Lead Analyst

**Specialization:** Deep critical analysis, research validity, overall scientific quality

**Focus areas:**
- Methodological rigor and validity of conclusions
- Whether evidence sufficiently supports claims
- Alternative explanations and confounding factors
- Significance and impact of findings
- Key limitations and their effect on results

**Output:** High-level strategic assessment of overall research quality

---

### Agent 2 — General Analyst

**Specialization:** Holistic document review across all dimensions

**Focus areas:**
- Content quality, organization, and logical flow
- Clarity and accessibility of writing
- Completeness and thoroughness
- Internal consistency
- Effectiveness of figures, tables, and supplementary materials

**Output:** Broad multi-dimensional assessment of the document as a whole

---

### Agent 3 — Methodology Reviewer

**Specialization:** Research design, statistical approaches, data analysis

**Focus areas:**
- Appropriateness of experimental design for the research question
- Sampling strategies and sample size justification
- Statistical test selection and assumption validation
- Reproducibility and transparency of methods
- Bias control and confounding variable management

**Output:** Technical assessment of methodological soundness with improvement suggestions

---

### Agent 4 — Literature Reviewer

**Specialization:** Scholarly context, citations, theoretical framework

**Focus areas:**
- Comprehensiveness and currency of literature review
- Citation quality, accuracy, and completeness
- Theoretical framework coherence
- Novelty and contribution relative to prior work
- Gaps in literature coverage

**Output:** Assessment of how the work positions itself within the broader research landscape

---

### Agent 5 — Quick Screener

**Specialization:** Rapid triage, key claims identification, red flag detection

**Focus areas:**
- Main research questions and primary claims
- Immediate red flags or critical concerns
- Whether key claims are adequately supported
- High-priority aspects requiring deeper scrutiny
- Obvious errors or inconsistencies

**Output:** Rapid triage report highlighting the most important findings and concerns

---

### Agent 6 — Fact Extractor

**Specialization:** Structured fact extraction, numerical data, summaries

**Focus areas:**
- Key facts, data points, and numerical results
- Sample characteristics (size, demographics, parameters)
- Methodological details and instruments used
- Primary outcomes and effect sizes
- Dates, locations, and contextual details

**Output:** Structured, organized factual information for easy reference

---

## Consensus Engine

After all agents complete, the consensus engine makes one final call to the NVIDIA model to synthesize all reports into a unified output.

**Model parameters:** temperature=0.7, max_tokens=4000

**System prompt:** *"You are an expert at synthesizing multiple expert analyses into a unified consensus report. Analyze the provided agent reports and identify common themes, areas of agreement, areas of disagreement, and generate unified recommendations. Attribute insights to specific agent types."*

**Output sections:**

| Section | Content |
|---------|---------|
| **Common Themes** | Themes and patterns that appeared across multiple agent analyses |
| **Agreements** | Points where agents reached consensus |
| **Disagreements** | Areas where agents had differing perspectives or conflicting findings |
| **Unified Recommendations** | Synthesized recommendations from the full panel |
| **Attributed Insights** | Unique contributions attributed to specific agents |

**Graceful degradation:** If some agents fail, consensus still generates from the available reports. Only if all agents fail does the document status become `FAILED`.

---

## PDF Export

The PDF report is generated using iText7 with immediate page flush disabled (so all pages remain writable for the footer pass).

**Report structure:**

| Section | Content |
|---------|---------|
| Cover page | Title, subtitle, metadata table (documentId, reportId, timestamp, agent count), contributing agent list |
| Table of contents | Links to all 5 content sections |
| Common Themes | Blue header, content with numbered item highlighting |
| Agreements | Green header, consensus findings |
| Disagreements | Orange header, conflicting perspectives |
| Unified Recommendations | Blue header, synthesized panel recommendations |
| Attributed Insights | Gray header, agent-specific contributions |
| Page footers | Page X of Y, "AI Panelist System • Powered by NVIDIA AI" |

**Color scheme:**

| Color | Hex | Used for |
|-------|-----|---------|
| Primary | `#2962FF` | Cover, Common Themes, Recommendations headers |
| Success | `#10B981` | Agreements header |
| Warning | `#F59E0B` | Disagreements header |
| Secondary | `#646464` | Attributed Insights, footers, subtitles |
| Accent | `#F5F7FA` | Content box backgrounds |

---

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                      Docker Network                          │
│                                                              │
│  ┌─────────────────────────┐    ┌────────────────────────┐  │
│  │     aipanelist-app      │    │  aipanelist-postgres   │  │
│  │   Spring Boot :8080     │───▶│   PostgreSQL 15        │  │
│  │                         │    │   :5432                │  │
│  │  DocumentController     │    └────────────────────────┘  │
│  │  PDFUploadService       │                                │
│  │  AsyncAnalysisService   │                                │
│  │  DocumentProcessor      │                                │
│  │  ChunkProcessor         │                                │
│  │  PanelOrchestrator      │───▶  NVIDIA API (external)    │
│  │  ├─ LeadAnalystAgent    │      integrate.api.nvidia.com  │
│  │  ├─ GeneralAnalystAgent │                                │
│  │  ├─ MethodologyReviewer │                                │
│  │  ├─ LiteratureReviewer  │                                │
│  │  ├─ QuickScreener       │                                │
│  │  └─ FactExtractor       │                                │
│  │  ConsensusEngine        │                                │
│  │  PDFExportService       │                                │
│  └─────────────────────────┘                                │
└──────────────────────────────────────────────────────────────┘
```

**Component responsibilities:**

| Component | Responsibility |
|-----------|---------------|
| `DocumentController` | REST endpoints, request validation, response formatting |
| `PDFUploadService` | File validation (size, MIME, structure), storage, DB record creation |
| `AsyncAnalysisService` | Orchestrates the full pipeline in a background thread |
| `DocumentProcessor` | PDF text extraction with PDFBox, token counting |
| `ChunkProcessor` | Semantic document chunking with overlap |
| `PanelOrchestrator` | Spawns 6 agents in parallel via ExecutorService |
| `AIAgent` (×6) | Per-agent analysis with retry logic and context carryover |
| `NVIDIAModelClient` | HTTP client with connection pooling, rate limiting, circuit breaker |
| `ConsensusEngine` | Synthesizes all agent reports into final consensus |
| `PDFExportService` | Generates styled multi-page PDF with iText7 |

**NVIDIA API client internals:**
- Connection pool: max 10 concurrent connections (PoolingHttpClientConnectionManager)
- Rate limiter: Semaphore-based, 60 req/min (configurable)
- Timeouts: 30s connection, 300s response (5 minutes per request)
- Circuit breaker (Resilience4j): 50% failure threshold, 10-call sliding window, 60s open duration
- 429 handling: reads `Retry-After` header and waits before retrying

---

## Database Schema

Schema is managed automatically by Hibernate (`ddl-auto=update`). The `init-db/01-init.sql` script runs once on first container start to create required PostgreSQL extensions.

| Table | Purpose |
|-------|---------|
| `documents` | Document metadata: filename, size, upload time, status, page/token counts, error message |
| `extracted_documents` | Full extracted text (LOB), token count, extraction timestamp |
| `document_chunks` | Chunked text with sequence numbers, byte offsets, token counts, overlap info |
| `analysis_reports` | Per-agent reports: key findings, strengths, weaknesses, recommendations, chunks analyzed/failed |
| `chunk_analyses` | Per-chunk analysis results with context summaries carried between chunks |
| `consensus_reports` | Final synthesized output: common themes, agreements, disagreements, recommendations, attributed insights |
| `agent_progress` | Real-time chunk completion tracking per agent (used by `/status` endpoint) |

**Entity relationships:**

```
documents (1) ──────── (1) extracted_documents
documents (1) ──────── (N) document_chunks
documents (1) ──────── (N) analysis_reports
documents (1) ──────── (1) consensus_reports
documents (1) ──────── (N) agent_progress
analysis_reports (1) ── (N) chunk_analyses
```

---

## Error Handling

All error responses follow this structure:

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

`documentId` and `details` are omitted when not applicable.

**HTTP status codes used:**

| Code | Meaning | Example |
|------|---------|---------|
| `400` | Bad request — validation failure | Invalid file type, empty file |
| `404` | Not found | Document ID doesn't exist |
| `409` | Conflict — document still processing | Requesting results before `COMPLETE` |
| `413` | Payload too large | File exceeds 50MB |
| `422` | Unprocessable entity | Text extraction failed |
| `500` | Internal server error | Unexpected application error |
| `502` | Bad gateway | NVIDIA API returned an error |
| `503` | Service unavailable | Max retries exceeded on NVIDIA API |

**Retry and resilience behavior:**

| Scenario | Behavior |
|----------|---------|
| NVIDIA API 5xx error | Retry up to 3 times with exponential backoff (1s, 2s, 4s) |
| NVIDIA API 429 rate limit | Read `Retry-After` header, wait, then retry |
| NVIDIA API 4xx error | No retry — fail immediately |
| Network timeout | Retry up to 3 times |
| Circuit breaker open | Fail fast — returns 503 until circuit closes (60s) |
| Single agent failure | Other agents continue — consensus uses available reports |
| All agents fail | Document status set to `FAILED` |

---

## Docker Management

**Start the project:**
```cmd
start.bat
```

**Stop the project (containers preserved):**
```cmd
stop.bat
```

**Other useful commands:**

```bash
# View live application logs
docker-compose logs -f app

# View database logs
docker-compose logs -f postgres

# Restart the application only
docker-compose restart app

# Remove containers (data preserved in volumes)
docker-compose down

# Remove containers AND all data (full reset)
docker-compose down -v

# Rebuild after code changes
docker-compose up -d --build app
```

---

## Troubleshooting

**NVIDIA API key not working**
```
Verify the key is valid at https://build.nvidia.com/
Ensure NVIDIA_API_KEY is set correctly in .env (no quotes, no spaces)
```

**Document stuck in ANALYZING or PROCESSING**
```bash
docker-compose logs -f app   # look for ERROR lines with the documentId
```

**Database connection refused on startup**
```bash
# Postgres may still be initializing — wait 30s then restart app
docker-compose restart app
```

**Out of memory errors**
```
Increase heap in .env:
JAVA_OPTS=-Xms512m -Xmx4096m -XX:+UseG1GC
```

**PDF upload rejected with INVALID_PDF**
```
The file may be password-protected or corrupted.
Try opening it in a PDF viewer first to confirm it's readable.
```

**Port 8080 already in use**
```
Change SERVER_PORT in .env to another port (e.g. 8090)
```

---

## License

MIT License — see [LICENSE](LICENSE) for details.
