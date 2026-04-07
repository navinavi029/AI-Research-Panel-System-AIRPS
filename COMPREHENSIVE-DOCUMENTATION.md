# AI Research Panel System (AI-RPS)

## Executive Summary

The **AI Research Panel System (AI-RPS)** is an enterprise-grade Java Spring Boot application that orchestrates sophisticated multi-agent artificial intelligence analysis of scholarly research documents. This system leverages NVIDIA's state-of-the-art language models to power a deliberative panel of six specialized AI agents, each bringing unique analytical perspectives to comprehensively evaluate research papers, methodologies, and academic studies.

The framework implements a microservices-inspired architecture within a containerized monolithic application, featuring asynchronous processing, intelligent document chunking, resilient error handling, and consensus-driven synthesis. AI-RPS transforms the traditional peer review process by providing rapid, comprehensive, multi-perspective analysis that would typically require weeks of expert review time.

## Table of Contents

1. [System Overview](#system-overview)
2. [Core Architecture](#core-architecture)
3. [The Six-Panel Agent System](#the-six-panel-agent-system)
4. [Document Processing Pipeline](#document-processing-pipeline)
5. [Intelligent Chunking Strategy](#intelligent-chunking-strategy)
6. [Multi-Agent Orchestration](#multi-agent-orchestration)
7. [Consensus Generation Engine](#consensus-generation-engine)
8. [API Reference](#api-reference)
9. [Deployment Architecture](#deployment-architecture)
10. [Configuration Management](#configuration-management)
11. [Error Handling & Resilience](#error-handling--resilience)
12. [Performance Characteristics](#performance-characteristics)
13. [Security Considerations](#security-considerations)
14. [Testing Strategy](#testing-strategy)
15. [Troubleshooting Guide](#troubleshooting-guide)
16. [Development Guidelines](#development-guidelines)
17. [Future Enhancements](#future-enhancements)

---

## System Overview

### Purpose and Vision

MACIF-SARDACG addresses the critical challenge of comprehensive academic research evaluation by providing:

- **Multi-Perspective Analysis**: Six specialized AI agents examine research from complementary angles
- **Scalable Processing**: Handles documents from 1 page to 500 pages (up to 1M tokens)
- **Rapid Turnaround**: Complete analysis in 30-45 minutes vs. weeks for traditional review
- **Consensus-Driven Insights**: Synthesizes diverse perspectives into actionable recommendations
- **Production-Ready Infrastructure**: Docker-containerized, horizontally scalable, enterprise-grade


### Key Capabilities

#### Document Processing
- **Format Support**: PDF documents up to 50MB
- **Text Extraction**: Apache PDFBox-powered extraction with reading order preservation
- **Intelligent Chunking**: Semantic boundary-aware splitting for documents exceeding 100K tokens
- **Context Preservation**: 500-token overlap between chunks maintains analytical continuity

#### AI-Powered Analysis
- **Six Specialized Agents**: Each powered by NVIDIA Llama 3.1 Nemotron 70B Instruct
- **Parallel Processing**: Concurrent agent execution for optimal throughput
- **Sequential Chunk Analysis**: Context-aware processing of large documents
- **Synthesis & Integration**: Cross-chunk theme identification and coherence maintenance

#### Consensus Generation
- **Theme Identification**: Common patterns across all agent analyses
- **Agreement Mapping**: Areas of unanimous or majority consensus
- **Disagreement Highlighting**: Divergent perspectives and their rationale
- **Unified Recommendations**: Actionable insights synthesized from panel deliberation
- **Attribution Tracking**: Source agent identification for specific insights

#### Enterprise Features
- **Asynchronous Processing**: Non-blocking analysis with real-time status tracking
- **Resilient Architecture**: Retry logic, circuit breakers, graceful degradation
- **Docker Containerization**: Consistent deployment across environments
- **RESTful API**: Standard HTTP endpoints for integration
- **Comprehensive Logging**: Structured logging for observability and debugging

---

## Core Architecture

### High-Level System Design

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client Layer                             │
│  (Web UI, CLI, API Consumers, Integration Partners)             │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTPS/REST
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      API Gateway Layer                           │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  DocumentController (Spring REST)                         │  │
│  │  - Upload Endpoint                                        │  │
│  │  - Status Endpoint                                        │  │
│  │  - Results Endpoint (Standard & Detailed)                │  │
│  │  - Global Exception Handler                              │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Asynchronous Processing Layer                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  AsyncAnalysisService (@Async)                           │  │
│  │  - Thread Pool Management (10 threads)                   │  │
│  │  - Non-blocking Execution                                │  │
│  │  - Status Tracking                                       │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Document Processing Layer                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐ │
│  │ PDFUpload    │→ │ Document     │→ │ Chunk                │ │
│  │ Service      │  │ Processor    │  │ Processor            │ │
│  │              │  │              │  │                      │ │
│  │ - Validation │  │ - Extraction │  │ - Semantic Splitting │ │
│  │ - Storage    │  │ - Token Count│  │ - Overlap Management │ │
│  └──────────────┘  └──────────────┘  └──────────────────────┘ │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Orchestration Layer                            │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  PanelOrchestrator                                       │  │
│  │  - Agent Creation & Configuration                        │  │
│  │  - Parallel Execution Management                         │  │
│  │  - Report Collection                                     │  │
│  │  - Failure Handling                                      │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Agent Layer (6 Agents)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐ │
│  │ Lead         │  │ General      │  │ Methodology          │ │
│  │ Analyst      │  │ Analyst      │  │ Reviewer             │ │
│  └──────────────┘  └──────────────┘  └──────────────────────┘ │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐ │
│  │ Literature   │  │ Quick        │  │ Fact                 │ │
│  │ Reviewer     │  │ Screener     │  │ Extractor            │ │
│  └──────────────┘  └──────────────┘  └──────────────────────┘ │
│                                                                  │
│  Each Agent:                                                     │
│  - Sequential Chunk Processing                                  │
│  - Context Carryover                                            │
│  - Chunk Analysis Synthesis                                     │
│  - Specialized System Prompts                                   │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Integration Layer                              │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  NVIDIAModelClient                                       │  │
│  │  - API Authentication                                    │  │
│  │  - Request/Response Handling                             │  │
│  │  - Connection Pooling (Max 10)                           │  │
│  │  - Rate Limiting (60 req/min)                            │  │
│  │  - Retry Logic (3 attempts, exponential backoff)         │  │
│  │  - Circuit Breaker (50% threshold, 1-min open)           │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTPS
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   External Services                              │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  NVIDIA API                                              │  │
│  │  Model: nvidia/llama-3.1-nemotron-70b-instruct          │  │
│  │  Context Window: 128K tokens                             │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Consensus Layer                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  ConsensusEngine                                         │  │
│  │  - Theme Identification                                  │  │
│  │  - Agreement/Disagreement Analysis                       │  │
│  │  - Unified Recommendation Generation                     │  │
│  │  - Insight Attribution                                   │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Persistence Layer                              │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  PostgreSQL 15 Database                                  │  │
│  │  - documents                                             │  │
│  │  - extracted_documents                                   │  │
│  │  - document_chunks                                       │  │
│  │  - analysis_reports                                      │  │
│  │  - chunk_analyses                                        │  │
│  │  - consensus_reports                                     │  │
│  │  - agent_progress                                        │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  File Storage (Docker Volume)                            │  │
│  │  - Uploaded PDFs                                         │  │
│  │  - Generated Reports                                     │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

#### API Gateway Layer
- **DocumentController**: Exposes RESTful endpoints for document upload, status queries, and results retrieval
- **GlobalExceptionHandler**: Centralized exception handling with structured error responses
- **Request Validation**: Input sanitization and validation at the API boundary

#### Asynchronous Processing Layer
- **AsyncAnalysisService**: Manages long-running analysis tasks in background threads
- **Thread Pool**: Configurable executor service (default: 10 threads)
- **Status Management**: Real-time progress tracking and state updates

#### Document Processing Layer
- **PDFUploadService**: File validation, storage, and metadata extraction
- **DocumentProcessor**: Text extraction using Apache PDFBox with reading order preservation
- **ChunkProcessor**: Semantic boundary-aware document segmentation

#### Orchestration Layer
- **PanelOrchestrator**: Creates agent panels, coordinates parallel execution, collects reports
- **Agent Lifecycle Management**: Initialization, execution, timeout handling, failure recovery

#### Agent Layer
- **Six Specialized Agents**: Independent analysis with unique perspectives
- **Sequential Processing**: Chunk-by-chunk analysis with context carryover
- **Synthesis Logic**: Integration of chunk-level analyses into unified reports

#### Integration Layer
- **NVIDIAModelClient**: HTTP client for NVIDIA API communication
- **Resilience Patterns**: Retry logic, circuit breaker, rate limiting, connection pooling

#### Consensus Layer
- **ConsensusEngine**: Synthesizes individual agent reports using AI-powered analysis
- **Theme Extraction**: Identifies common patterns across agent perspectives
- **Conflict Resolution**: Highlights agreements and disagreements with attribution

#### Persistence Layer
- **PostgreSQL Database**: Relational storage for structured data
- **File Storage**: Docker volume for binary file persistence
- **JPA Repositories**: Spring Data JPA for database access

---

## The Six-Panel Agent System

### Agent Architecture

Each agent in MACIF-SARDACG is an instance of the `AIAgent` abstract base class, specialized through:
- **Unique System Prompts**: Tailored instructions defining analytical focus
- **Agent Type Identification**: Enum-based classification for attribution
- **Shared Processing Logic**: Common chunking, synthesis, and error handling

### Agent Specializations


#### 1. Lead Analyst Agent

**Role**: Chief Evaluator and Critical Assessor

**Analytical Focus**:
- Overall research quality and validity
- Critical evaluation of claims and conclusions
- Identification of logical fallacies or methodological flaws
- Assessment of contribution to the field
- Evaluation of research significance and impact potential

**System Prompt Characteristics**:
- Emphasis on critical thinking and skepticism
- Focus on research rigor and scientific validity
- Attention to overgeneralizations and unsupported claims
- Evaluation of novelty and originality

**Typical Output Sections**:
- Executive Summary of Research Quality
- Critical Assessment of Core Claims
- Methodological Strengths and Weaknesses
- Significance and Impact Evaluation
- Recommendations for Improvement

---

#### 2. General Analyst Agent

**Role**: Comprehensive Reviewer

**Analytical Focus**:
- Holistic document review covering all aspects
- Structure and organization assessment
- Clarity and coherence evaluation
- Completeness of argumentation
- Balance between different sections

**System Prompt Characteristics**:
- Broad analytical scope without specialization bias
- Attention to document structure and flow
- Evaluation of writing quality and clarity
- Assessment of logical progression

**Typical Output Sections**:
- Overall Document Assessment
- Structural Analysis
- Content Completeness Review
- Clarity and Coherence Evaluation
- General Recommendations

---

#### 3. Methodology Reviewer Agent

**Role**: Methods and Statistics Specialist

**Analytical Focus**:
- Research design appropriateness
- Statistical methods and analysis validity
- Data collection procedures
- Sample size and power analysis
- Control of confounding variables
- Reproducibility and replicability

**System Prompt Characteristics**:
- Deep focus on methodological rigor
- Statistical literacy and technical expertise
- Attention to experimental design principles
- Evaluation of data quality and integrity

**Typical Output Sections**:
- Research Design Evaluation
- Statistical Methods Assessment
- Data Collection and Quality Review
- Validity and Reliability Analysis
- Methodological Recommendations

---

#### 4. Literature Reviewer Agent

**Role**: Contextual and Theoretical Framework Specialist

**Analytical Focus**:
- Literature review comprehensiveness
- Theoretical framework appropriateness
- Citation quality and relevance
- Positioning within existing research
- Identification of gaps in literature coverage
- Assessment of theoretical contributions

**System Prompt Characteristics**:
- Focus on scholarly context and positioning
- Evaluation of citation practices
- Assessment of theoretical grounding
- Attention to literature gaps

**Typical Output Sections**:
- Literature Review Assessment
- Theoretical Framework Evaluation
- Citation Analysis
- Research Positioning
- Contextual Recommendations

---

#### 5. Quick Screener Agent

**Role**: Initial Assessment and Key Claims Identifier

**Analytical Focus**:
- Rapid identification of core arguments
- Key claims extraction
- Initial quality screening
- Focused analysis of specific aspects
- Red flag identification

**System Prompt Characteristics**:
- Efficiency-oriented analysis
- Focus on essential elements
- Quick pattern recognition
- Prioritization of critical issues

**Typical Output Sections**:
- Core Claims Summary
- Initial Quality Assessment
- Key Strengths Identified
- Critical Issues Flagged
- Focused Recommendations

---

#### 6. Fact Extractor Agent

**Role**: Data and Information Synthesizer

**Analytical Focus**:
- Extraction of key facts and data points
- Identification of quantitative findings
- Structured summarization
- Data accuracy verification
- Creation of factual summaries

**System Prompt Characteristics**:
- Emphasis on factual accuracy
- Structured data extraction
- Quantitative focus
- Objective summarization

**Typical Output Sections**:
- Key Facts and Figures
- Quantitative Findings Summary
- Data Points Extraction
- Structured Information Summary
- Factual Accuracy Assessment

---

### Agent Interaction Model

#### Parallel Execution
- All six agents analyze the document simultaneously
- No inter-agent communication during analysis phase
- Independent perspectives ensure diversity of insights
- Orchestrator manages concurrent execution with timeout controls

#### Sequential Chunk Processing (Per Agent)
```
For each agent:
  For chunk 1:
    Analyze(chunk_1_text)
    Store(chunk_1_analysis)
  
  For chunk 2:
    context = Summarize(chunk_1_analysis)
    Analyze(chunk_2_text + context)
    Store(chunk_2_analysis)
  
  For chunk N:
    context = Summarize(chunk_1_analysis, ..., chunk_N-1_analysis)
    Analyze(chunk_N_text + context)
    Store(chunk_N_analysis)
  
  Synthesize(all_chunk_analyses) → final_agent_report
```

#### Context Carryover Strategy
- **Chunk 1**: Analyzed with no prior context
- **Chunk 2+**: Includes 200-300 word summary of previous findings
- **Synthesis**: Integrates all chunk analyses, resolves contradictions, identifies themes

---

## Document Processing Pipeline

### Stage 1: Upload and Validation

**Process Flow**:
1. Client submits PDF via multipart/form-data POST request
2. PDFUploadService validates file:
   - Size check (≤ 50MB)
   - MIME type verification (application/pdf)
   - PDF structure validation using PDFBox
3. Generate unique document ID (UUID)
4. Store PDF to file system: `/app/data/uploads/{documentId}.pdf`
5. Create database record with metadata
6. Return document ID to client
7. Update status to `UPLOADED`

**Validation Rules**:
```java
// File size validation
if (file.getSize() > 50 * 1024 * 1024) {
    throw new InvalidFileException("File exceeds 50MB limit");
}

// MIME type validation
if (!"application/pdf".equals(file.getContentType())) {
    throw new InvalidFileException("Only PDF files accepted");
}

// PDF structure validation
try (PDDocument document = PDDocument.load(file.getInputStream())) {
    if (document.getNumberOfPages() == 0) {
        throw new InvalidFileException("PDF contains no pages");
    }
} catch (IOException e) {
    throw new InvalidFileException("Corrupted or invalid PDF");
}
```

**Error Handling**:
- Invalid file type → 400 Bad Request with descriptive message
- Corrupted PDF → 400 Bad Request with validation error
- File too large → 400 Bad Request with size limit message
- Storage failure → 500 Internal Server Error with retry suggestion

---

### Stage 2: Text Extraction

**Process Flow**:
1. Trigger asynchronous extraction task
2. Load PDF from file system
3. Extract text using Apache PDFBox `PDFTextStripper`
4. Preserve logical reading order
5. Calculate token count (approximate: 1 token ≈ 4 characters)
6. Validate document size (≤ 500 pages, ≤ 1M tokens)
7. Store extracted text in database
8. Update status to `PROCESSING`

**Extraction Configuration**:
```java
PDFTextStripper stripper = new PDFTextStripper();
stripper.setSortByPosition(true);  // Preserve reading order
stripper.setStartPage(1);
stripper.setEndPage(document.getNumberOfPages());
String extractedText = stripper.getText(document);
```

**Token Counting**:
```java
// Approximate tokenization for English text
int tokenCount = extractedText.length() / 4;

// Validation
if (tokenCount > 1_000_000) {
    throw new ExtractionException("Document exceeds 1M token limit");
}
```

**Error Handling**:
- Extraction failure → Retry 3 times with exponential backoff
- Document too large → Update status to `FAILED` with size error
- Corrupted content → Log error, notify user, mark as failed

---

### Stage 3: Intelligent Chunking

**Process Flow**:
1. Check if document exceeds 100K tokens
2. If yes, divide into chunks using semantic boundaries
3. Create Document_Chunk entities with metadata
4. Store chunks in database
5. Proceed to analysis stage

**Chunking Algorithm**:
```java
public List<DocumentChunk> chunkDocument(ExtractedDocument document) {
    if (document.getTokenCount() <= 100_000) {
        // Single chunk - no splitting needed
        return List.of(createSingleChunk(document));
    }
    
    List<DocumentChunk> chunks = new ArrayList<>();
    String text = document.getExtractedText();
    int targetChunkSize = 100_000;
    int overlapSize = 500;
    
    List<SemanticBoundary> boundaries = identifyBoundaries(text);
    
    int currentPosition = 0;
    int chunkSequence = 1;
    
    while (currentPosition < text.length()) {
        int chunkEnd = findOptimalSplitPoint(
            text, 
            currentPosition, 
            targetChunkSize, 
            boundaries
        );
        
        String chunkText = text.substring(currentPosition, chunkEnd);
        DocumentChunk chunk = createChunk(
            chunkText, 
            chunkSequence, 
            currentPosition, 
            chunkEnd
        );
        chunks.add(chunk);
        
        // Move to next chunk with overlap
        currentPosition = chunkEnd - (overlapSize * 4); // Convert tokens to chars
        chunkSequence++;
    }
    
    // Update total chunk count in all chunks
    chunks.forEach(chunk -> chunk.setTotalChunks(chunks.size()));
    
    return chunks;
}
```

**Semantic Boundary Detection**:
```java
public List<SemanticBoundary> identifyBoundaries(String text) {
    List<SemanticBoundary> boundaries = new ArrayList<>();
    
    // Priority 1: Section headers
    Pattern sectionPattern = Pattern.compile(
        "^(#{1,6}\\s+|\\d+\\.\\s+|[A-Z][A-Z\\s]+:).*$", 
        Pattern.MULTILINE
    );
    Matcher sectionMatcher = sectionPattern.matcher(text);
    while (sectionMatcher.find()) {
        boundaries.add(new SemanticBoundary(
            sectionMatcher.start(), 
            BoundaryType.SECTION_HEADER, 
            1  // Highest priority
        ));
    }
    
    // Priority 2: Paragraph breaks
    Pattern paragraphPattern = Pattern.compile("\\n\\n+");
    Matcher paragraphMatcher = paragraphPattern.matcher(text);
    while (paragraphMatcher.find()) {
        boundaries.add(new SemanticBoundary(
            paragraphMatcher.start(), 
            BoundaryType.PARAGRAPH_BREAK, 
            2
        ));
    }
    
    // Priority 3: Sentence endings
    Pattern sentencePattern = Pattern.compile("\\. [A-Z]");
    Matcher sentenceMatcher = sentencePattern.matcher(text);
    while (sentenceMatcher.find()) {
        boundaries.add(new SemanticBoundary(
            sentenceMatcher.start(), 
            BoundaryType.SENTENCE_ENDING, 
            3
        ));
    }
    
    return boundaries;
}
```

**Chunk Metadata**:
- `chunkId`: Unique identifier (UUID)
- `documentId`: Parent document reference
- `sequenceNumber`: Position in document (1-indexed)
- `totalChunks`: Total number of chunks in document
- `chunkText`: Extracted text content
- `tokenCount`: Approximate token count for this chunk
- `startByteOffset`: Starting position in original text
- `endByteOffset`: Ending position in original text
- `overlapTokens`: Number of overlapping tokens with previous chunk

---

## Multi-Agent Orchestration

### Orchestration Workflow

```
┌─────────────────────────────────────────────────────────────┐
│  PanelOrchestrator.orchestrateAnalysis(documentId, chunks)  │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
                    ┌────────────────┐
                    │ Create Panel   │
                    │ (6 Agents)     │
                    └────────┬───────┘
                             │
                             ▼
        ┌────────────────────────────────────────┐
        │  Submit All Agents to Thread Pool      │
        │  (Parallel Execution)                  │
        └────────┬───────────────────────────────┘
                 │
                 ├─────────┬─────────┬─────────┬─────────┬─────────┐
                 ▼         ▼         ▼         ▼         ▼         ▼
            ┌────────┐┌────────┐┌────────┐┌────────┐┌────────┐┌────────┐
            │ Lead   ││General ││Method. ││Liter.  ││Quick   ││Fact    │
            │Analyst ││Analyst ││Reviewer││Reviewer││Screener││Extract.│
            └───┬────┘└───┬────┘└───┬────┘└───┬────┘└───┬────┘└───┬────┘
                │         │         │         │         │         │
                │    Each Agent Processes Chunks Sequentially      │
                │         │         │         │         │         │
                ▼         ▼         ▼         ▼         ▼         ▼
            ┌────────────────────────────────────────────────────────┐
            │  For each chunk:                                       │
            │  1. Build prompt with previous context                 │
            │  2. Call NVIDIA API                                    │
            │  3. Store ChunkAnalysis                                │
            │  4. Update progress                                    │
            └────────────────────────────────────────────────────────┘
                │         │         │         │         │         │
                ▼         ▼         ▼         ▼         ▼         ▼
            ┌────────────────────────────────────────────────────────┐
            │  Synthesize all ChunkAnalyses into AnalysisReport      │
            └────────────────────────────────────────────────────────┘
                │         │         │         │         │         │
                └─────────┴─────────┴─────────┴─────────┴─────────┘
                                    │
                                    ▼
                        ┌───────────────────────┐
                        │ Collect All Reports   │
                        │ (Wait with Timeout)   │
                        └───────────┬───────────┘
                                    │
                                    ▼
                        ┌───────────────────────┐
                        │ Handle Failures       │
                        │ (Log & Continue)      │
                        └───────────┬───────────┘
                                    │
                                    ▼
                        ┌───────────────────────┐
                        │ Return Reports to     │
                        │ ConsensusEngine       │
                        └───────────────────────┘
```

### Agent Creation and Configuration

```java
public AnalysisPanel createPanel(String documentId) {
    AnalysisPanel panel = new AnalysisPanel(documentId);
    
    // Create 6 specialized agents
    panel.addAgent(new LeadAnalystAgent(
        generateAgentId(), 
        modelClient
    ));
    
    panel.addAgent(new GeneralAnalystAgent(
        generateAgentId(), 
        modelClient
    ));
    
    panel.addAgent(new MethodologyReviewerAgent(
        generateAgentId(), 
        modelClient
    ));
    
    panel.addAgent(new LiteratureReviewerAgent(
        generateAgentId(), 
        modelClient
    ));
    
    panel.addAgent(new QuickScreenerAgent(
        generateAgentId(), 
        modelClient
    ));
    
    panel.addAgent(new FactExtractorAgent(
        generateAgentId(), 
        modelClient
    ));
    
    return panel;
}
```

### Parallel Execution Management

```java
public void orchestrateAnalysis(AnalysisPanel panel, List<DocumentChunk> chunks) {
    ExecutorService executor = Executors.newFixedThreadPool(6);
    List<Future<AnalysisReport>> futures = new ArrayList<>();
    
    // Submit all agents for parallel execution
    for (AIAgent agent : panel.getAgents()) {
        Future<AnalysisReport> future = executor.submit(() -> {
            try {
                return agent.analyze(chunks);
            } catch (Exception e) {
                logger.error("Agent {} failed", agent.getAgentId(), e);
                throw e;
            }
        });
        futures.add(future);
    }
    
    // Collect results with timeout
    List<AnalysisReport> reports = new ArrayList<>();
    List<AgentFailure> failures = new ArrayList<>();
    
    for (int i = 0; i < futures.size(); i++) {
        try {
            AnalysisReport report = futures.get(i).get(30, TimeUnit.MINUTES);
            reports.add(report);
            logger.info("Agent {} completed successfully", 
                panel.getAgents().get(i).getAgentId());
        } catch (TimeoutException e) {
            AIAgent agent = panel.getAgents().get(i);
            logger.error("Agent {} timed out after 30 minutes", agent.getAgentId());
            failures.add(new AgentFailure(agent, "Timeout after 30 minutes"));
        } catch (Exception e) {
            AIAgent agent = panel.getAgents().get(i);
            logger.error("Agent {} failed with exception", agent.getAgentId(), e);
            failures.add(new AgentFailure(agent, e.getMessage()));
        }
    }
    
    executor.shutdown();
    
    // Store reports and failures
    reportRepository.saveAll(reports);
    if (!failures.isEmpty()) {
        logger.warn("Panel analysis completed with {} agent failures", 
            failures.size());
    }
}
```

### Progress Tracking

```java
public void updateAgentProgress(String documentId, String agentId, 
                                 int chunksCompleted, int totalChunks) {
    AgentProgress progress = progressRepository
        .findByDocumentIdAndAgentId(documentId, agentId)
        .orElse(new AgentProgress(documentId, agentId));
    
    progress.setChunksCompleted(chunksCompleted);
    progress.setTotalChunks(totalChunks);
    progress.setLastUpdated(LocalDateTime.now());
    
    progressRepository.save(progress);
    
    // Calculate overall progress
    List<AgentProgress> allProgress = progressRepository
        .findByDocumentId(documentId);
    
    int totalCompleted = allProgress.stream()
        .mapToInt(AgentProgress::getChunksCompleted)
        .sum();
    
    int totalExpected = allProgress.stream()
        .mapToInt(AgentProgress::getTotalChunks)
        .sum();
    
    double percentComplete = (double) totalCompleted / totalExpected * 100;
    
    logger.info("Document {} analysis progress: {:.1f}% ({}/{})", 
        documentId, percentComplete, totalCompleted, totalExpected);
}
```

---

## Consensus Generation Engine

### Consensus Synthesis Process

The ConsensusEngine receives all individual agent reports and synthesizes them into a unified consensus report using AI-powered analysis.

### Synthesis Workflow

```
┌──────────────────────────────────────────────────────────┐
│  ConsensusEngine.generateConsensus(agentReports)         │
└────────────────────────┬─────────────────────────────────┘
                         │
                         ▼
            ┌────────────────────────────┐
            │ Validate Input Reports     │
            │ (Minimum 3 required)       │
            └────────────┬───────────────┘
                         │
                         ▼
            ┌────────────────────────────┐
            │ Build Synthesis Prompt     │
            │ - Include all 6 reports    │
            │ - Add synthesis instructions│
            └────────────┬───────────────┘
                         │
                         ▼
            ┌────────────────────────────┐
            │ Call NVIDIA API            │
            │ (llama-3.1-nemotron-70b)   │
            └────────────┬───────────────┘
                         │
                         ▼
            ┌────────────────────────────┐
            │ Parse Response             │
            │ - Common themes            │
            │ - Agreements               │
            │ - Disagreements            │
            │ - Recommendations          │
            │ - Attributed insights      │
            └────────────┬───────────────┘
                         │
                         ▼
            ┌────────────────────────────┐
            │ Create ConsensusReport     │
            │ Entity                     │
            └────────────┬───────────────┘
                         │
                         ▼
            ┌────────────────────────────┐
            │ Store in Database          │
            └────────────┬───────────────┘
                         │
                         ▼
            ┌────────────────────────────┐
            │ Update Document Status     │
            │ to COMPLETE                │
            └────────────────────────────┘
```

### Synthesis Prompt Template

```
You are synthesizing analysis from a panel of 6 specialized AI agents who have 
independently reviewed a research document. Your task is to create a comprehensive 
consensus report that integrates their diverse perspectives.

AGENT REPORTS:

[Lead Analyst - Critical Evaluation]
{leadAnalystReport}

[General Analyst - Comprehensive Review]
{generalAnalystReport}

[Methodology Reviewer - Methods & Statistics]
{methodologyReviewerReport}

[Literature Reviewer - Context & Theory]
{literatureReviewerReport}

[Quick Screener - Key Claims & Issues]
{quickScreenerReport}

[Fact Extractor - Data & Facts]
{factExtractorReport}

SYNTHESIS INSTRUCTIONS:

1. COMMON THEMES
   Identify recurring themes, patterns, and observations that appear across 
   multiple agent reports. Focus on insights mentioned by 3+ agents.

2. AREAS OF AGREEMENT
   Highlight specific points where agents unanimously or predominantly agree. 
   Include the strength of consensus (e.g., "all agents agree", "5 of 6 agents").

3. AREAS OF DISAGREEMENT
   Identify contradictions or divergent perspectives between agents. Explain 
   the nature of the disagreement and which agents hold which positions.

4. UNIFIED RECOMMENDATIONS
   Synthesize actionable recommendations that integrate insights from all agents. 
   Prioritize recommendations by importance and feasibility.

5. ATTRIBUTED INSIGHTS
   For key insights, attribute them to their source agent(s). Format: 
   "Insight description (Agent Type)"

OUTPUT FORMAT:
Provide a structured JSON response with the following fields:
- commonThemes: array of theme strings
- agreements: array of agreement strings with consensus strength
- disagreements: array of disagreement objects with {issue, positions}
- unifiedRecommendations: array of recommendation strings (prioritized)
- attributedInsights: array of {insight, sourceAgents} objects
```

### Consensus Report Structure

```java
@Entity
public class ConsensusReport {
    @Id
    private String reportId;
    
    private String documentId;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String commonThemes;  // JSON array
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String agreements;  // JSON array
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String disagreements;  // JSON array
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String unifiedRecommendations;  // JSON array
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String attributedInsights;  // JSON array
    
    private LocalDateTime generatedAt;
    
    private int agentReportsIncluded;
    
    private int synthesisTokensUsed;
    
    private Duration synthesisTime;
}
```

### Example Consensus Output

```json
{
  "reportId": "consensus-550e8400-e29b-41d4-a716-446655440000",
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "commonThemes": [
    "Strong methodological foundation with appropriate statistical analysis",
    "Limited sample size may affect generalizability of findings",
    "Clear research question and well-defined objectives",
    "Insufficient discussion of study limitations",
    "Novel approach to data collection methodology"
  ],
  "agreements": [
    {
      "statement": "The research question is well-defined and addresses a significant gap",
      "consensus": "unanimous (6/6 agents)"
    },
    {
      "statement": "Statistical methods are appropriate for the research design",
      "consensus": "strong (5/6 agents)"
    },
    {
      "statement": "Sample size is smaller than ideal for the analysis performed",
      "consensus": "strong (5/6 agents)"
    }
  ],
  "disagreements": [
    {
      "issue": "Interpretation of primary outcome results",
      "positions": [
        {
          "view": "Results support the primary hypothesis with sufficient evidence",
          "agents": ["General Analyst", "Quick Screener", "Fact Extractor"]
        },
        {
          "view": "Results are suggestive but require cautious interpretation due to limitations",
          "agents": ["Lead Analyst", "Methodology Reviewer", "Literature Reviewer"]
        }
      ]
    }
  ],
  "unifiedRecommendations": [
    {
      "priority": 1,
      "recommendation": "Expand sample size in future studies to improve statistical power and generalizability"
    },
    {
      "priority": 2,
      "recommendation": "Strengthen the discussion section with more comprehensive treatment of study limitations"
    },
    {
      "priority": 3,
      "recommendation": "Consider additional control variables to address potential confounding factors"
    },
    {
      "priority": 4,
      "recommendation": "Enhance literature review to better position findings within existing research"
    }
  ],
  "attributedInsights": [
    {
      "insight": "The novel data collection approach represents a significant methodological contribution",
      "sourceAgents": ["Methodology Reviewer", "Lead Analyst"]
    },
    {
      "insight": "Statistical power analysis would strengthen confidence in null findings",
      "sourceAgents": ["Methodology Reviewer"]
    },
    {
      "insight": "Theoretical framework could be more explicitly connected to research design",
      "sourceAgents": ["Literature Reviewer", "General Analyst"]
    }
  ],
  "generatedAt": "2024-01-15T14:30:00Z",
  "agentReportsIncluded": 6,
  "synthesisTokensUsed": 45000,
  "synthesisTime": "PT2M15S"
}
```

---

## API Reference

### Base URL

```
Production: https://api.ai-rps.example.com
Development: http://localhost:8080
```

### Authentication

Currently, the API does not require authentication. Future versions will implement:
- API key authentication
- OAuth 2.0 / JWT tokens
- Rate limiting per client

### Common Response Codes

- `200 OK`: Request successful
- `202 Accepted`: Request accepted, processing asynchronously
- `400 Bad Request`: Invalid input or validation error
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server-side error
- `503 Service Unavailable`: External service (NVIDIA API) unavailable

---

### Endpoint 1: Upload Document

Upload a PDF research document for analysis.

**HTTP Request**
```
POST /api/documents/upload
```

**Request Headers**
```
Content-Type: multipart/form-data
```

**Request Body**
```
file: <binary PDF data>
```

**Success Response (200 OK)**
```json
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "filename": "research-paper.pdf",
  "fileSizeBytes": 2457600,
  "status": "UPLOADED",
  "uploadedAt": "2024-01-15T10:00:00Z",
  "estimatedProcessingTime": "30-45 minutes"
}
```

**Error Responses**

*File Too Large (400)*
```json
{
  "error": "FILE_TOO_LARGE",
  "message": "File size exceeds 50MB limit",
  "maxSizeBytes": 52428800,
  "actualSizeBytes": 60000000
}
```

*Invalid File Type (400)*
```json
{
  "error": "INVALID_FILE_TYPE",
  "message": "Only PDF files are accepted",
  "acceptedTypes": ["application/pdf"],
  "receivedType": "application/msword"
}
```

*Corrupted PDF (400)*
```json
{
  "error": "CORRUPTED_PDF",
  "message": "The uploaded file is corrupted or not a valid PDF",
  "details": "Unable to parse PDF structure"
}
```

**cURL Example**
```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@research-paper.pdf" \
  -H "Accept: application/json"
```

**Python Example**
```python
import requests

url = "http://localhost:8080/api/documents/upload"
files = {"file": open("research-paper.pdf", "rb")}

response = requests.post(url, files=files)
data = response.json()

print(f"Document ID: {data['documentId']}")
print(f"Status: {data['status']}")
```

---

### Endpoint 2: Get Document Status

Check the processing status and progress of a document.

**HTTP Request**
```
GET /api/documents/{documentId}/status
```

**Path Parameters**
- `documentId` (required): UUID of the uploaded document

**Success Response (200 OK)**

*Processing Status*
```json
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "filename": "research-paper.pdf",
  "status": "ANALYZING",
  "uploadedAt": "2024-01-15T10:00:00Z",
  "currentStage": "Agent Analysis",
  "progress": {
    "overallPercent": 45.5,
    "totalChunks": 3,
    "agentProgress": [
      {
        "agentType": "LEAD_ANALYST",
        "agentId": "agent-001",
        "chunksCompleted": 2,
        "totalChunks": 3,
        "percentComplete": 66.7,
        "status": "PROCESSING"
      },
      {
        "agentType": "GENERAL_ANALYST",
        "agentId": "agent-002",
        "chunksCompleted": 2,
        "totalChunks": 3,
        "percentComplete": 66.7,
        "status": "PROCESSING"
      },
      {
        "agentType": "METHODOLOGY_REVIEWER",
        "agentId": "agent-003",
        "chunksCompleted": 1,
        "totalChunks": 3,
        "percentComplete": 33.3,
        "status": "PROCESSING"
      },
      {
        "agentType": "LITERATURE_REVIEWER",
        "agentId": "agent-004",
        "chunksCompleted": 1,
        "totalChunks": 3,
        "percentComplete": 33.3,
        "status": "PROCESSING"
      },
      {
        "agentType": "QUICK_SCREENER",
        "agentId": "agent-005",
        "chunksCompleted": 1,
        "totalChunks": 3,
        "percentComplete": 33.3,
        "status": "PROCESSING"
      },
      {
        "agentType": "FACT_EXTRACTOR",
        "agentId": "agent-006",
        "chunksCompleted": 1,
        "totalChunks": 3,
        "percentComplete": 33.3,
        "status": "PROCESSING"
      }
    ]
  },
  "estimatedTimeRemaining": "15 minutes",
  "startedAt": "2024-01-15T10:01:00Z"
}
```

*Complete Status*
```json
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "filename": "research-paper.pdf",
  "status": "COMPLETE",
  "uploadedAt": "2024-01-15T10:00:00Z",
  "completedAt": "2024-01-15T10:35:00Z",
  "processingDuration": "PT35M",
  "resultsAvailable": true
}
```

*Failed Status*
```json
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "filename": "research-paper.pdf",
  "status": "FAILED",
  "uploadedAt": "2024-01-15T10:00:00Z",
  "failedAt": "2024-01-15T10:15:00Z",
  "errorMessage": "All agents failed to complete analysis",
  "errorDetails": "NVIDIA API unavailable after 3 retry attempts"
}
```

**Status Values**
- `UPLOADED`: Document uploaded, awaiting processing
- `PROCESSING`: Extracting text and chunking document
- `ANALYZING`: AI agents analyzing document chunks
- `DELIBERATING`: Generating consensus report
- `COMPLETE`: Analysis complete, results available
- `FAILED`: Processing failed (see errorMessage)

**Error Response (404)**
```json
{
  "error": "DOCUMENT_NOT_FOUND",
  "message": "No document found with ID: 550e8400-e29b-41d4-a716-446655440000"
}
```

**cURL Example**
```bash
curl http://localhost:8080/api/documents/550e8400-e29b-41d4-a716-446655440000/status
```

---

### Endpoint 3: Get Consensus Results

Retrieve the final consensus report for a completed analysis.

**HTTP Request**
```
GET /api/documents/{documentId}/results
```

**Path Parameters**
- `documentId` (required): UUID of the uploaded document

**Success Response (200 OK)**
```json
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "filename": "research-paper.pdf",
  "consensusReport": {
    "reportId": "consensus-550e8400-e29b-41d4-a716-446655440000",
    "generatedAt": "2024-01-15T10:35:00Z",
    "commonThemes": [
      "Strong methodological foundation with appropriate statistical analysis",
      "Limited sample size may affect generalizability of findings",
      "Clear research question and well-defined objectives"
    ],
    "agreements": [
      {
        "statement": "The research question is well-defined and addresses a significant gap",
        "consensus": "unanimous (6/6 agents)"
      },
      {
        "statement": "Statistical methods are appropriate for the research design",
        "consensus": "strong (5/6 agents)"
      }
    ],
    "disagreements": [
      {
        "issue": "Interpretation of primary outcome results",
        "positions": [
          {
            "view": "Results support the primary hypothesis with sufficient evidence",
            "agents": ["General Analyst", "Quick Screener", "Fact Extractor"]
          },
          {
            "view": "Results require cautious interpretation due to limitations",
            "agents": ["Lead Analyst", "Methodology Reviewer", "Literature Reviewer"]
          }
        ]
      }
    ],
    "unifiedRecommendations": [
      {
        "priority": 1,
        "recommendation": "Expand sample size in future studies"
      },
      {
        "priority": 2,
        "recommendation": "Strengthen discussion of study limitations"
      }
    ],
    "attributedInsights": [
      {
        "insight": "Novel data collection approach represents significant methodological contribution",
        "sourceAgents": ["Methodology Reviewer", "Lead Analyst"]
      }
    ],
    "agentReportsIncluded": 6,
    "synthesisTokensUsed": 45000
  }
}
```

**Error Response - Still Processing (202 Accepted)**
```json
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ANALYZING",
  "message": "Document is still being analyzed",
  "progress": {
    "overallPercent": 45.5
  },
  "estimatedTimeRemaining": "15 minutes"
}
```

**Error Response - Not Found (404)**
```json
{
  "error": "DOCUMENT_NOT_FOUND",
  "message": "No document found with ID: 550e8400-e29b-41d4-a716-446655440000"
}
```

**cURL Example**
```bash
curl http://localhost:8080/api/documents/550e8400-e29b-41d4-a716-446655440000/results
```

---

### Endpoint 4: Get Detailed Results

Retrieve all individual agent reports plus the consensus report.

**HTTP Request**
```
GET /api/documents/{documentId}/results/detailed
```

**Path Parameters**
- `documentId` (required): UUID of the uploaded document

**Success Response (200 OK)**
```json
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "filename": "research-paper.pdf",
  "agentReports": [
    {
      "reportId": "report-agent-001",
      "agentType": "LEAD_ANALYST",
      "agentId": "agent-001",
      "completedAt": "2024-01-15T10:30:00Z",
      "keyFindings": "The research demonstrates strong methodological rigor...",
      "strengths": [
        "Clear research question",
        "Appropriate statistical methods",
        "Well-structured presentation"
      ],
      "weaknesses": [
        "Limited sample size",
        "Potential selection bias",
        "Insufficient discussion of limitations"
      ],
      "recommendations": [
        "Expand sample size in future studies",
        "Address potential confounding variables",
        "Strengthen limitations discussion"
      ],
      "chunksAnalyzed": 3,
      "chunksFailed": 0
    },
    {
      "reportId": "report-agent-002",
      "agentType": "GENERAL_ANALYST",
      "agentId": "agent-002",
      "completedAt": "2024-01-15T10:31:00Z",
      "keyFindings": "Comprehensive review reveals well-executed research...",
      "strengths": [
        "Logical flow and organization",
        "Clear writing style",
        "Adequate literature review"
      ],
      "weaknesses": [
        "Some sections could be more concise",
        "Limited discussion of alternative explanations"
      ],
      "recommendations": [
        "Streamline methodology section",
        "Expand discussion of alternative interpretations"
      ],
      "chunksAnalyzed": 3,
      "chunksFailed": 0
    }
    // ... 4 more agent reports
  ],
  "consensusReport": {
    // Same structure as /results endpoint
  }
}
```

**Error Responses**: Same as `/results` endpoint

**cURL Example**
```bash
curl http://localhost:8080/api/documents/550e8400-e29b-41d4-a716-446655440000/results/detailed
```

---

## Deployment Architecture

### Docker Compose Stack

MACIF-SARDACG runs as a multi-container Docker application with three deployment profiles:

#### 1. Production Profile (Default)

**Services**:
- `aipanelist-app`: Spring Boot application
- `aipanelist-postgres`: PostgreSQL 15 database

**Resource Allocation**:
- App: 2 CPU cores, 2GB RAM
- Database: 2 CPU cores, 2GB RAM

**Start Command**:
```bash
docker-compose up -d
```

#### 2. Development Profile

**Additional Services**:
- `aipanelist-pgadmin`: Database management UI

**Features**:
- Hot reload enabled
- Remote debugging on port 5005
- Verbose logging (TRACE level)
- Source code mounted as volume

**Start Command**:
```bash
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

#### 3. Production with Nginx Profile

**Additional Services**:
- `aipanelist-nginx`: Reverse proxy with SSL/TLS

**Features**:
- Rate limiting (100 req/min per IP)
- SSL/TLS termination
- Load balancing ready
- Enhanced security headers

**Start Command**:
```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### Container Configuration

#### Application Container

**Base Image**: `eclipse-temurin:21-jre-alpine`

**Exposed Ports**:
- `8080`: HTTP API

**Environment Variables**:
- `NVIDIA_API_KEY`: NVIDIA API authentication key (required)
- `DATABASE_URL`: PostgreSQL connection URL
- `DATABASE_USERNAME`: Database username
- `DATABASE_PASSWORD`: Database password
- `UPLOAD_DIR`: File upload directory
- `LOG_LEVEL_ROOT`: Root logging level
- `LOG_LEVEL_APP`: Application logging level
- `JAVA_OPTS`: JVM options

**Volumes**:
- `upload-data:/app/data/uploads`: Persistent file storage

**Health Check**:
```yaml
healthcheck:
  test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", 
         "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 60s
```

#### Database Container

**Base Image**: `postgres:15-alpine`

**Exposed Ports**:
- `5432`: PostgreSQL

**Environment Variables**:
- `POSTGRES_DB`: Database name
- `POSTGRES_USER`: Database username
- `POSTGRES_PASSWORD`: Database password

**Volumes**:
- `postgres-data:/var/lib/postgresql/data`: Persistent database storage
- `./init-db:/docker-entrypoint-initdb.d`: Initialization scripts

**Health Check**:
```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U aipanelist"]
  interval: 10s
  timeout: 5s
  retries: 5
```

### Network Configuration

All containers run on a custom bridge network `aipanelist-network` with DNS resolution enabled.

**Network Definition**:
```yaml
networks:
  aipanelist-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
```

### Volume Management

**Persistent Volumes**:
- `postgres-data`: Database files
- `upload-data`: Uploaded PDF documents
- `pgadmin-data`: PgAdmin configuration (dev mode only)

**Backup Commands**:
```bash
# Backup database
docker-compose exec -T postgres pg_dump -U aipanelist aipanelist > backup.sql

# Backup uploaded files
docker run --rm -v aipanelist_upload-data:/data -v $(pwd):/backup \
  alpine tar czf /backup/uploads-backup.tar.gz -C /data .

# Restore database
docker-compose exec -T postgres psql -U aipanelist aipanelist < backup.sql

# Restore uploaded files
docker run --rm -v aipanelist_upload-data:/data -v $(pwd):/backup \
  alpine tar xzf /backup/uploads-backup.tar.gz -C /data
```

---

## Configuration Management

### Environment Variables

All configuration is managed through environment variables defined in `.env` file.

**Required Variables**:
```bash
# NVIDIA API Configuration (REQUIRED)
NVIDIA_API_KEY=nvapi-xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
NVIDIA_API_ENDPOINT=https://integrate.api.nvidia.com/v1

# Database Configuration
DATABASE_URL=jdbc:postgresql://postgres:5432/aipanelist
DATABASE_USERNAME=aipanelist
DATABASE_PASSWORD=changeme_in_production

# Application Configuration
SERVER_PORT=8080
UPLOAD_DIR=/app/data/uploads
```

**Optional Variables**:
```bash
# Processing Configuration
MAX_CHUNK_SIZE_TOKENS=100000
CHUNK_OVERLAP_TOKENS=500
MAX_DOCUMENT_PAGES=500
MAX_DOCUMENT_TOKENS=1000000

# Async Processing
ASYNC_THREAD_POOL_SIZE=10
AGENT_TIMEOUT_MINUTES=30

# Logging
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=DEBUG
LOG_LEVEL_HIBERNATE=WARN

# JVM Configuration
JAVA_OPTS=-Xms512m -Xmx2048m -XX:+UseG1GC

# Rate Limiting
NVIDIA_API_RATE_LIMIT=60
NVIDIA_API_MAX_CONNECTIONS=10

# Circuit Breaker
CIRCUIT_BREAKER_FAILURE_THRESHOLD=50
CIRCUIT_BREAKER_WAIT_DURATION=60000
```

### Application Properties

Spring Boot configuration in `src/main/resources/application.properties`:

```properties
# Application
spring.application.name=macif-sardacg
server.port=${SERVER_PORT:8080}

# Database
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USERNAME}
spring.datasource.password=${DATABASE_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true

# File Upload
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
upload.directory=${UPLOAD_DIR:/app/data/uploads}

# Async
spring.task.execution.pool.core-size=${ASYNC_THREAD_POOL_SIZE:10}
spring.task.execution.pool.max-size=20
spring.task.execution.pool.queue-capacity=100
spring.task.execution.thread-name-prefix=async-

# Logging
logging.level.root=${LOG_LEVEL_ROOT:INFO}
logging.level.com.aipanelist=${LOG_LEVEL_APP:DEBUG}
logging.level.org.hibernate=${LOG_LEVEL_HIBERNATE:WARN}
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n

# Actuator
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always

# NVIDIA API
nvidia.api.key=${NVIDIA_API_KEY}
nvidia.api.endpoint=${NVIDIA_API_ENDPOINT:https://integrate.api.nvidia.com/v1}
nvidia.api.model=nvidia/llama-3.1-nemotron-70b-instruct
nvidia.api.rate-limit=${NVIDIA_API_RATE_LIMIT:60}
nvidia.api.max-connections=${NVIDIA_API_MAX_CONNECTIONS:10}

# Processing
processing.max-chunk-size-tokens=${MAX_CHUNK_SIZE_TOKENS:100000}
processing.chunk-overlap-tokens=${CHUNK_OVERLAP_TOKENS:500}
processing.max-document-pages=${MAX_DOCUMENT_PAGES:500}
processing.max-document-tokens=${MAX_DOCUMENT_TOKENS:1000000}
processing.agent-timeout-minutes=${AGENT_TIMEOUT_MINUTES:30}

# Circuit Breaker
resilience4j.circuitbreaker.instances.nvidia-api.failure-rate-threshold=${CIRCUIT_BREAKER_FAILURE_THRESHOLD:50}
resilience4j.circuitbreaker.instances.nvidia-api.wait-duration-in-open-state=${CIRCUIT_BREAKER_WAIT_DURATION:60000}
resilience4j.circuitbreaker.instances.nvidia-api.sliding-window-size=10
```

---

## Error Handling & Resilience

### Error Categories

#### 1. User Input Errors (4xx)
- Invalid file format
- File too large
- Corrupted PDF
- Invalid document ID

**Handling**: Immediate validation at API boundary, descriptive error messages

#### 2. Processing Errors (5xx)
- Text extraction failure
- Chunking errors
- Database failures

**Handling**: Retry with exponential backoff, fallback strategies, detailed logging

#### 3. External Service Errors (503)
- NVIDIA API unavailable
- Rate limiting
- Network timeouts

**Handling**: Circuit breaker pattern, retry logic, graceful degradation

#### 4. Agent Failures
- Individual agent timeout
- Analysis errors
- Synthesis failures

**Handling**: Continue with remaining agents, note failures in consensus report

### Retry Strategy

**Exponential Backoff Implementation**:
```java
public class RetryHandler {
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;
    private static final double BACKOFF_MULTIPLIER = 2.0;
    
    public <T> T executeWithRetry(
        Supplier<T> operation, 
        String operationName,
        Class<? extends Exception>... retryableExceptions
    ) {
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < MAX_RETRIES) {
            try {
                return operation.get();
            } catch (Exception e) {
                if (!isRetryable(e, retryableExceptions)) {
                    throw e;
                }
                
                lastException = e;
                attempt++;
                
                if (attempt < MAX_RETRIES) {
                    long backoffMs = (long) (INITIAL_BACKOFF_MS * 
                        Math.pow(BACKOFF_MULTIPLIER, attempt - 1));
                    
                    logger.warn("Attempt {}/{} failed for {}, retrying in {}ms", 
                        attempt, MAX_RETRIES, operationName, backoffMs, e);
                    
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                }
            }
        }
        
        throw new MaxRetriesExceededException(
            String.format("Failed after %d attempts: %s", MAX_RETRIES, operationName),
            lastException
        );
    }
    
    private boolean isRetryable(Exception e, Class<? extends Exception>[] retryableExceptions) {
        for (Class<? extends Exception> retryable : retryableExceptions) {
            if (retryable.isInstance(e)) {
                return true;
            }
        }
        return false;
    }
}
```

### Circuit Breaker Pattern

**Configuration**:
```java
@Configuration
public class CircuitBreakerConfiguration {
    
    @Bean
    public CircuitBreaker nvidiaApiCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)  // Open if 50% of calls fail
            .waitDurationInOpenState(Duration.ofMinutes(1))  // Wait 1 min before half-open
            .slidingWindowSize(10)  // Consider last 10 calls
            .permittedNumberOfCallsInHalfOpenState(3)  // Allow 3 test calls
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .recordExceptions(
                IOException.class,
                TimeoutException.class,
                APIException.class
            )
            .build();
        
        return CircuitBreaker.of("nvidia-api", config);
    }
}
```

**Usage**:
```java
public ModelResponse sendRequest(ModelRequest request) {
    return circuitBreaker.executeSupplier(() -> {
        return retryHandler.executeWithRetry(
            () -> httpClient.post(request),
            "NVIDIA API call",
            IOException.class,
            TimeoutException.class
        );
    });
}
```

### Graceful Degradation

**Agent Failure Handling**:
```java
public ConsensusReport orchestrateAnalysis(String documentId, List<DocumentChunk> chunks) {
    List<AnalysisReport> successfulReports = new ArrayList<>();
    List<AgentFailure> failures = new ArrayList<>();
    
    // Execute all agents
    for (AIAgent agent : panel.getAgents()) {
        try {
            AnalysisReport report = agent.analyze(chunks);
            successfulReports.add(report);
        } catch (Exception e) {
            logger.error("Agent {} failed", agent.getAgentId(), e);
            failures.add(new AgentFailure(agent, e));
        }
    }
    
    // Require minimum 3 successful agents for consensus
    if (successfulReports.size() < 3) {
        throw new AllAgentsFailedException(
            "Insufficient agent reports for consensus generation",
            failures
        );
    }
    
    // Generate consensus with available reports
    ConsensusReport consensus = consensusEngine.generateConsensus(successfulReports);
    
    // Note failures in consensus report
    if (!failures.isEmpty()) {
        consensus.addNote(String.format(
            "Analysis completed with %d agent failures: %s",
            failures.size(),
            failures.stream()
                .map(f -> f.getAgentType().toString())
                .collect(Collectors.joining(", "))
        ));
    }
    
    return consensus;
}
```

### Structured Error Responses

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFile(InvalidFileException e) {
        ErrorResponse error = ErrorResponse.builder()
            .error("INVALID_FILE")
            .message(e.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDocumentNotFound(DocumentNotFoundException e) {
        ErrorResponse error = ErrorResponse.builder()
            .error("DOCUMENT_NOT_FOUND")
            .message(e.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(APIException.class)
    public ResponseEntity<ErrorResponse> handleAPIException(APIException e) {
        ErrorResponse error = ErrorResponse.builder()
            .error("EXTERNAL_SERVICE_ERROR")
            .message("NVIDIA API error: " + e.getMessage())
            .timestamp(LocalDateTime.now())
            .retryable(e.isRetryable())
            .build();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        logger.error("Unhandled exception", e);
        ErrorResponse error = ErrorResponse.builder()
            .error("INTERNAL_SERVER_ERROR")
            .message("An unexpected error occurred")
            .timestamp(LocalDateTime.now())
            .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

---

## Performance Characteristics

### Processing Time Estimates

**Single-Chunk Document (< 100K tokens)**:
- Text Extraction: 10-30 seconds
- Agent Analysis: 5-7 minutes per agent (parallel)
- Consensus Generation: 2-3 minutes
- **Total**: 7-10 minutes

**Multi-Chunk Document (3 chunks, ~300K tokens)**:
- Text Extraction: 30-60 seconds
- Chunking: 5-10 seconds
- Agent Analysis: 15-21 minutes per agent (parallel)
- Consensus Generation: 2-3 minutes
- **Total**: 17-24 minutes

**Large Document (5 chunks, ~500K tokens)**:
- Text Extraction: 60-90 seconds
- Chunking: 10-15 seconds
- Agent Analysis: 25-35 minutes per agent (parallel)
- Consensus Generation: 3-4 minutes
- **Total**: 28-39 minutes

### Resource Utilization

**CPU Usage**:
- Idle: 5-10%
- Text Extraction: 40-60%
- Agent Analysis: 20-30% (mostly I/O wait)
- Consensus Generation: 20-30%

**Memory Usage**:
- Base Application: 300-400 MB
- Per Document Processing: 50-100 MB
- Peak (6 agents + consensus): 800-1200 MB

**Network Bandwidth**:
- NVIDIA API Requests: 50-200 KB per chunk
- NVIDIA API Responses: 10-50 KB per chunk
- Total per document: 1-5 MB

**Database Storage**:
- Document metadata: ~1 KB
- Extracted text: 100 KB - 5 MB
- Chunk analyses: 50-200 KB per agent
- Consensus report: 20-100 KB
- Total per document: 500 KB - 10 MB

### Scalability Considerations

**Horizontal Scaling**:
- Stateless application design enables multiple instances
- Shared PostgreSQL database for coordination
- Shared file storage (NFS/S3) for uploaded documents
- Load balancer distributes requests across instances

**Vertical Scaling**:
- Increase thread pool size for more concurrent agents
- Allocate more memory for larger documents
- Faster CPU reduces text extraction time

**Bottlenecks**:
- NVIDIA API rate limits (60 req/min default)
- Database connection pool (default: 10 connections)
- File I/O for large PDF uploads

**Optimization Strategies**:
- Implement request queuing for rate limit management
- Cache frequently accessed data
- Use connection pooling for database and HTTP clients
- Implement async processing for all long-running tasks

---

## Security Considerations

### API Security

**Current State**: No authentication required (development/demo)

**Recommended Production Security**:
1. **API Key Authentication**
   - Generate unique API keys per client
   - Include in `Authorization` header
   - Rate limit per API key

2. **OAuth 2.0 / JWT Tokens**
   - Implement OAuth 2.0 authorization flow
   - Issue JWT tokens with expiration
   - Validate tokens on each request

3. **HTTPS/TLS**
   - Enforce HTTPS for all API endpoints
   - Use valid SSL/TLS certificates
   - Implement HSTS headers

### Input Validation

**File Upload Security**:
```java
public void validatePDF(MultipartFile file) {
    // Size validation
    if (file.getSize() > MAX_FILE_SIZE) {
        throw new InvalidFileException("File too large");
    }
    
    // MIME type validation
    if (!"application/pdf".equals(file.getContentType())) {
        throw new InvalidFileException("Invalid file type");
    }
    
    // Magic number validation
    byte[] header = new byte[4];
    file.getInputStream().read(header);
    if (!Arrays.equals(header, new byte[]{0x25, 0x50, 0x44, 0x46})) {  // %PDF
        throw new InvalidFileException("Not a valid PDF");
    }
    
    // Structure validation with PDFBox
    try (PDDocument doc = PDDocument.load(file.getInputStream())) {
        if (doc.isEncrypted()) {
            throw new InvalidFileException("Encrypted PDFs not supported");
        }
    }
}
```

### Data Protection

**Sensitive Data Handling**:
- API keys stored in environment variables (never in code)
- Database passwords encrypted at rest
- Uploaded documents stored with restricted permissions
- Automatic cleanup of old documents (configurable retention)

**Data Retention Policy**:
```java
@Scheduled(cron = "0 0 2 * * *")  // Daily at 2 AM
public void cleanupOldDocuments() {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
    List<Document> oldDocuments = documentRepository
        .findByUploadedAtBefore(cutoff);
    
    for (Document doc : oldDocuments) {
        // Delete file
        Files.deleteIfExists(Paths.get(uploadDir, doc.getDocumentId() + ".pdf"));
        
        // Delete database records
        documentRepository.delete(doc);
        
        logger.info("Cleaned up document {} (uploaded {})", 
            doc.getDocumentId(), doc.getUploadedAt());
    }
}
```

### Dependency Security

**Vulnerability Scanning**:
```bash
# Maven dependency check
mvn org.owasp:dependency-check-maven:check

# Docker image scanning
docker scan aipanelist-app:latest
```

**Regular Updates**:
- Monitor security advisories for Spring Boot, PDFBox, PostgreSQL
- Update dependencies quarterly or when critical vulnerabilities discovered
- Test thoroughly after updates

---

## Testing Strategy

### Test Coverage

**Total Tests**: 152
- Unit Tests: 146
- Integration Tests: 6
- Property-Based Tests: 15 (embedded in unit tests)

**Coverage Metrics**:
- Line Coverage: 87%
- Branch Coverage: 82%
- Method Coverage: 91%

### Test Categories

#### 1. Unit Tests

**Component Testing**:
```java
@ExtendWith(MockitoExtension.class)
class DocumentProcessorTest {
    
    @Mock
    private PDFTextStripper textStripper;
    
    @InjectMocks
    private DocumentProcessorImpl processor;
    
    @Test
    void shouldExtractTextFromValidPDF() throws Exception {
        // Given
        Path pdfPath = Paths.get("test-document.pdf");
        when(textStripper.getText(any())).thenReturn("Sample text");
        
        // When
        ExtractedDocument result = processor.extractText("doc-123", pdfPath);
        
        // Then
        assertThat(result.getExtractedText()).isEqualTo("Sample text");
        assertThat(result.getTokenCount()).isGreaterThan(0);
    }
    
    @Test
    void shouldThrowExceptionForCorruptedPDF() {
        // Given
        Path corruptedPath = Paths.get("corrupted.pdf");
        
        // When/Then
        assertThatThrownBy(() -> processor.extractText("doc-123", corruptedPath))
            .isInstanceOf(ExtractionException.class)
            .hasMessageContaining("Failed to extract text");
    }
}
```

#### 2. Integration Tests

**End-to-End Testing with Testcontainers**:
```java
@SpringBootTest
@Testcontainers
class EndToEndIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");
    
    @Autowired
    private DocumentController controller;
    
    @Test
    void shouldProcessDocumentEndToEnd() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            getClass().getResourceAsStream("/test-document.pdf")
        );
        
        // When - Upload
        ResponseEntity<?> uploadResponse = controller.uploadDocument(file);
        String documentId = ((DocumentUploadResponse) uploadResponse.getBody())
            .getDocumentId();
        
        // Then - Status progresses
        await().atMost(Duration.ofMinutes(5))
            .pollInterval(Duration.ofSeconds(5))
            .until(() -> {
                DocumentStatus status = controller.getStatus(documentId).getBody();
                return status.getStatus() == ProcessingStatus.COMPLETE;
            });
        
        // Then - Results available
        ResponseEntity<?> resultsResponse = controller.getResults(documentId);
        assertThat(resultsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        ConsensusReport consensus = ((ResultsResponse) resultsResponse.getBody())
            .getConsensusReport();
        assertThat(consensus.getCommonThemes()).isNotEmpty();
        assertThat(consensus.getAgentReportsIncluded()).isEqualTo(6);
    }
}
```

#### 3. Property-Based Tests

**Invariant Testing with jqwik**:
```java
class ChunkProcessorPropertyTest {
    
    @Property
    void allChunksShouldBeBelowMaxSize(
        @ForAll @StringLength(min = 100000, max = 500000) String documentText
    ) {
        // Given
        ChunkProcessor processor = new ChunkProcessorImpl();
        ExtractedDocument document = new ExtractedDocument();
        document.setExtractedText(documentText);
        
        // When
        List<DocumentChunk> chunks = processor.chunkDocument(document);
        
        // Then
        assertThat(chunks).allMatch(chunk -> 
            chunk.getTokenCount() <= 100_000
        );
    }
    
    @Property
    void consecutiveChunksShouldHaveOverlap(
        @ForAll @StringLength(min = 200000, max = 500000) String documentText
    ) {
        // Given
        ChunkProcessor processor = new ChunkProcessorImpl();
        ExtractedDocument document = new ExtractedDocument();
        document.setExtractedText(documentText);
        
        // When
        List<DocumentChunk> chunks = processor.chunkDocument(document);
        
        // Then
        for (int i = 0; i < chunks.size() - 1; i++) {
            DocumentChunk current = chunks.get(i);
            DocumentChunk next = chunks.get(i + 1);
            
            String currentEnd = current.getChunkText()
                .substring(Math.max(0, current.getChunkText().length() - 2000));
            String nextStart = next.getChunkText().substring(0, 2000);
            
            assertThat(nextStart).contains(currentEnd.substring(0, 500));
        }
    }
}
```

### Running Tests

**All Tests**:
```bash
# In Docker
docker-compose exec app mvn test

# Locally (requires Java 21)
mvn test
```

**Specific Test Class**:
```bash
mvn test -Dtest=DocumentProcessorTest
```

**With Coverage Report**:
```bash
mvn test jacoco:report
# View: target/site/jacoco/index.html
```

**Integration Tests Only**:
```bash
mvn test -Dtest=*IntegrationTest
```

---

## Troubleshooting Guide

### Common Issues and Solutions

#### Issue 1: "NVIDIA API key not configured"

**Symptoms**:
```
ERROR: NVIDIA_API_KEY environment variable not set
Application failed to start
```

**Solution**:
1. Create `.env` file from template:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` and add your API key:
   ```
   NVIDIA_API_KEY=nvapi-your-key-here
   ```

3. Restart containers:
   ```bash
   docker-compose down
   docker-compose up -d
   ```

**Verification**:
```bash
docker-compose exec app env | grep NVIDIA_API_KEY
```

---

#### Issue 2: "Database connection failed"

**Symptoms**:
```
ERROR: Connection to postgres:5432 refused
Could not connect to database
```

**Solution**:
1. Check if PostgreSQL container is running:
   ```bash
   docker-compose ps postgres
   ```

2. If not running, start it:
   ```bash
   docker-compose up -d postgres
   ```

3. Check PostgreSQL logs:
   ```bash
   docker-compose logs postgres
   ```

4. Verify database credentials in `.env`:
   ```
   DATABASE_USERNAME=aipanelist
   DATABASE_PASSWORD=changeme
   ```

5. Test connection manually:
   ```bash
   docker-compose exec postgres psql -U aipanelist -d aipanelist
   ```

---

#### Issue 3: "Circuit breaker is OPEN"

**Symptoms**:
```
ERROR: Circuit breaker 'nvidia-api' is OPEN
Requests to NVIDIA API are blocked
```

**Explanation**: Circuit breaker opens when 50% of recent API calls fail, preventing cascading failures.

**Solution**:
1. Wait 1 minute for circuit breaker to transition to half-open state

2. Check NVIDIA API status:
   ```bash
   curl -H "Authorization: Bearer $NVIDIA_API_KEY" \
     https://integrate.api.nvidia.com/v1/models
   ```

3. If API is available, circuit breaker will automatically close after successful test calls

4. If API remains unavailable, check:
   - API key validity
   - Network connectivity
   - NVIDIA service status

**Manual Reset** (if needed):
```bash
# Restart application to reset circuit breaker
docker-compose restart app
```

---

#### Issue 4: "All agents failed"

**Symptoms**:
```
ERROR: All agents failed to complete analysis
Document status: FAILED
```

**Possible Causes**:
1. NVIDIA API unavailable
2. Rate limit exceeded
3. Network connectivity issues
4. Invalid document content

**Diagnosis**:
```bash
# Check application logs
docker-compose logs app | grep ERROR

# Check agent-specific failures
docker-compose logs app | grep "Agent.*failed"

# Check NVIDIA API connectivity
docker-compose exec app curl -H "Authorization: Bearer $NVIDIA_API_KEY" \
  https://integrate.api.nvidia.com/v1/models
```

**Solutions**:
- If rate limited: Wait and retry, or increase rate limit in configuration
- If API unavailable: Wait for service restoration
- If network issues: Check firewall, proxy settings
- If document issues: Try different document

---

#### Issue 5: "File upload failed: File too large"

**Symptoms**:
```
400 Bad Request
{"error": "FILE_TOO_LARGE", "message": "File exceeds 50MB limit"}
```

**Solution**:
1. Check file size:
   ```bash
   ls -lh research-paper.pdf
   ```

2. If file is legitimately large, compress it:
   ```bash
   # Using Ghostscript
   gs -sDEVICE=pdfwrite -dCompatibilityLevel=1.4 -dPDFSETTINGS=/ebook \
      -dNOPAUSE -dQUIET -dBATCH \
      -sOutputFile=compressed.pdf input.pdf
   ```

3. Alternatively, increase limit in configuration (not recommended):
   ```properties
   # application.properties
   spring.servlet.multipart.max-file-size=100MB
   spring.servlet.multipart.max-request-size=100MB
   ```

---

#### Issue 6: "Document stuck in ANALYZING status"

**Symptoms**:
- Status remains "ANALYZING" for > 1 hour
- No progress updates

**Diagnosis**:
```bash
# Check if agents are still running
docker-compose logs app | tail -100 | grep "Agent.*chunk"

# Check thread pool status
docker-compose logs app | grep "thread pool"

# Check for deadlocks
docker-compose exec app jstack 1
```

**Solutions**:
1. Check agent timeout configuration:
   ```properties
   processing.agent-timeout-minutes=30
   ```

2. Restart application if truly stuck:
   ```bash
   docker-compose restart app
   ```

3. Re-upload document if restart doesn't help

---

#### Issue 7: "Out of memory error"

**Symptoms**:
```
java.lang.OutOfMemoryError: Java heap space
Container killed (OOMKilled)
```

**Solution**:
1. Increase JVM heap size in `.env`:
   ```
   JAVA_OPTS=-Xms1g -Xmx4g -XX:+UseG1GC
   ```

2. Increase Docker container memory limit in `docker-compose.yml`:
   ```yaml
   services:
     app:
       deploy:
         resources:
           limits:
             memory: 4G
   ```

3. Restart containers:
   ```bash
   docker-compose down
   docker-compose up -d
   ```

---

### Diagnostic Commands

**Check Service Health**:
```bash
# All services
docker-compose ps

# Application health endpoint
curl http://localhost:8080/actuator/health

# Database health
docker-compose exec postgres pg_isready -U aipanelist
```

**View Logs**:
```bash
# All logs
docker-compose logs -f

# Application logs only
docker-compose logs -f app

# Last 100 lines
docker-compose logs --tail=100 app

# Filter by error level
docker-compose logs app | grep ERROR
```

**Resource Usage**:
```bash
# Container stats
docker stats

# Disk usage
docker system df

# Volume usage
docker volume ls
du -sh /var/lib/docker/volumes/aipanelist_*
```

**Database Inspection**:
```bash
# Connect to database
docker-compose exec postgres psql -U aipanelist -d aipanelist

# Check document count
SELECT COUNT(*) FROM documents;

# Check processing status distribution
SELECT status, COUNT(*) FROM documents GROUP BY status;

# Check recent documents
SELECT document_id, filename, status, uploaded_at 
FROM documents 
ORDER BY uploaded_at DESC 
LIMIT 10;
```

---

## Development Guidelines

### Project Structure

```
ai-rps/
├── src/
│   ├── main/
│   │   ├── java/com/aipanelist/
│   │   │   ├── agents/              # AI agent implementations
│   │   │   │   ├── AIAgent.java     # Abstract base class
│   │   │   │   ├── LeadAnalystAgent.java
│   │   │   │   ├── GeneralAnalystAgent.java
│   │   │   │   ├── MethodologyReviewerAgent.java
│   │   │   │   ├── LiteratureReviewerAgent.java
│   │   │   │   ├── QuickScreenerAgent.java
│   │   │   │   └── FactExtractorAgent.java
│   │   │   ├── api/                 # REST controllers and DTOs
│   │   │   │   ├── DocumentController.java
│   │   │   │   ├── ResultsService.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── *.java (DTOs)
│   │   │   ├── async/               # Async processing
│   │   │   │   └── AsyncAnalysisService.java
│   │   │   ├── config/              # Spring configuration
│   │   │   │   ├── AsyncConfiguration.java
│   │   │   │   ├── NVIDIAConfiguration.java
│   │   │   │   └── *.java
│   │   │   ├── consensus/           # Consensus generation
│   │   │   │   └── ConsensusEngine.java
│   │   │   ├── integration/         # External API clients
│   │   │   │   └── NVIDIAModelClient.java
│   │   │   ├── model/               # Domain models
│   │   │   │   ├── Document.java
│   │   │   │   ├── DocumentChunk.java
│   │   │   │   ├── AnalysisReport.java
│   │   │   │   ├── ConsensusReport.java
│   │   │   │   └── *.java
│   │   │   ├── orchestration/       # Panel orchestration
│   │   │   │   └── PanelOrchestrator.java
│   │   │   ├── processing/          # Document processing
│   │   │   │   ├── DocumentProcessor.java
│   │   │   │   ├── ChunkProcessor.java
│   │   │   │   └── *.java
│   │   │   ├── repository/          # JPA repositories
│   │   │   │   └── *.java
│   │   │   └── upload/              # File upload
│   │   │       └── PDFUploadService.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── logback-spring.xml
│   └── test/
│       └── java/com/aipanelist/     # Tests mirror main structure
├── docker-compose.yml               # Production compose
├── docker-compose.dev.yml           # Development overrides
├── docker-compose.prod.yml          # Production with Nginx
├── Dockerfile                       # Multi-stage build
├── Dockerfile.dev                   # Development image
├── .env.example                     # Environment template
├── pom.xml                          # Maven configuration
└── README.md                        # User documentation
```

### Coding Standards

**Java Style**:
- Follow Google Java Style Guide
- Use meaningful variable names
- Maximum method length: 50 lines
- Maximum class length: 500 lines
- Prefer composition over inheritance

**Documentation**:
- Javadoc for all public classes and methods
- Inline comments for complex logic
- README for each major package

**Testing**:
- Minimum 80% code coverage
- Unit tests for all business logic
- Integration tests for API endpoints
- Property tests for invariants

### Adding New Features

#### 1. Adding a New Agent Type

**Step 1**: Create agent class
```java
package com.aipanelist.agents;

public class StatisticalAnalystAgent extends AIAgent {
    
    public StatisticalAnalystAgent(String agentId, NVIDIAModelClient modelClient) {
        super(agentId, AgentType.STATISTICAL_ANALYST, modelClient);
    }
    
    @Override
    protected String getSystemPrompt() {
        return """
            You are a statistical analysis expert reviewing research documents.
            Focus on:
            - Statistical test selection and appropriateness
            - Power analysis and sample size justification
            - Effect size interpretation
            - Multiple comparison corrections
            - Assumption violations
            
            Provide detailed statistical critique with specific recommendations.
            """;
    }
}
```

**Step 2**: Add enum value
```java
public enum AgentType {
    LEAD_ANALYST,
    GENERAL_ANALYST,
    METHODOLOGY_REVIEWER,
    LITERATURE_REVIEWER,
    QUICK_SCREENER,
    FACT_EXTRACTOR,
    STATISTICAL_ANALYST  // New
}
```

**Step 3**: Register in orchestrator
```java
public AnalysisPanel createPanel(String documentId) {
    AnalysisPanel panel = new AnalysisPanel(documentId);
    
    // Existing agents...
    
    panel.addAgent(new StatisticalAnalystAgent(
        generateAgentId(), 
        modelClient
    ));
    
    return panel;
}
```

**Step 4**: Write tests
```java
@Test
void statisticalAnalystShouldFocusOnStatistics() {
    // Test implementation
}
```

#### 2. Adding a New API Endpoint

**Step 1**: Define DTO
```java
public record DocumentSummaryDTO(
    String documentId,
    String filename,
    ProcessingStatus status,
    LocalDateTime uploadedAt,
    int agentReportsCount
) {}
```

**Step 2**: Add controller method
```java
@GetMapping("/documents")
public ResponseEntity<List<DocumentSummaryDTO>> listDocuments(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
) {
    Page<Document> documents = documentRepository.findAll(
        PageRequest.of(page, size, Sort.by("uploadedAt").descending())
    );
    
    List<DocumentSummaryDTO> summaries = documents.stream()
        .map(this::toSummaryDTO)
        .toList();
    
    return ResponseEntity.ok(summaries);
}
```

**Step 3**: Write tests
```java
@Test
void shouldListDocumentsWithPagination() {
    // Test implementation
}
```

**Step 4**: Update API documentation

---

## Future Enhancements

### Planned Features

#### 1. Multi-Model Support
- Support for additional LLM providers (OpenAI, Anthropic, Cohere)
- Model selection per agent type
- Ensemble consensus across different models

#### 2. Advanced Chunking Strategies
- Semantic chunking using embeddings
- Hierarchical chunking for structured documents
- Adaptive chunk sizing based on content density

#### 3. Interactive Analysis
- Real-time chat with agents about specific findings
- Follow-up questions and clarifications
- Iterative refinement of analysis

#### 4. Batch Processing
- Upload multiple documents simultaneously
- Comparative analysis across documents
- Batch export of results

#### 5. Custom Agent Configuration
- User-defined agent types and prompts
- Adjustable agent weights in consensus
- Domain-specific agent specializations

#### 6. Enhanced Reporting
- PDF export of consensus reports
- Interactive visualizations of agent agreements/disagreements
- Citation extraction and verification

#### 7. Collaboration Features
- Multi-user workspaces
- Shared document libraries
- Commenting and annotation

#### 8. Advanced Analytics
- Trend analysis across multiple documents
- Meta-analysis capabilities
- Citation network visualization

### Technical Improvements

#### 1. Performance Optimization
- Implement caching for repeated analyses
- Optimize database queries with indexes
- Parallel chunk processing per agent

#### 2. Scalability Enhancements
- Kubernetes deployment manifests
- Horizontal pod autoscaling
- Distributed task queue (RabbitMQ/Kafka)

#### 3. Monitoring & Observability
- Prometheus metrics export
- Grafana dashboards
- Distributed tracing with Jaeger

#### 4. Security Hardening
- OAuth 2.0 authentication
- Role-based access control (RBAC)
- Audit logging
- Data encryption at rest

---

## Conclusion

The **AI Research Panel System (AI-RPS)** represents a sophisticated approach to automated research evaluation. By orchestrating six specialized AI agents powered by NVIDIA's advanced language models, the system provides comprehensive, multi-perspective analysis that rivals traditional peer review processes while delivering results in a fraction of the time.

### Key Strengths

1. **Comprehensive Analysis**: Six specialized perspectives ensure thorough evaluation
2. **Scalable Architecture**: Handles documents from 1 page to 500 pages
3. **Production-Ready**: Docker containerization, error handling, monitoring
4. **Extensible Design**: Easy to add new agents, models, or features
5. **Well-Tested**: 152 tests with 87% code coverage

### Getting Started

1. Install Docker Desktop
2. Get free NVIDIA API key
3. Run `docker-compose up -d`
4. Upload PDF to `http://localhost:8080/api/documents/upload`
5. Retrieve results in 30-45 minutes

### Support and Contribution

For issues, questions, or feature requests, please contact the development team or refer to the project repository.

---

**Document Version**: 1.0  
**Last Updated**: January 2024  
**System Version**: AI-RPS v1.0.0  
**Maintained By**: AI Research Panel Development Team

