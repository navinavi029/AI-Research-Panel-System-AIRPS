# Implementation Plan: AI Panelist System

## Overview

This implementation plan breaks down the AI Panelist System into discrete, testable tasks following Test-Driven Development (TDD) principles. The system is a Java Spring Boot application that orchestrates multi-agent AI analysis of research documents using NVIDIA models.

The implementation follows this sequence:
1. Project setup and core infrastructure
2. Data models and persistence layer
3. PDF upload and validation
4. Document processing and chunking
5. NVIDIA model client integration
6. AI agent implementation
7. Panel orchestration
8. Consensus generation
9. Results retrieval and status tracking
10. Error handling and resilience
11. Docker containerization
12. Integration and final testing

Each task references specific requirements for traceability. Tasks marked with `*` are optional testing tasks that can be skipped for faster MVP delivery.

## Tasks

- [x] 1. Set up project structure and core dependencies
  - Create Spring Boot project with Java 17
  - Add dependencies: Spring Web, Spring Data JPA, PostgreSQL, Apache PDFBox, Lombok, jqwik for property testing
  - Configure application.properties with database connection, file storage paths, and NVIDIA API configuration
  - Set up logging configuration with structured logging
  - Create base package structure: upload, processing, orchestration, agents, consensus, api, config
  - _Requirements: 8.5, 8.6_

- [x] 2. Implement data models and JPA entities
  - [x] 2.1 Create core entity classes
    - Implement Document entity with id, filename, fileSize, uploadedAt, status, errorMessage, totalTokens, totalPages
    - Implement ExtractedDocument entity with documentId, extractedText (LOB), tokenCount, extractedAt, readingOrderPreserved
    - Implement DocumentChunk entity with chunkId, documentId, sequenceNumber, totalChunks, chunkText (LOB), tokenCount, byte offsets, overlapTokens
    - Implement AnalysisReport entity with reportId, documentId, agentId, agentType, findings/strengths/weaknesses/recommendations (LOB), completedAt, chunk counts
    - Implement ChunkAnalysis entity with analysisId, reportId, chunkId, chunkSequence, findings (LOB), contextSummary (LOB), analyzedAt
    - Implement ConsensusReport entity with reportId, documentId, themes/agreements/disagreements/recommendations/insights (LOB), generatedAt, agentReportsIncluded
    - Implement AgentProgress entity with progressId, documentId, agentId, agentType, chunksCompleted, totalChunks, lastUpdated
    - Create ProcessingStatus enum: UPLOADED, PROCESSING, ANALYZING, DELIBERATING, COMPLETE, FAILED
    - Create AgentType enum: LEAD_ANALYST, GENERAL_ANALYST, METHODOLOGY_REVIEWER, LITERATURE_REVIEWER, QUICK_SCREENER, FACT_EXTRACTOR
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_

  - [ ]* 2.2 Write property test for data model persistence
    - **Property 9: Chunk Metadata Completeness**
    - **Validates: Requirements 2.10**

- [x] 3. Create Spring Data JPA repositories
  - Create DocumentRepository extending JpaRepository
  - Create ExtractedDocumentRepository with findByDocumentId method
  - Create DocumentChunkRepository with findByDocumentIdOrderBySequenceNumber method
  - Create AnalysisReportRepository with findByDocumentId method
  - Create ChunkAnalysisRepository with findByReportIdOrderByChunkSequence method
  - Create ConsensusReportRepository with findByDocumentId method
  - Create AgentProgressRepository with findByDocumentIdAndAgentId method
  - _Requirements: 2.3, 4.6, 6.6, 10.7_

- [ ] 4. Implement PDF Upload Service
  - [x] 4.1 Create PDFUploadService interface and implementation
    - Implement uploadDocument method accepting MultipartFile
    - Implement validatePDF method checking file size (≤50MB), MIME type (application/pdf), and PDF structure using PDFBox
    - Implement storeFile method saving to /app/data/uploads/{documentId}.pdf
    - Generate unique document IDs using UUID
    - Return DocumentUploadResponse with documentId, filename, status
    - Throw InvalidFileException for validation failures with specific error messages
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [ ]* 4.2 Write property tests for PDF upload validation
    - **Property 1: File Size Validation**
    - **Validates: Requirements 1.1, 1.3**

  - [ ]* 4.3 Write property test for non-PDF rejection
    - **Property 2: Non-PDF Rejection**
    - **Validates: Requirements 1.2**

  - [ ]* 4.4 Write property test for PDF format validation
    - **Property 3: PDF Format Validation**
    - **Validates: Requirements 1.4**

  - [ ]* 4.5 Write unit tests for PDF upload edge cases
    - Test empty PDF files
    - Test corrupted PDF files
    - Test files exactly at 50MB limit
    - Test non-PDF files with PDF extension
    - _Requirements: 1.2, 1.5_

- [ ] 5. Implement Document Processor
  - [x] 5.1 Create DocumentProcessor interface and implementation
    - Implement extractText method using Apache PDFBox PDFTextStripper
    - Configure PDFTextStripper to preserve logical reading order
    - Implement calculateTokenCount method (approximate: 1 token ≈ 4 characters)
    - Store ExtractedDocument entity with text, token count, and metadata
    - Validate document size limits: reject if >500 pages or >1,000,000 tokens
    - Handle extraction failures with ExtractionException and detailed error logging
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.11, 2.12_

  - [ ]* 5.2 Write property test for text extraction preservation
    - **Property 4: Text Extraction Preservation**
    - **Validates: Requirements 2.1, 2.3, 2.5**

  - [ ]* 5.3 Write property test for token count storage
    - **Property 11: Token Count Storage**
    - **Validates: Requirements 2.12**

  - [ ]* 5.4 Write property test for document size limit
    - **Property 10: Document Size Limit**
    - **Validates: Requirements 2.11**

  - [ ]* 5.5 Write unit tests for document processing edge cases
    - Test PDFs with only images (no text)
    - Test single-character documents
    - Test documents at exact size limits
    - Test extraction failure scenarios
    - _Requirements: 2.2, 2.4_

- [x] 6. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Implement Chunk Processor
  - [x] 7.1 Create ChunkProcessor interface and implementation
    - Implement chunkDocument method that checks if document exceeds 100,000 tokens
    - Implement identifyBoundaries method detecting section headers, paragraph breaks, sentence endings
    - Implement createChunk method with target size 100,000 tokens and 500-token overlap
    - Store DocumentChunk entities with sequence number, total chunks, byte offsets, token count
    - Use regex patterns for semantic boundary detection: section headers (^#+\s, ^[A-Z][^.]+:$), paragraphs (\n\n), sentences (\.[\s\n])
    - Ensure chunks split at semantic boundaries, prioritizing headers > paragraphs > sentences
    - _Requirements: 2.6, 2.7, 2.8, 2.9, 2.10_

  - [ ]* 7.2 Write property test for chunking trigger
    - **Property 5: Chunking Trigger**
    - **Validates: Requirements 2.6**

  - [ ]* 7.3 Write property test for chunk size constraint
    - **Property 6: Chunk Size Constraint**
    - **Validates: Requirements 2.7**

  - [ ]* 7.4 Write property test for semantic boundary splitting
    - **Property 7: Semantic Boundary Splitting**
    - **Validates: Requirements 2.8**

  - [ ]* 7.5 Write property test for chunk overlap
    - **Property 8: Chunk Overlap**
    - **Validates: Requirements 2.9**

  - [ ]* 7.6 Write unit tests for chunking edge cases
    - Test documents just under 100K tokens (no chunking)
    - Test documents just over 100K tokens (2 chunks)
    - Test documents with no clear semantic boundaries
    - Test very large documents (1M tokens)
    - _Requirements: 2.6, 2.7, 2.8_

- [x] 8. Implement NVIDIA Model Client
  - [x] 8.1 Create NVIDIAModelClient interface and implementation
    - Implement sendRequest method sending POST requests to nvidia/llama-3.1-nemotron-70b-instruct endpoint
    - Configure authentication using NVIDIA_API_KEY from environment variables
    - Implement connection pooling with max 10 concurrent connections using Apache HttpClient
    - Implement rate limiting with configurable requests per minute (default 60)
    - Implement retry logic: 3 attempts with exponential backoff (1s, 2s, 4s) for 5xx errors
    - Handle 429 rate limit responses using Retry-After header
    - Parse JSON responses and return ModelResponse objects
    - Return descriptive errors for 4xx responses and network failures
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7_

  - [x] 8.2 Write property test for model request routing
    - **Property 21: Model Request Routing**
    - **Validates: Requirements 5.3**

  - [ ]* 8.3 Write property test for response parsing
    - **Property 22: Response Parsing**
    - **Validates: Requirements 5.4**

  - [ ]* 8.4 Write property test for rate limiting
    - **Property 23: Rate Limiting**
    - **Validates: Requirements 5.5**

  - [ ]* 8.5 Write property test for API error handling
    - **Property 24: API Error Handling**
    - **Validates: Requirements 5.6**

  - [ ]* 8.6 Write property test for connection pooling
    - **Property 25: Connection Pooling**
    - **Validates: Requirements 5.7**

  - [ ]* 8.7 Write unit tests for NVIDIA client edge cases
    - Test API unavailability with retries
    - Test rate limiting with backoff
    - Test malformed API responses
    - Test network timeouts
    - _Requirements: 5.4, 5.5, 5.6_

- [x] 9. Implement Circuit Breaker for resilience
  - Add resilience4j dependency
  - Create CircuitBreaker configuration for NVIDIA API with 50% failure threshold, 1-minute open duration, 10-request sliding window
  - Wrap NVIDIAModelClient sendRequest method with circuit breaker
  - Return descriptive error when circuit is open
  - _Requirements: 9.4_

- [x] 10. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 11. Implement AI Agent base class and specializations
  - [x] 11.1 Create AIAgent abstract base class
    - Define abstract analyze method accepting List<DocumentChunk>
    - Define abstract getSystemPrompt method returning agent-specific prompt
    - Implement analyzeChunk method that calls NVIDIAModelClient with chunk text and previous analyses summary
    - Implement synthesizeChunkAnalyses method that combines all ChunkAnalysis into unified AnalysisReport
    - Implement retry logic: 3 attempts per chunk with exponential backoff
    - Handle chunk failures: continue with remaining chunks, note gaps in report
    - Store ChunkAnalysis entities for each processed chunk
    - Target 5 minutes per chunk processing time
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.7, 11.1, 11.2, 11.6, 11.8, 11.9_

  - [x] 11.2 Implement specialized agent classes
    - Create LeadAnalystAgent with system prompt for deep critical analysis and research validity
    - Create GeneralAnalystAgent with system prompt for comprehensive document review
    - Create MethodologyReviewerAgent with system prompt for methodology and statistical analysis
    - Create LiteratureReviewerAgent with system prompt for literature context and citations
    - Create QuickScreenerAgent with system prompt for initial screening and key claims
    - Create FactExtractorAgent with system prompt for fact extraction and summarization
    - Each agent includes specialization type in AnalysisReport metadata
    - _Requirements: 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 4.1.1, 4.1.2, 4.1.3, 4.1.4, 4.1.5, 4.1.6, 4.1.7_

  - [ ]* 11.3 Write property test for multi-chunk sequential processing
    - **Property 15: Multi-Chunk Sequential Processing**
    - **Validates: Requirements 4.2, 11.1**

  - [ ]* 11.4 Write property test for context carryover
    - **Property 16: Context Carryover**
    - **Validates: Requirements 4.3, 11.2**

  - [ ]* 11.5 Write property test for chunk synthesis
    - **Property 17: Chunk Synthesis**
    - **Validates: Requirements 4.4, 11.9**

  - [ ]* 11.6 Write property test for analysis report structure
    - **Property 18: Analysis Report Structure**
    - **Validates: Requirements 4.5, 4.1.7**

  - [ ]* 11.7 Write property test for report submission
    - **Property 19: Report Submission**
    - **Validates: Requirements 4.6**

  - [ ]* 11.8 Write property test for retry with exponential backoff
    - **Property 20: Retry with Exponential Backoff**
    - **Validates: Requirements 4.8, 11.6**

  - [ ]* 11.9 Write unit tests for agent edge cases
    - Test single-chunk document analysis
    - Test multi-chunk document with context carryover
    - Test chunk failure with continuation
    - Test all chunks failing
    - Test synthesis with contradictory findings
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 11.7_

- [x] 12. Implement Panel Orchestrator
  - [x] 12.1 Create PanelOrchestrator interface and implementation
    - Implement createPanel method that instantiates 6 specialized agents (one of each type)
    - Assign unique agent IDs using UUID
    - Configure each agent with NVIDIAModelClient instance
    - Implement orchestrateAnalysis method that submits analysis tasks to all agents in parallel using ExecutorService
    - Collect AnalysisReport from each agent with 30-minute timeout per agent
    - Handle agent failures: log error, continue with remaining agents, store failure information
    - Update AgentProgress entities as agents complete chunks
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 9.3, 10.9_

  - [ ]* 12.2 Write property test for panel composition
    - **Property 12: Panel Composition**
    - **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7**

  - [ ]* 12.3 Write property test for agent ID uniqueness
    - **Property 13: Agent ID Uniqueness**
    - **Validates: Requirements 3.8**

  - [ ]* 12.4 Write property test for agent API configuration
    - **Property 14: Agent API Configuration**
    - **Validates: Requirements 3.9**

  - [ ]* 12.5 Write property test for agent failure resilience
    - **Property 38: Agent Failure Resilience**
    - **Validates: Requirements 9.3, 11.7**

  - [ ]* 12.6 Write unit tests for orchestration edge cases
    - Test all agents succeed
    - Test one agent fails, others succeed
    - Test multiple agents fail
    - Test all agents fail
    - Test agent timeout scenarios
    - _Requirements: 3.1, 9.3_

- [x] 13. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 14. Implement Consensus Engine
  - [x] 14.1 Create ConsensusEngine interface and implementation
    - Implement generateConsensus method accepting List<AnalysisReport>
    - Build synthesis prompt including all agent reports with agent type labels
    - Call NVIDIAModelClient with nvidia/llama-3.1-nemotron-70b-instruct to identify themes, agreements, disagreements
    - Parse model response to extract common themes, agreements, disagreements, unified recommendations
    - Attribute specific insights to source agent types in final report
    - Store ConsensusReport entity with documentId
    - Target completion within 2 minutes
    - Handle consensus generation failures with descriptive errors
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8_

  - [ ]* 14.2 Write property test for consensus synthesis
    - **Property 26: Consensus Synthesis**
    - **Validates: Requirements 6.1**

  - [ ]* 14.3 Write property test for consensus report persistence
    - **Property 27: Consensus Report Persistence**
    - **Validates: Requirements 6.6**

  - [ ]* 14.4 Write property test for insight attribution
    - **Property 28: Insight Attribution**
    - **Validates: Requirements 6.8**

  - [ ]* 14.5 Write unit tests for consensus edge cases
    - Test consensus with all 6 agent reports
    - Test consensus with some agent failures
    - Test consensus with conflicting agent findings
    - Test consensus generation timeout
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [x] 15. Implement Document Status Service
  - [x] 15.1 Create DocumentStatusService interface and implementation
    - Implement updateStatus method updating Document entity status field
    - Implement updateChunkProgress method updating AgentProgress entities
    - Implement getStatus method returning DocumentStatus with current status, chunk progress, error messages
    - Calculate estimated time remaining based on average chunk processing time
    - Include per-agent progress showing chunks completed vs total chunks
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7, 10.8, 10.9_

  - [ ]* 15.2 Write property test for status lifecycle
    - **Property 40: Status Lifecycle**
    - **Validates: Requirements 10.1, 10.2, 10.3, 10.4, 10.5**

  - [ ]* 15.3 Write property test for failure status
    - **Property 41: Failure Status**
    - **Validates: Requirements 10.6**

  - [ ]* 15.4 Write property test for status retrieval
    - **Property 42: Status Retrieval**
    - **Validates: Requirements 10.7**

  - [ ]* 15.5 Write property test for chunk progress tracking
    - **Property 43: Chunk Progress Tracking**
    - **Validates: Requirements 10.8, 10.9**

  - [ ]* 15.6 Write unit tests for status tracking edge cases
    - Test status transitions for successful processing
    - Test status transitions with failures at different stages
    - Test chunk progress updates for multi-chunk documents
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_

- [x] 16. Implement Results Service
  - [x] 16.1 Create ResultsService interface and implementation
    - Implement getConsensusReport method querying ConsensusReportRepository
    - Implement getDetailedResults method querying both AnalysisReportRepository and ConsensusReportRepository
    - Implement formatAsJSON method serializing reports to JSON
    - Throw DocumentNotFoundException for non-existent document IDs
    - Return current status if document is still processing
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

  - [ ]* 16.2 Write property test for results retrieval by ID
    - **Property 29: Results Retrieval by ID**
    - **Validates: Requirements 7.1**

  - [ ]* 16.3 Write property test for non-existent document error
    - **Property 30: Non-Existent Document Error**
    - **Validates: Requirements 7.3**

  - [ ]* 16.4 Write property test for detailed results completeness
    - **Property 31: Detailed Results Completeness**
    - **Validates: Requirements 7.4**

  - [ ]* 16.5 Write property test for JSON output format
    - **Property 32: JSON Output Format**
    - **Validates: Requirements 7.5**

  - [ ]* 16.6 Write unit tests for results retrieval edge cases
    - Test retrieval of completed consensus report
    - Test retrieval during processing (status returned)
    - Test retrieval of non-existent document
    - Test detailed results with all agent reports
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [x] 17. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 18. Implement REST API Controller
  - [x] 18.1 Create DocumentController with REST endpoints
    - Implement POST /api/documents/upload endpoint accepting MultipartFile
    - Validate file size, type, and structure before calling PDFUploadService
    - Return 400 Bad Request with specific error messages for validation failures
    - Return 200 OK with DocumentUploadResponse containing documentId
    - Implement GET /api/documents/{documentId}/status endpoint
    - Return current DocumentStatus with processing stage and chunk progress
    - Implement GET /api/documents/{documentId}/results endpoint
    - Return ConsensusReport if complete, or current status if processing
    - Return 404 Not Found for non-existent document IDs
    - Implement GET /api/documents/{documentId}/results/detailed endpoint
    - Return all 6 AnalysisReports plus ConsensusReport
    - All responses in JSON format
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 9.5_

  - [ ]* 18.2 Write property test for input validation errors
    - **Property 39: Input Validation Errors**
    - **Validates: Requirements 9.5**

  - [ ]* 18.3 Write unit tests for API endpoints
    - Test upload endpoint with valid PDF
    - Test upload endpoint with invalid inputs
    - Test status endpoint for various processing stages
    - Test results endpoint for completed analysis
    - Test results endpoint for non-existent document
    - Test detailed results endpoint
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 19. Implement asynchronous processing orchestration
  - [x] 19.1 Create AsyncAnalysisService with @Async methods
    - Configure Spring @EnableAsync with dedicated thread pool (10 threads)
    - Implement processDocument method that orchestrates: extraction → chunking → panel analysis → consensus
    - Update document status at each stage: UPLOADED → PROCESSING → ANALYZING → DELIBERATING → COMPLETE
    - Handle errors at each stage: log error, update status to FAILED with error message, notify user
    - Call DocumentProcessor.extractText after upload
    - Call ChunkProcessor.chunkDocument after extraction
    - Call PanelOrchestrator.orchestrateAnalysis with chunks
    - Call ConsensusEngine.generateConsensus with agent reports
    - Store final ConsensusReport and update status to COMPLETE
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_

  - [ ]* 19.2 Write unit tests for async processing flow
    - Test successful end-to-end processing
    - Test failure at extraction stage
    - Test failure at chunking stage
    - Test failure at analysis stage
    - Test failure at consensus stage
    - Verify status updates at each stage
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_

- [x] 20. Implement comprehensive error handling
  - [x] 20.1 Create error handling infrastructure
    - Create custom exception classes: InvalidFileException, ExtractionException, APIException, DocumentNotFoundException, MaxRetriesExceededException, AllAgentsFailedException
    - Create ErrorResponse DTO with code, message, timestamp, documentId, details
    - Implement @ControllerAdvice global exception handler
    - Map exceptions to appropriate HTTP status codes and error responses
    - Implement structured logging with context (documentId, stage, errorType, retryAttempt)
    - Log all errors with stack traces and sufficient debugging detail
    - _Requirements: 9.1, 9.2, 9.5_

  - [ ]* 20.2 Write property test for error logging detail
    - **Property 36: Error Logging Detail**
    - **Validates: Requirements 9.1**

  - [ ]* 20.3 Write property test for analysis failure notification
    - **Property 37: Analysis Failure Notification**
    - **Validates: Requirements 9.2**

  - [ ]* 20.4 Write unit tests for error handling scenarios
    - Test each custom exception type
    - Test global exception handler mappings
    - Test error response format
    - Test logging output for various errors
    - _Requirements: 9.1, 9.2, 9.5_

- [x] 21. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 22. Implement Docker containerization
  - [x] 22.1 Create Dockerfile
    - Use openjdk:17-slim as base image
    - Copy JAR file to /app/app.jar
    - Create /app/data/uploads directory for file storage
    - Expose port 8080 (configurable via SERVER_PORT environment variable)
    - Set ENTRYPOINT to run Spring Boot application
    - Configure JVM options for container environment
    - _Requirements: 8.1, 8.2_

  - [x] 22.2 Create docker-compose.yml
    - Define app service using Dockerfile
    - Define postgres service using postgres:15 image
    - Configure environment variables: NVIDIA_API_KEY, NVIDIA_API_ENDPOINT, DATABASE_URL, SERVER_PORT
    - Mount volume for /app/data to persist uploaded documents and results
    - Configure health checks for both services
    - Set up network for service communication
    - _Requirements: 8.1, 8.2, 8.4, 8.5_

  - [x] 22.3 Configure application for Docker environment
    - Update application.properties to read from environment variables
    - Configure database connection using DATABASE_URL
    - Configure file storage path to use mounted volume
    - Configure logging to stdout for Docker log collection
    - Set initialization timeout to 30 seconds
    - _Requirements: 8.3, 8.4, 8.5, 8.6_

  - [ ]* 22.4 Write property test for file persistence to volume
    - **Property 33: File Persistence to Volume**
    - **Validates: Requirements 8.4**

  - [ ]* 22.5 Write property test for environment variable configuration
    - **Property 34: Environment Variable Configuration**
    - **Validates: Requirements 8.5**

  - [ ]* 22.6 Write property test for stdout logging
    - **Property 35: Stdout Logging**
    - **Validates: Requirements 8.6**

  - [ ]* 22.7 Write integration tests for Docker deployment
    - Test container startup within 30 seconds
    - Test volume mounting and file persistence
    - Test environment variable configuration
    - Test service communication (app to postgres)
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 23. Create README and deployment documentation
  - Document system architecture and components
  - Document API endpoints with request/response examples
  - Document environment variables and configuration options
  - Document Docker deployment steps: build image, run docker-compose, verify startup
  - Document NVIDIA API key setup
  - Document testing strategy and how to run tests
  - Include example curl commands for API usage
  - _Requirements: 8.1, 8.2, 8.5_

- [ ] 24. Integration testing and end-to-end validation
  - [x] 24.1 Create integration test suite using Testcontainers
    - Set up PostgreSQL container for integration tests
    - Create test PDFs with known content for validation
    - Test complete upload → extraction → chunking → analysis → consensus flow
    - Test multi-chunk document processing with context carryover
    - Test error scenarios: corrupted PDF, extraction failure, API unavailability
    - Test concurrent document processing
    - Verify all status transitions occur correctly
    - Verify final consensus report contains expected structure
    - _Requirements: All requirements_

  - [ ]* 24.2 Write end-to-end property tests
    - Test complete system behavior with generated test documents
    - Verify all correctness properties hold in integrated system
    - Test system resilience with random failures injected
    - _Requirements: All requirements_

- [x] 25. Final checkpoint - Ensure all tests pass
  - Run complete test suite: unit tests, property tests, integration tests
  - Verify all 43 correctness properties are tested
  - Verify test coverage meets goals: 80% line coverage, 75% branch coverage
  - Build Docker image and verify startup
  - Test API endpoints manually with sample PDFs
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional testing tasks that can be skipped for faster MVP delivery
- Each task references specific requirements for traceability
- Property-based tests use jqwik with minimum 100 iterations per property
- All property tests include comments referencing the design document property number
- Checkpoints ensure incremental validation and provide opportunities for user feedback
- The implementation follows TDD principles: write tests before implementation code
- NVIDIA API integration requires valid API key configured via environment variable
- Multi-chunk processing is critical for large documents and must maintain context across chunks
- Error handling and resilience are built into every component with retry logic and graceful degradation
- Docker containerization ensures consistent deployment across environments
