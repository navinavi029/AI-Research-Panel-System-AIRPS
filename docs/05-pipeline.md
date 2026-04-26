# Processing Pipeline

When a document is uploaded, processing runs entirely in the background on a Spring async thread pool. The client polls `/status` to track progress.

---

## Status Transitions

```
UPLOADED → PROCESSING → ANALYZING → DELIBERATING → COMPLETE
                                                  ↘
                                               FAILED (any stage)
```

| Status | Stage | What's happening |
|--------|-------|-----------------|
| `UPLOADED` | — | File stored, record created, async pipeline triggered |
| `PROCESSING` | Stage 1 | Text extraction from PDF |
| `ANALYZING` | Stages 2–3 | Chunking + 6 agents running in parallel |
| `DELIBERATING` | Stage 4 | Consensus engine synthesizing reports |
| `COMPLETE` | — | All done, results available |
| `FAILED` | Any | Error occurred, message stored on document record |

---

## Stage 1 — Upload & Validation

**Triggered by:** `POST /api/documents/upload`

**Performed synchronously** (before the async pipeline starts):

1. Validate file is not empty
2. Validate MIME type is `application/pdf`
3. Validate file size ≤ 50MB (52,428,800 bytes)
4. Load PDF with Apache PDFBox to validate structure — rejects corrupted or password-protected files
5. Generate a UUID as `documentId`
6. Store file to `/app/data/uploads/{documentId}.pdf`
7. Create `Document` entity in database with `status=UPLOADED`
8. Return `documentId` to client
9. Trigger async pipeline

---

## Stage 2 — Text Extraction

**Status:** `PROCESSING`

1. Load PDF bytes from disk
2. Open with Apache PDFBox `PDDocument`
3. Validate page count ≤ 500 pages
4. Configure `PDFTextStripper` with `setSortByPosition(true)` to preserve logical reading order
5. Extract full text
6. Calculate token count: `text.length() / 4` (1 token ≈ 4 characters)
7. Validate token count ≤ 1,000,000 tokens
8. Store `ExtractedDocument` entity (full text as LOB)
9. Update `Document` entity with `totalPages` and `totalTokens`

---

## Stage 3 — Document Chunking

**Status:** `ANALYZING` (set at start of this stage)

### Small documents (≤ 100,000 tokens)

A single chunk is created containing the full document text. No splitting occurs.

### Large documents (> 100,000 tokens)

Semantic chunking is performed to split the document at logical boundaries:

**Boundary priority (highest to lowest):**
1. Section headers — markdown style (`## Introduction`) or colon style (`Introduction:`)
2. Paragraph breaks — two or more consecutive newlines
3. Sentence endings — period followed by whitespace

**Algorithm:**
1. Scan forward from the current position
2. Find the best semantic boundary within ±10% of the 100,000 token target
3. Split at that boundary
4. Add a 500-token overlap at the start of the next chunk (taken from the end of the previous chunk)
5. Repeat until the full document is chunked

**Result:** A list of `DocumentChunk` entities stored in the database, each with:
- `sequenceNumber` — position in the document (1-based)
- `totalChunks` — total number of chunks
- `chunkText` — the chunk content
- `tokenCount` — approximate token count
- `startByteOffset` / `endByteOffset` — position in original text
- `overlapTokens` — number of overlap tokens at the start (0 for first chunk)

---

## Stage 4 — Parallel Agent Analysis

**Status:** `ANALYZING`

All 6 agents are submitted to a Java `ExecutorService` simultaneously. Each agent runs in its own thread with a 30-minute timeout.

**Per-agent processing:**

```
For each chunk (in sequence order):
    1. Build prompt with:
       - Agent system prompt (specialization instructions)
       - Context summary from previous chunks (truncated to 800 chars)
       - Current chunk text
    2. Send to NVIDIA API
    3. On failure: retry up to 3 times
       - Attempt 1: wait 1s
       - Attempt 2: wait 2s
       - Attempt 3: wait 4s
    4. On persistent failure: mark chunk as failed, continue to next chunk
    5. Store ChunkAnalysis entity
    6. Update AgentProgress entity

After all chunks:
    Synthesize all ChunkAnalysis results into a single AnalysisReport
    Store AnalysisReport entity
```

**Retryable errors:** 5xx responses, network timeouts, 429 rate limits  
**Non-retryable errors:** 4xx responses (except 429)

**Graceful degradation:** If some chunks fail, the agent still synthesizes a report from the successful chunks, noting the gaps. If all chunks fail for an agent, that agent's report is omitted from consensus.

---

## Stage 5 — Consensus Generation

**Status:** `DELIBERATING`

1. Collect all available `AnalysisReport` entities for the document
2. Build a synthesis prompt containing all agent reports formatted with their type and findings
3. Call NVIDIA model with:
   - System prompt: *"You are an expert at synthesizing multiple expert analyses into a unified consensus report."*
   - Temperature: 0.7
   - Max tokens: 4000
4. Parse the model response into 5 sections using regex (section headers or colon format)
5. Create `ConsensusReport` entity with all 5 sections
6. Update document status to `COMPLETE`

**If all agents failed:** Status is set to `FAILED` before reaching this stage.

---

## Thread Pool Configuration

| Setting | Value | Description |
|---------|-------|-------------|
| Core pool size | 10 | Threads always kept alive |
| Max pool size | 20 | Maximum concurrent threads |
| Queue capacity | 100 | Tasks queued when all threads busy |
| Thread prefix | `async-` | Thread name prefix in logs |

Each document upload consumes one async thread for the pipeline orchestration, plus up to 6 threads for parallel agent execution.

---

## Timing Expectations

| Stage | Typical Duration |
|-------|-----------------|
| Upload + validation | < 5 seconds |
| Text extraction | 5–30 seconds |
| Chunking | < 5 seconds |
| Agent analysis (per chunk) | 30s–5 minutes per agent |
| Consensus generation | 1–3 minutes |
| **Total (single-chunk doc)** | **3–10 minutes** |
| **Total (multi-chunk doc)** | **10–30+ minutes** |

Timing depends heavily on NVIDIA API response times and document size.
