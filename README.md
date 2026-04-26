# AI Research Panel System

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)

A Spring Boot application that orchestrates multi-agent AI analysis of research documents using NVIDIA models. Upload a PDF and receive a comprehensive consensus report from 6 specialized AI agents running in parallel, exportable as JSON or a styled PDF.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Processing Pipeline](#processing-pipeline)
- [AI Agents](#ai-agents)
- [Consensus Engine](#consensus-engine)
- [PDF Export](#pdf-export)
- [Architecture](#architecture)
- [Database Schema](#database-schema)
- [Error Handling](#error-handling)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

1. **Docker Desktop** — [Download](https://www.docker.com/products/docker-desktop)
2. **NVIDIA API Key** (free) — [Get one at build.nvidia.com](https://build.nvidia.com/)

No local Java, Maven, or PostgreSQL required. Everything runs in Docker.

---

## Quick Start

```cmd
start.bat
```

The script handles everything:
- Checks Docker is running
- Creates `.env` from `.env.example` if missing
- Prompts for your NVIDIA API key if not configured
- Builds and starts all containers
- Waits for health check
- Opens Swagger UI automatically

**Access points after startup:**

| URL | Purpose |
|-----|---------|
| `http://localhost:8080/swagger-ui.html` | Interactive API docs |
| `http://localhost:8080/actuator/health` | Health check |
| `http://localhost:8080/api/documents/upload` | Upload endpoint |

---

## Configuration

Copy `.env.example` to `.env` and configure:

```env
# ── Required ──────────────────────────────────────────
NVIDIA_API_KEY=your-api-key-here

# ── Database ──────────────────────────────────────────
POSTGRES_PASSWORD=changeme
POSTGRES_PORT=5432

# ── Server ────────────────────────────────────────────
SERVER_PORT=8080

# ── Logging ───────────────────────────────────────────
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=DEBUG

# ── JVM ───────────────────────────────────────────────
JAVA_OPTS=-Xms512m -Xmx2048m -XX:+UseG1GC
```

**All configuration properties:**

| Property | Default | Description |
|----------|---------|-------------|
| `NVIDIA_API_KEY` | — | **Required.** NVIDIA API key |
| `NVIDIA_API_ENDPOINT` | `https://integrate.api.nvidia.com/v1` | NVIDIA API base URL |
| `NVIDIA_API_MODEL` | `meta/llama-3.3-70b-instruct` | Model used for all agents and consensus |
| `POSTGRES_PASSWORD` | `changeme` | PostgreSQL password |
| `POSTGRES_PORT` | `5432` | PostgreSQL host port |
| `SERVER_PORT` | `8080` | Application HTTP port |
| `LOG_LEVEL_ROOT` | `INFO` | Root log level |
| `LOG_LEVEL_APP` | `DEBUG` | Application log level |
| `JAVA_OPTS` | `-Xms512m -Xmx2048m -XX:+UseG1GC` | JVM options |

**Advanced (optional, set in `docker-compose.yml`):**

| Property | Default | Description |
|----------|---------|-------------|
| `NVIDIA_API_RATE_LIMIT` | `60` | Max requests per minute to NVIDIA API |
| `NVIDIA_API_MAX_CONNECTIONS` | `10` | Max concurrent HTTP connections |
| `PROCESSING_CHUNK_SIZE` | `100000` | Tokens per document chunk |
| `PROCESSING_MAX_RETRIES` | `3` | Retry attempts per chunk |
| `ASYNC_CORE_POOL_SIZE` | `10` | Core async thread pool size |
| `ASYNC_MAX_POOL_SIZE` | `20` | Max async thread pool size |

---

## API Reference

**Base URL:** `http://localhost:8080/api/documents`

### POST `/upload`

Upload a PDF document for analysis.

**Request:** `multipart/form-data`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | File | Yes | PDF file, max 50MB, max 500 pages |

**Response `200 OK`:**
```json
{
  "documentId": "a1b2c3d4-...",
  "filename": "research-paper.pdf",
  "uploadedAt": "2026-04-26T09:00:00",
  "status": "UPLOADED"
}
```

**Error codes:**

| Code | HTTP | Meaning |
|------|------|---------|
| `INVALID_FILE` | 400 | Empty file or not a PDF |
| `FILE_TOO_LARGE` | 413 | File exceeds 50MB |
| `INVALID_PDF` | 400 | Corrupted or unreadable PDF |

---

### GET `/{documentId}/status`

Poll processing status and per-agent progress.

**Response `200 OK`:**
```json
{
  "documentId": "a1b2c3d4-...",
  "status": "ANALYZING",
  "agentProgress": [
    {
      "agentId": "lead-analyst-...",
      "agentType": "LEAD_ANALYST",
      "chunksCompleted": 2,
      "totalChunks": 3
    }
  ]
}
```

**Processing statuses:**

| Status | Meaning |
|--------|---------|
| `UPLOADED` | File received, queued for processing |
| `PROCESSING` | Text extraction in progress |
| `ANALYZING` | 6 agents analyzing in parallel |
| `DELIBERATING` | Consensus engine synthesizing |
| `COMPLETE` | Results ready |
| `FAILED` | Processing failed — check `errorMessage` |

---

### GET `/{documentId}/results`

Get the consensus report as JSON.

**Response `200 OK`:**
```json
{
  "reportId": "consensus-...",
  "documentId": "a1b2c3d4-...",
  "commonThemes": "...",
  "agreements": "...",
  "disagreements": "...",
  "unifiedRecommendations": "...",
  "attributedInsights": "...",
  "generatedAt": "2026-04-26T09:15:00",
  "agentReportsIncluded": 6
}
```

Returns `DocumentStatus` (with HTTP 200) if still processing.

---

### GET `/{documentId}/results/detailed`

Get all 6 individual agent reports plus the consensus report.

**Response `200 OK`:**
```json
{
  "documentId": "a1b2c3d4-...",
  "agentReports": [
    {
      "reportId": "...",
      "agentType": "LEAD_ANALYST",
      "keyFindings": "...",
      "strengths": "...",
      "weaknesses": "...",
      "recommendations": "...",
      "completedAt": "...",
      "chunksAnalyzed": 3,
      "chunksFailed": 0
    }
  ],
  "consensusReport": { ... }
}
```

---

### GET `/{documentId}/results/pdf`

Download the consensus report as a styled PDF.

**Response `200 OK`:**
- Content-Type: `application/pdf`
- Content-Disposition: `attachment; filename="analysis-report-{documentId}.pdf"`
- Body: Binary PDF

---

**Common error response format (all endpoints):**
```json
{
  "code": "DOCUMENT_NOT_FOUND",
  "message": "Document a1b2c3d4 not found",
  "documentId": "a1b2c3d4-...",
  "details": {}
}
```

**Example workflow:**
```bash
# 1. Upload
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@paper.pdf"
# → { "documentId": "abc123" }

# 2. Poll until COMPLETE
curl http://localhost:8080/api/documents/abc123/status

# 3. Get results
curl http://localhost:8080/api/documents/abc123/results

# 4. Download PDF
curl -O -J http://localhost:8080/api/documents/abc123/results/pdf
```

---

## Processing Pipeline

```
Upload PDF
    │
    ▼
Validate (size ≤50MB, type=PDF, ≤500 pages)
    │
    ▼
Store to /app/data/uploads/{documentId}.pdf
    │
    ▼ (async background thread)
Extract Text (Apache PDFBox, reading order preserved)
    │
    ▼
Chunk Document
    ├── ≤100K tokens → single chunk
    └── >100K tokens → semantic chunking
                        (section headers → paragraph breaks → sentence endings)
                        (100K token target, 500-token overlap)
    │
    ▼
Parallel Agent Analysis (6 agents × ExecutorService, 30-min timeout)
    ├── Lead Analyst
    ├── General Analyst
    ├── Methodology Reviewer
    ├── Literature Reviewer
    ├── Quick Screener
    └── Fact Extractor
         │
         └── Each agent: sequential chunk processing
                          retry (3× exponential backoff: 1s, 2s, 4s)
                          context carryover between chunks
    │
    ▼
Consensus Engine (NVIDIA model synthesizes all agent reports)
    │
    ▼
COMPLETE — results available via API
```

**Status transitions:** `UPLOADED → PROCESSING → ANALYZING → DELIBERATING → COMPLETE`  
**On any failure:** `→ FAILED` (error message stored on document)

---

## AI Agents

All 6 agents run in parallel. Each processes document chunks sequentially, carrying context forward between chunks, then synthesizes a unified report.

| Agent | Specialization |
|-------|---------------|
| **Lead Analyst** | Deep critical analysis — methodological rigor, validity of conclusions, scientific quality, key limitations |
| **General Analyst** | Holistic review — content quality, organization, clarity, completeness, logical flow |
| **Methodology Reviewer** | Research design — experimental design, sampling, statistical validity, reproducibility, bias control |
| **Literature Reviewer** | Scholarly context — citation quality, theoretical framework, novelty, positioning relative to prior work |
| **Quick Screener** | Rapid triage — main claims, red flags, high-priority issues requiring deeper scrutiny |
| **Fact Extractor** | Structured extraction — numerical data, sample characteristics, methodological parameters, primary outcomes |

**Per-agent output structure:**
- Key Findings
- Strengths
- Weaknesses
- Recommendations
- Chunks analyzed / failed

**Retry behavior per chunk:**
- 3 attempts with exponential backoff (1s → 2s → 4s)
- Retryable: 5xx errors, network timeouts, 429 rate limits
- Non-retryable: 4xx errors (except 429)
- Failed chunks are noted in the report; analysis continues with remaining chunks

---

## Consensus Engine

After all agents complete, the consensus engine calls the NVIDIA model once more to synthesize all 6 reports into a unified output.

**System prompt:** *"You are an expert at synthesizing multiple expert analyses into a unified consensus report."*

**Output sections:**

| Section | Content |
|---------|---------|
| Common Themes | Themes that appeared across multiple agent analyses |
| Agreements | Points where agents reached consensus |
| Disagreements | Areas where agents had differing perspectives |
| Unified Recommendations | Synthesized recommendations from the full panel |
| Attributed Insights | Unique contributions attributed to specific agents |

**Graceful degradation:** If some agents fail, consensus still generates from the available reports. Only if all agents fail does the document status become `FAILED`.

---

## PDF Export

The PDF report is generated with iText7 and includes:

1. **Cover page** — title, subtitle, metadata table (documentId, reportId, generated timestamp, agent count), contributing agent list
2. **Table of contents** — links to all 5 sections
3. **Content sections** — each with a color-coded header, description, and formatted content box
4. **Page footers** — page numbers and "AI Panelist System • Powered by NVIDIA AI"

**Color scheme:**

| Color | Hex | Used for |
|-------|-----|---------|
| Primary | `#2962FF` | Headers, cover, common themes, recommendations |
| Success | `#10B981` | Agreements section |
| Warning | `#F59E0B` | Disagreements section |
| Secondary | `#646464` | Subtitles, footers, attributed insights |

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                   Docker Network                     │
│                                                     │
│  ┌──────────────────────┐   ┌────────────────────┐  │
│  │   aipanelist-app     │   │ aipanelist-postgres │  │
│  │   Spring Boot :8080  │──▶│  PostgreSQL 15      │  │
│  │                      │   │  :5432              │  │
│  │  DocumentController  │   └────────────────────┘  │
│  │  AsyncAnalysisService│                           │
│  │  PanelOrchestrator   │                           │
│  │  ConsensusEngine     │──▶  NVIDIA API            │
│  │  PDFExportService    │     (external)            │
│  └──────────────────────┘                           │
└─────────────────────────────────────────────────────┘
```

**Key components:**

| Component | Responsibility |
|-----------|---------------|
| `DocumentController` | REST API, request validation |
| `PDFUploadService` | File storage, document entity creation |
| `AsyncAnalysisService` | Pipeline orchestration in background thread |
| `DocumentProcessor` | PDF text extraction (PDFBox) |
| `ChunkProcessor` | Semantic document chunking |
| `PanelOrchestrator` | Spawns and manages 6 parallel agents |
| `AIAgent` (×6) | Per-agent analysis with retry logic |
| `NVIDIAModelClient` | HTTP client with pooling, rate limiting, circuit breaker |
| `ConsensusEngine` | Synthesizes agent reports into consensus |
| `PDFExportService` | Generates styled PDF report (iText7) |

**NVIDIA API client features:**
- Connection pool: max 10 concurrent connections
- Rate limiter: semaphore-based, 60 req/min (configurable)
- Timeouts: 30s connection, 300s response
- Circuit breaker: 50% failure threshold, 10-call window, 60s open duration
- 429 handling: reads `Retry-After` header and waits

---

## Database Schema

| Table | Purpose |
|-------|---------|
| `documents` | Document metadata, status, page/token counts |
| `extracted_documents` | Full extracted text (LOB) |
| `document_chunks` | Chunked text with sequence numbers and byte offsets |
| `analysis_reports` | Per-agent reports with findings, strengths, weaknesses, recommendations |
| `chunk_analyses` | Per-chunk analysis results with context summaries |
| `consensus_reports` | Final synthesized consensus output |
| `agent_progress` | Real-time chunk completion tracking per agent |

**Relationships:**
```
documents (1) ──── (1) extracted_documents
documents (1) ──── (N) document_chunks
documents (1) ──── (N) analysis_reports
documents (1) ──── (1) consensus_reports
documents (1) ──── (N) agent_progress
analysis_reports (1) ── (N) chunk_analyses
```

Schema is managed by Hibernate (`ddl-auto=update`) — no manual migrations needed.

---

## Error Handling

**HTTP status codes:**

| Code | Meaning |
|------|---------|
| `400` | Invalid file, validation failure |
| `404` | Document not found |
| `409` | Document still processing |
| `413` | File exceeds 50MB |
| `422` | Text extraction failed |
| `500` | Unexpected server error |
| `502` | NVIDIA API error |
| `503` | Max retries exceeded |

All error responses follow the same JSON structure:
```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable description",
  "documentId": "...",
  "details": {}
}
```

---

## Troubleshooting

**API key not working**
```
Check NVIDIA_API_KEY in .env
Validate at https://build.nvidia.com/
```

**Document stuck in ANALYZING**
```bash
docker-compose logs -f app   # look for agent errors
```

**Database connection refused**
```bash
docker-compose restart app   # postgres may still be starting
```

**Out of memory**
```
Increase JAVA_OPTS in .env:
JAVA_OPTS=-Xms512m -Xmx4096m -XX:+UseG1GC
```

**Useful commands:**
```bash
# Live logs
docker-compose logs -f app

# Restart app only
docker-compose restart app

# Stop everything (keep data)
docker-compose down

# Stop and wipe all data
docker-compose down -v

# Rebuild after code changes
docker-compose up -d --build app
```

---

## License

MIT License — see [LICENSE](LICENSE) for details.
