# AI Research Panel System (AI-RPS)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![CI](https://github.com/navinavi029/AI-Research-Panel-System-AIRPS/workflows/CI/badge.svg)](https://github.com/navinavi029/AI-Research-Panel-System-AIRPS/actions)
[![Coverage](https://img.shields.io/badge/coverage-87%25-brightgreen.svg)](https://github.com/navinavi029/AI-Research-Panel-System-AIRPS)

> **🐳 Docker-First Architecture**: This project runs entirely in Docker containers. No local Java, Maven, PostgreSQL, or other dependencies required. Just install Docker Desktop and you're ready to go!

A Java Spring Boot application that orchestrates multi-agent AI analysis of research documents using NVIDIA models.

## Overview

The AI Research Panel System (AI-RPS) enables users to upload PDF research documents and receive comprehensive analysis from multiple AI agents acting as a deliberative panel. The system leverages NVIDIA free models to power multi-agent analysis, where AI panelists independently review the study and then collaborate to produce a consensus output.

## Features

- PDF document upload and validation (up to 50MB, max 500 pages or 1M tokens)
- Text extraction with logical reading order preservation
- Intelligent chunking for large documents (100K token chunks with 500-token overlap)
- Multi-agent AI analysis with 6 specialized agents running in parallel
- Consensus generation from individual agent reports
- Asynchronous processing with real-time status tracking
- Comprehensive error handling with retry logic and circuit breaker
- Docker containerization for easy deployment

## Technology Stack

- **Java 21**
- **Spring Boot 3.2.1**
- **Spring Data JPA** with PostgreSQL 15
- **Apache PDFBox** for PDF processing
- **NVIDIA API** (llama-3.1-nemotron-70b-instruct)
- **Resilience4j** for circuit breaker pattern
- **jqwik** for property-based testing
- **Docker** and **Docker Compose** for containerization

## Prerequisites

**Only 2 things required:**

1. **Docker Desktop** - [Download here](https://www.docker.com/products/docker-desktop)
2. **NVIDIA API Key** (free) - [Get it here](https://build.nvidia.com/)

That's it! No Java, Maven, PostgreSQL, or any other local dependencies needed. Everything runs in Docker containers.

## Quick Start

**New to the system?** See [GETTING-STARTED.md](GETTING-STARTED.md) for a detailed step-by-step guide.

### 1. Prerequisites

- **Docker Desktop** - [Download here](https://www.docker.com/products/docker-desktop)
- **NVIDIA API Key** (free) - [Get it here](https://build.nvidia.com/)

### 2. Configure Environment

```bash
# Create environment file
cp .env.example .env

# Edit .env and add your NVIDIA API key
NVIDIA_API_KEY=your-api-key-here
```

### 3. Start the Application

**Windows:**
```cmd
docker-start.bat
```

**Linux/Mac:**
```bash
make up
```

### 4. Verify It's Working

Open your browser: http://localhost:8080/actuator/health

You should see `{"status":"UP"}`

### 5. Test the API

Visit the interactive API documentation:
```
http://localhost:8080/swagger-ui.html
```

See [API-GUIDE.md](API-GUIDE.md) for detailed API testing instructions.

---

## Documentation

📚 **[Complete Documentation Guide](.github/DOCUMENTATION.md)** - Visual guide to all documentation

- **[GETTING-STARTED.md](GETTING-STARTED.md)** - Complete setup guide for new users
- **[API-GUIDE.md](API-GUIDE.md)** - API endpoints and testing guide
- **[DOCKER-GUIDE.md](DOCKER-GUIDE.md)** - Docker deployment and management
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - How to contribute to the project
- **[SECURITY.md](SECURITY.md)** - Security policy and best practices

## Docker Architecture

The application runs in Docker containers with no local dependencies required. See [DOCKER-GUIDE.md](DOCKER-GUIDE.md) for comprehensive deployment documentation.

**Container Services:**
- **aipanelist-app** - Spring Boot application (port 8080)
- **aipanelist-postgres** - PostgreSQL 15 database
- **aipanelist-pgadmin** - Database UI (dev mode only)

**Deployment Modes:**
- **Production** - Optimized for production use
- **Development** - Hot reload, debugging, PgAdmin

## API Documentation

The system provides a RESTful API for document analysis. See [API-GUIDE.md](API-GUIDE.md) for detailed testing instructions.

**Base URL:** `http://localhost:8080/api`

**Interactive Documentation:** http://localhost:8080/swagger-ui.html

### Key Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/documents/upload` | POST | Upload PDF for analysis |
| `/api/documents/{id}/status` | GET | Check processing status |
| `/api/documents/{id}/results` | GET | Get consensus report |
| `/api/documents/{id}/results/detailed` | GET | Get all agent reports |

**Example Usage:**

```bash
# Upload a document
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@research-paper.pdf"

# Check status
curl http://localhost:8080/api/documents/{documentId}/status

# Get results
curl http://localhost:8080/api/documents/{documentId}/results
```

## AI Agents

The system employs 6 specialized AI agents that analyze documents in parallel:

1. **Lead Analyst**: Deep critical analysis and research validity assessment
2. **General Analyst**: Comprehensive document review and overall evaluation
3. **Methodology Reviewer**: Methodology and statistical analysis evaluation
4. **Literature Reviewer**: Literature context, citations, and positioning
5. **Quick Screener**: Initial screening and key claims identification
6. **Fact Extractor**: Fact extraction and summarization

Each agent processes the document independently, then their findings are synthesized into a consensus report.

## Configuration

All configuration is managed through the `.env` file. See `.env.example` for all available options.

**Required:**
```env
NVIDIA_API_KEY=your-api-key-here
```

**Optional (with defaults):**
```env
POSTGRES_PASSWORD=changeme
SERVER_PORT=8080
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=DEBUG
JAVA_OPTS=-Xms512m -Xmx2048m
```

For advanced configuration options, see [DOCKER-GUIDE.md](DOCKER-GUIDE.md).

## Building and Testing

All building and testing happens inside Docker containers.

**Build:**
```bash
docker-compose up --build
```

**Run Tests:**
```bash
docker-compose exec app mvn test
```

For detailed testing information, see [DOCKER-GUIDE.md](DOCKER-GUIDE.md).

## Architecture

### System Components

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ HTTP
       ▼
┌─────────────────────────────────────┐
│      DocumentController (API)       │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│    AsyncAnalysisService (Async)     │
└──────┬──────────────────────────────┘
       │
       ├──► PDFUploadService
       │
       ├──► DocumentProcessor (Extract)
       │
       ├──► ChunkProcessor (Chunk)
       │
       ├──► PanelOrchestrator
       │    └──► 6 AI Agents (Parallel)
       │         └──► NVIDIAModelClient
       │
       └──► ConsensusEngine
            └──► NVIDIAModelClient
```

### Processing Flow

1. **Upload**: User uploads PDF via REST API
2. **Validation**: File size, type, and structure validated
3. **Extraction**: Text extracted with reading order preserved
4. **Chunking**: Large documents split into 100K token chunks
5. **Analysis**: 6 agents analyze in parallel (5 min/chunk target)
6. **Consensus**: Findings synthesized into unified report (2 min target)
7. **Results**: Consensus report available via API

### Database Schema

- `documents`: Document metadata and status
- `extracted_documents`: Extracted text content
- `document_chunks`: Document chunks for large files
- `analysis_reports`: Individual agent reports
- `chunk_analyses`: Per-chunk analysis results
- `consensus_reports`: Final consensus reports
- `agent_progress`: Real-time agent progress tracking

## Error Handling

The system includes comprehensive error handling:

- **Retry Logic**: 3 attempts with exponential backoff for transient failures
- **Circuit Breaker**: Protects against cascading failures (50% threshold, 1-min open)
- **Graceful Degradation**: Continues with remaining agents if some fail
- **Detailed Errors**: Descriptive error messages with context
- **Structured Logging**: All errors logged with documentId, stage, and stack traces

## Performance

- **Target Processing Time**: 5 minutes per 100K token chunk per agent
- **Consensus Generation**: 2 minutes target
- **Parallel Processing**: 6 agents run concurrently
- **Connection Pooling**: Max 10 concurrent NVIDIA API connections
- **Rate Limiting**: 60 requests/minute (configurable)

## Troubleshooting

### Common Issues

**"NVIDIA API key not configured"**
- Ensure `NVIDIA_API_KEY` is set in `.env`
- Verify the key is valid at https://build.nvidia.com/

**"Database connection failed"**
- Wait 30-60 seconds for PostgreSQL to start
- Restart: `docker-compose restart app`

**"File upload failed"**
- Check file is a valid PDF
- Maximum size: 50MB, 500 pages

**View Logs:**
```bash
docker-compose logs -f app
```

For more troubleshooting help, see [DOCKER-GUIDE.md](DOCKER-GUIDE.md).

## Development

For development setup, debugging, and contributing to the codebase, see:
- **[DOCKER-GUIDE.md](DOCKER-GUIDE.md)** - Development mode setup
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - Contribution guidelines

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details on:

- How to report bugs
- How to suggest features
- Development setup
- Coding standards
- Pull request process

## Community

- **Issues**: [GitHub Issues](https://github.com/navinavi029/AI-Research-Panel-System-AIRPS/issues)
- **Discussions**: [GitHub Discussions](https://github.com/navinavi029/AI-Research-Panel-System-AIRPS/discussions)
- **Security**: See [SECURITY.md](SECURITY.md) for reporting vulnerabilities

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

Copyright © 2024 AI Research Panel System Contributors.

## Support

For issues, questions, or contributions, please contact the development team.
