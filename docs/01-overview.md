# Overview

## What is AI Research Panel System?

The AI Research Panel System (AI-RPS) is a Spring Boot application that automates the peer review process for research documents. You upload a PDF and the system dispatches 6 specialized AI agents — each with a distinct analytical lens — to analyze the document in parallel. Their findings are then synthesized by a consensus engine into a single unified report, available as JSON or a professionally styled PDF.

The system is designed to be zero-dependency from the user's perspective. Everything runs inside Docker containers. You need Docker Desktop and a free NVIDIA API key — nothing else.

---

## How It Works

```
You upload a PDF
        │
        ▼
Text is extracted and chunked
        │
        ▼
6 AI agents analyze in parallel
        │
        ▼
Consensus engine synthesizes findings
        │
        ▼
Report available as JSON or PDF
```

Processing is fully asynchronous. After uploading, you poll the status endpoint until the document reaches `COMPLETE`, then retrieve the results.

---

## Key Capabilities

| Capability | Detail |
|------------|--------|
| Document size | Up to 50MB, 500 pages, 1,000,000 tokens |
| Agents | 6 specialized agents running in parallel |
| Chunking | Semantic chunking at 100K tokens with 500-token overlap |
| Retry logic | 3 attempts per chunk with exponential backoff |
| Output formats | JSON (consensus + detailed) and styled PDF |
| Model | `meta/llama-3.3-70b-instruct` via NVIDIA API |

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.2.1 |
| Database | PostgreSQL 15 |
| ORM | Spring Data JPA / Hibernate |
| PDF reading | Apache PDFBox 3.0.1 |
| PDF generation | iText7 |
| HTTP client | Apache HttpClient 5 |
| Resilience | Resilience4j (circuit breaker) |
| API docs | SpringDoc OpenAPI / Swagger UI |
| Containerization | Docker + Docker Compose |

---

## Documentation Pages

| Page | Description |
|------|-------------|
| [Getting Started](02-getting-started.md) | Prerequisites, setup, and first run |
| [Configuration](03-configuration.md) | All environment variables and settings |
| [API Reference](04-api-reference.md) | All endpoints with request/response details |
| [Processing Pipeline](05-pipeline.md) | How documents are processed end to end |
| [AI Agents](06-agents.md) | All 6 agents and their specializations |
| [Consensus Engine](07-consensus.md) | How agent reports are synthesized |
| [PDF Export](08-pdf-export.md) | PDF report structure and styling |
| [Architecture](09-architecture.md) | System design and component overview |
| [Database Schema](10-database.md) | Tables, entities, and relationships |
| [Error Handling](11-error-handling.md) | Error codes, retry behavior, resilience |
| [Docker Management](12-docker.md) | Container management and useful commands |
| [Troubleshooting](13-troubleshooting.md) | Common issues and fixes |
