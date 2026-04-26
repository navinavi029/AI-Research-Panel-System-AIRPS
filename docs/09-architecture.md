# Architecture

---

## System Overview

```
┌──────────────────────────────────────────────────────────────────┐
│                        Docker Network                            │
│                                                                  │
│  ┌──────────────────────────────┐   ┌────────────────────────┐  │
│  │       aipanelist-app         │   │  aipanelist-postgres   │  │
│  │      Spring Boot :8080       │──▶│    PostgreSQL 15       │  │
│  │                              │   │       :5432            │  │
│  │  REST Layer                  │   └────────────────────────┘  │
│  │  ├─ DocumentController       │                               │
│  │                              │                               │
│  │  Upload Layer                │                               │
│  │  ├─ PDFUploadService         │                               │
│  │                              │                               │
│  │  Async Pipeline              │                               │
│  │  ├─ AsyncAnalysisService     │                               │
│  │  ├─ DocumentProcessor        │                               │
│  │  ├─ ChunkProcessor           │                               │
│  │  ├─ PanelOrchestrator        │──▶  NVIDIA API (external)    │
│  │  │  ├─ LeadAnalystAgent      │     integrate.api.nvidia.com  │
│  │  │  ├─ GeneralAnalystAgent   │                               │
│  │  │  ├─ MethodologyReviewer   │                               │
│  │  │  ├─ LiteratureReviewer    │                               │
│  │  │  ├─ QuickScreener         │                               │
│  │  │  └─ FactExtractor         │                               │
│  │  ├─ ConsensusEngine          │                               │
│  │                              │                               │
│  │  Export Layer                │                               │
│  │  └─ PDFExportService         │                               │
│  └──────────────────────────────┘                               │
└──────────────────────────────────────────────────────────────────┘
```

---

## Component Responsibilities

### REST Layer

| Component | Responsibility |
|-----------|---------------|
| `DocumentController` | Handles all HTTP requests. Validates inputs, delegates to services, formats responses. |
| `GlobalExceptionHandler` | `@ControllerAdvice` that catches all exceptions and returns consistent `ErrorResponse` JSON. |

### Upload Layer

| Component | Responsibility |
|-----------|---------------|
| `PDFUploadService` | Validates file (size, MIME type, PDF structure via PDFBox). Stores file to disk. Creates `Document` entity. |

### Async Pipeline

| Component | Responsibility |
|-----------|---------------|
| `AsyncAnalysisService` | Orchestrates the full pipeline in a background thread. Updates document status at each stage. Handles top-level errors. |
| `DocumentProcessor` | Loads PDF with PDFBox. Extracts text with `setSortByPosition(true)`. Validates page/token limits. Stores `ExtractedDocument`. |
| `ChunkProcessor` | Splits extracted text into semantic chunks at 100K token boundaries with 500-token overlap. Stores `DocumentChunk` entities. |
| `PanelOrchestrator` | Creates 6 agent instances. Submits all to `ExecutorService`. Collects results with 30-minute timeout. |
| `AIAgent` (×6) | Base class for all agents. Processes chunks sequentially with context carryover. Retries on failure. Synthesizes `AnalysisReport`. |
| `NVIDIAModelClient` | HTTP client for NVIDIA API. Manages connection pool, rate limiting, retries, and circuit breaker. |
| `ConsensusEngine` | Collects all `AnalysisReport` entities. Calls NVIDIA model to synthesize. Parses response into 5 sections. Stores `ConsensusReport`. |

### Export Layer

| Component | Responsibility |
|-----------|---------------|
| `PDFExportService` | Interface for PDF generation. |
| `StyledPDFExportServiceImpl` | `@Primary` implementation using iText7. Generates multi-page styled PDF with cover, TOC, sections, and footers. |
| `PDFExportServiceImpl` | Fallback implementation using Apache PDFBox. Not used by default. |

---

## NVIDIA API Client

The `NVIDIAModelClientImpl` is the single point of contact with the NVIDIA API. It handles all resilience concerns so the agents don't need to.

```
Agent calls modelClient.sendRequest(request)
        │
        ▼
Circuit Breaker check (Resilience4j)
        │
        ▼
Rate Limiter (Semaphore, 60 req/min)
        │
        ▼
HTTP POST to NVIDIA API
(PoolingHttpClientConnectionManager, max 10 connections)
(30s connection timeout, 300s response timeout)
        │
        ├── 2xx → parse and return ModelResponse
        ├── 429 → read Retry-After header, wait, retry
        ├── 5xx → retry with exponential backoff (up to 3×)
        └── 4xx → throw APIException (no retry)
```

**Circuit breaker configuration (Resilience4j):**

| Setting | Value |
|---------|-------|
| Failure rate threshold | 50% |
| Sliding window size | 10 calls |
| Minimum calls before evaluation | 5 |
| Open state duration | 60 seconds |
| Half-open permitted calls | 3 |

---

## Package Structure

```
com.aipanelist
├── AIPanelistApplication.java       Entry point
├── agents/                          AI agent implementations
│   ├── AIAgent.java                 Abstract base class
│   ├── LeadAnalystAgent.java
│   ├── GeneralAnalystAgent.java
│   ├── MethodologyReviewerAgent.java
│   ├── LiteratureReviewerAgent.java
│   ├── QuickScreenerAgent.java
│   └── FactExtractorAgent.java
├── api/                             REST layer
│   ├── DocumentController.java
│   ├── GlobalExceptionHandler.java
│   ├── ResultsService.java
│   ├── ResultsServiceImpl.java
│   ├── DetailedResultsDTO.java
│   ├── ErrorResponse.java
│   ├── DocumentNotFoundException.java
│   └── DocumentStillProcessingException.java
├── async/                           Async pipeline orchestration
│   ├── AsyncAnalysisService.java
│   └── AsyncAnalysisServiceImpl.java
├── config/                          Spring configuration
│   ├── AsyncConfiguration.java
│   ├── NVIDIAConfiguration.java
│   ├── OpenApiConfiguration.java
│   ├── ProcessingConfiguration.java
│   └── StorageConfiguration.java
├── consensus/                       Consensus engine
│   ├── ConsensusEngine.java
│   ├── ConsensusEngineImpl.java
│   └── ConsensusGenerationException.java
├── export/                          PDF export
│   ├── PDFExportService.java
│   ├── StyledPDFExportServiceImpl.java
│   ├── PDFExportServiceImpl.java
│   └── PDFGenerationException.java
├── integration/                     NVIDIA API client
│   ├── NVIDIAModelClient.java
│   └── NVIDIAModelClientImpl.java
├── model/                           JPA entities and domain models
│   ├── Document.java
│   ├── ExtractedDocument.java
│   ├── DocumentChunk.java
│   ├── AnalysisReport.java
│   ├── ChunkAnalysis.java
│   ├── ConsensusReport.java
│   ├── AgentProgress.java
│   ├── AgentType.java
│   ├── ProcessingStatus.java
│   ├── ModelRequest.java
│   ├── ModelResponse.java
│   └── [exception classes]
├── orchestration/                   Panel orchestration
│   ├── AnalysisPanel.java
│   ├── PanelOrchestrator.java
│   └── PanelOrchestratorImpl.java
├── processing/                      Document processing
│   ├── DocumentProcessor.java
│   ├── DocumentProcessorImpl.java
│   ├── ChunkProcessor.java
│   ├── ChunkProcessorImpl.java
│   ├── DocumentStatus.java
│   ├── DocumentStatusService.java
│   └── DocumentStatusServiceImpl.java
├── repository/                      Spring Data JPA repositories
│   ├── DocumentRepository.java
│   ├── ExtractedDocumentRepository.java
│   ├── DocumentChunkRepository.java
│   ├── AnalysisReportRepository.java
│   ├── ChunkAnalysisRepository.java
│   ├── ConsensusReportRepository.java
│   └── AgentProgressRepository.java
└── upload/                          File upload handling
    ├── PDFUploadService.java
    ├── PDFUploadServiceImpl.java
    ├── DocumentUploadResponse.java
    └── InvalidFileException.java
```
