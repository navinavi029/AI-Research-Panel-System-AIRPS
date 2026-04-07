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

### 1. Get NVIDIA API Key

1. Visit https://build.nvidia.com/
2. Sign up for a free account
3. Navigate to API Keys section
4. Generate a new API key
5. Copy the key for configuration

### 2. Start with Docker (Recommended)

#### Windows Users - One Command Start

```cmd
docker-start.bat
```

This will:
- Check if Docker is running
- Create .env file from template
- Let you choose deployment mode (Production/Development/Production+Nginx)
- Start all services automatically

#### Linux/Mac Users - Using Make

```bash
# Production mode
make up

# Development mode (with hot reload and debugging)
make dev

# Production with Nginx (SSL/TLS ready)
make prod
```

#### Manual Docker Start

1. Create `.env` file from template:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` and set your NVIDIA API key:
   ```
   NVIDIA_API_KEY=your-api-key-here
   ```

3. Start services:
   ```bash
   # Production
   docker-compose up -d
   
   # Development (with hot reload)
   docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d
   
   # Production with Nginx
   docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
   ```

### 3. Access the Application

- **API**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **PgAdmin** (dev mode only): http://localhost:5050

### 4. Docker Management Commands

#### Windows Batch Files

```cmd
docker-start.bat   # Start services (interactive mode selection)
docker-stop.bat    # Stop services (with cleanup options)
docker-logs.bat    # View logs (with filtering)
docker-health.bat  # Check health and quick actions
```

#### Make Commands (Linux/Mac/Windows with Make)

```bash
make help          # Show all available commands
make status        # Show service status
make logs          # View logs
make health        # Check application health
make shell         # Open shell in app container
make db-shell      # Open PostgreSQL shell
make backup-db     # Backup database
make clean         # Remove everything
```

### 5. Development Mode Features

Development mode includes:
- **Hot reload** - Code changes automatically restart the app
- **Remote debugging** - Debug port 5005
- **PgAdmin** - Database management UI at http://localhost:5050
- **Verbose logging** - TRACE level for debugging

Start development mode:
```bash
# Windows
docker-start.bat  # Choose option 2

# Linux/Mac
make dev
```

### 6. Running Tests in Docker

```bash
# Run all tests inside Docker container
docker-compose exec app mvn test

# Or using Make
make test

# Run specific test
docker-compose exec app mvn test -Dtest=DocumentControllerTest
```

See [README-DOCKER.md](README-DOCKER.md) for comprehensive Docker deployment documentation.

## Docker Architecture

### Container Services

The application runs in a multi-container Docker environment:

1. **aipanelist-app** - Spring Boot application
   - Built with multi-stage Dockerfile
   - Runs on port 8080
   - Connects to PostgreSQL
   - Mounts volume for file uploads

2. **aipanelist-postgres** - PostgreSQL 15 database
   - Persistent data storage
   - Automatic initialization scripts
   - Health checks enabled
   - Optimized for production

3. **aipanelist-pgadmin** (dev mode only)
   - Database management UI
   - Accessible at http://localhost:5050
   - Default credentials in .env

4. **aipanelist-nginx** (prod mode only)
   - Reverse proxy
   - Rate limiting
   - SSL/TLS termination
   - Load balancing ready

### Docker Compose Profiles

Three deployment profiles are available:

#### 1. Production (Default)
```bash
docker-compose up -d
```
- Optimized JVM settings
- Minimal logging
- Resource limits enforced
- Auto-restart enabled

#### 2. Development
```bash
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```
- Hot reload enabled
- Remote debugging on port 5005
- PgAdmin included
- Verbose logging
- Source code mounted

#### 3. Production with Nginx
```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```
- Nginx reverse proxy
- Rate limiting
- SSL/TLS support
- Production PostgreSQL tuning
- Higher resource limits

### Volume Management

Persistent data is stored in Docker volumes:

- **postgres-data** - Database files
- **upload-data** - Uploaded PDF documents
- **pgadmin-data** - PgAdmin configuration (dev mode)

Backup database:
```bash
make backup-db
# or
docker-compose exec -T postgres pg_dump -U aipanelist aipanelist > backup.sql
```

Restore database:
```bash
make restore-db FILE=backup.sql
# or
docker-compose exec -T postgres psql -U aipanelist aipanelist < backup.sql
```

### Environment Variables

Configure via `.env` file:

```bash
# Required
NVIDIA_API_KEY=your-api-key-here

# Optional (with defaults)
POSTGRES_PASSWORD=changeme
SERVER_PORT=8080
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=DEBUG
JAVA_OPTS=-Xms512m -Xmx2048m
```

### Resource Limits

Default resource allocation:

**Production:**
- App: 2 CPU, 2GB RAM
- Database: 2 CPU, 2GB RAM
- Nginx: 0.5 CPU, 256MB RAM

**Development:**
- App: 1 CPU, 512MB RAM
- Database: 1 CPU, 512MB RAM

Adjust in docker-compose files as needed.

## API Documentation

### Base URL

```
http://localhost:8080/api
```

### Endpoints

#### 1. Upload Document

Upload a PDF document for analysis.

**Request:**
```http
POST /api/documents/upload
Content-Type: multipart/form-data

file: <PDF file>
```

**Response (200 OK):**
```json
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "filename": "research-paper.pdf",
  "status": "UPLOADED"
}
```

**Error Responses:**
- `400 Bad Request`: Invalid file (not PDF, too large, corrupted)
- `500 Internal Server Error`: Server error during upload

**Example:**
```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@research-paper.pdf"
```

#### 2. Get Document Status

Check the processing status of a document.

**Request:**
```http
GET /api/documents/{documentId}/status
```

**Response (200 OK):**
```json
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ANALYZING",
  "progress": {
    "totalChunks": 3,
    "agentProgress": [
      {
        "agentType": "LEAD_ANALYST",
        "chunksCompleted": 2,
        "totalChunks": 3
      },
      {
        "agentType": "METHODOLOGY_REVIEWER",
        "chunksCompleted": 1,
        "totalChunks": 3
      }
    ]
  },
  "estimatedTimeRemaining": "15 minutes"
}
```

**Status Values:**
- `UPLOADED`: Document uploaded, awaiting processing
- `PROCESSING`: Extracting text and chunking
- `ANALYZING`: AI agents analyzing document
- `DELIBERATING`: Generating consensus report
- `COMPLETE`: Analysis complete
- `FAILED`: Processing failed (check errorMessage)

**Error Responses:**
- `404 Not Found`: Document ID does not exist

**Example:**
```bash
curl http://localhost:8080/api/documents/550e8400-e29b-41d4-a716-446655440000/status
```

#### 3. Get Consensus Results

Retrieve the final consensus report.

**Request:**
```http
GET /api/documents/{documentId}/results
```

**Response (200 OK):**
```json
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "consensusReport": {
    "commonThemes": [
      "Strong methodology with appropriate statistical analysis",
      "Limited sample size may affect generalizability"
    ],
    "agreements": [
      "All agents agree the research question is well-defined",
      "Consensus on the validity of the experimental design"
    ],
    "disagreements": [
      "Lead Analyst questions the interpretation of results",
      "Literature Reviewer suggests additional context needed"
    ],
    "unifiedRecommendations": [
      "Consider expanding sample size in future studies",
      "Strengthen discussion of limitations"
    ],
    "keyInsights": [
      "Novel approach to data collection (Methodology Reviewer)",
      "Significant contribution to existing literature (Literature Reviewer)"
    ],
    "generatedAt": "2024-01-15T14:30:00Z"
  }
}
```

**Error Responses:**
- `404 Not Found`: Document ID does not exist
- `202 Accepted`: Document still processing (returns current status)

**Example:**
```bash
curl http://localhost:8080/api/documents/550e8400-e29b-41d4-a716-446655440000/results
```

#### 4. Get Detailed Results

Retrieve all individual agent reports plus consensus.

**Request:**
```http
GET /api/documents/{documentId}/results/detailed
```

**Response (200 OK):**
```json
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "agentReports": [
    {
      "agentType": "LEAD_ANALYST",
      "findings": "Comprehensive analysis of research methodology...",
      "strengths": ["Clear research question", "Appropriate methods"],
      "weaknesses": ["Limited sample size", "Potential bias"],
      "recommendations": ["Expand sample", "Address limitations"],
      "completedAt": "2024-01-15T14:25:00Z"
    },
    {
      "agentType": "METHODOLOGY_REVIEWER",
      "findings": "Statistical analysis review...",
      "strengths": ["Robust statistical tests", "Clear reporting"],
      "weaknesses": ["Missing power analysis"],
      "recommendations": ["Include power calculations"],
      "completedAt": "2024-01-15T14:26:00Z"
    }
    // ... 4 more agent reports
  ],
  "consensusReport": {
    // Same structure as /results endpoint
  }
}
```

**Error Responses:**
- `404 Not Found`: Document ID does not exist
- `202 Accepted`: Document still processing

**Example:**
```bash
curl http://localhost:8080/api/documents/550e8400-e29b-41d4-a716-446655440000/results/detailed
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

### Environment Variables

See `.env.example` for all available configuration options.

**Required:**
- `NVIDIA_API_KEY`: Your NVIDIA API key

**Database:**
- `DATABASE_URL`: PostgreSQL connection URL
- `DATABASE_USERNAME`: Database username
- `DATABASE_PASSWORD`: Database password

**Storage:**
- `UPLOAD_DIR`: Directory for uploaded files (default: /app/data/uploads)

**Processing:**
- `MAX_CHUNK_SIZE_TOKENS`: Maximum tokens per chunk (default: 100000)
- `CHUNK_OVERLAP_TOKENS`: Overlap between chunks (default: 500)
- `MAX_DOCUMENT_PAGES`: Maximum pages per document (default: 500)
- `MAX_DOCUMENT_TOKENS`: Maximum tokens per document (default: 1000000)

**Async Processing:**
- `ASYNC_THREAD_POOL_SIZE`: Thread pool size (default: 10)
- `AGENT_TIMEOUT_MINUTES`: Timeout per agent (default: 30)

**Logging:**
- `LOG_LEVEL`: Logging level (default: INFO)

### Application Properties

Configuration is in `src/main/resources/application.properties`. All values can be overridden with environment variables.

## Building and Testing

All building and testing happens inside Docker containers - no local Java or Maven installation required.

### Build

```bash
# Build is automatic when starting Docker
docker-compose up --build

# Force rebuild
docker-compose build --no-cache

# Or using Make
make rebuild
```

### Testing

```bash
# Run all tests in Docker container
docker-compose exec app mvn test

# Or using Make
make test

# Run specific test class
docker-compose exec app mvn test -Dtest=DocumentControllerTest

# Run with coverage
docker-compose exec app mvn test jacoco:report

# View coverage report
docker-compose exec app cat target/site/jacoco/index.html
```

**Test Coverage:**
- 152 tests passing (146 unit + 6 integration)
- Unit tests for all components
- Integration tests with Testcontainers
- Property-based tests with jqwik

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

**1. "NVIDIA API key not configured"**
- Ensure `NVIDIA_API_KEY` environment variable is set
- Verify API key is valid at https://build.nvidia.com/

**2. "Database connection failed"**
- Check PostgreSQL is running
- Verify `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- Ensure database `aipanelist` exists

**3. "File upload failed: File too large"**
- Maximum file size is 50MB
- Maximum document size is 500 pages or 1M tokens

**4. "Circuit breaker is OPEN"**
- NVIDIA API is experiencing issues
- Wait 1 minute for circuit breaker to reset
- Check NVIDIA API status

**5. "All agents failed"**
- Check NVIDIA API connectivity
- Review logs for specific agent errors
- Verify API rate limits not exceeded

### Logs

```bash
# View application logs (Docker)
docker-compose logs -f app

# View specific service logs
docker-compose logs -f postgres

# View logs with timestamps
docker-compose logs -f --timestamps app
```

## Development

### Project Structure

```
src/
├── main/
│   ├── java/com/aipanelist/
│   │   ├── agents/              # AI agent implementations
│   │   ├── api/                 # REST controllers and DTOs
│   │   ├── async/               # Async processing service
│   │   ├── config/              # Spring configuration
│   │   ├── consensus/           # Consensus generation
│   │   ├── integration/         # NVIDIA API client
│   │   ├── model/               # Domain models and entities
│   │   ├── orchestration/       # Panel orchestration
│   │   ├── processing/          # Document processing
│   │   ├── repository/          # JPA repositories
│   │   └── upload/              # PDF upload service
│   └── resources/
│       ├── application.properties
│       └── logback-spring.xml
└── test/
    └── java/com/aipanelist/     # Unit and integration tests
```

### Adding New Agents

To add a new specialized agent:

1. Create new class extending `AIAgent`
2. Implement `getSystemPrompt()` with agent-specific instructions
3. Add new `AgentType` enum value
4. Register agent in `PanelOrchestrator.createPanel()`

### Testing Strategy

- **Unit Tests**: Test individual components in isolation
- **Integration Tests**: Test component interactions with Testcontainers
- **Property Tests**: Test invariants with jqwik (100+ iterations)
- **TDD Approach**: Write tests before implementation

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
