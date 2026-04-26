# Database Schema

The database is PostgreSQL 15. Schema is managed automatically by Hibernate (`ddl-auto=update`) — no manual migrations are needed.

The `init-db/01-init.sql` script runs once on first container start to create required PostgreSQL extensions (`uuid-ossp`, `pg_trgm`) and set permissions.

---

## Tables

### `documents`

Tracks every uploaded document and its current processing state.

| Column | Type | Description |
|--------|------|-------------|
| `document_id` | VARCHAR(100) PK | UUID generated at upload time |
| `filename` | VARCHAR(255) | Original filename as uploaded |
| `file_size_bytes` | BIGINT | File size in bytes |
| `uploaded_at` | TIMESTAMP | When the file was uploaded |
| `status` | VARCHAR(20) | Current `ProcessingStatus` enum value |
| `error_message` | TEXT | Error details if status is `FAILED` |
| `total_tokens` | INT | Approximate token count (set after extraction) |
| `total_pages` | INT | Page count (set after extraction) |

---

### `extracted_documents`

Stores the full extracted text from each document.

| Column | Type | Description |
|--------|------|-------------|
| `document_id` | VARCHAR(100) PK/FK | References `documents.document_id` |
| `extracted_text` | TEXT (LOB) | Full extracted text content |
| `token_count` | INT | Approximate token count |
| `extracted_at` | TIMESTAMP | When extraction completed |
| `reading_order_preserved` | BOOLEAN | Always `true` (PDFBox `setSortByPosition`) |

---

### `document_chunks`

Stores the chunked segments of large documents.

| Column | Type | Description |
|--------|------|-------------|
| `chunk_id` | VARCHAR(100) PK | UUID |
| `document_id` | VARCHAR(100) FK | References `documents.document_id` |
| `sequence_number` | INT | Position in document (1-based) |
| `total_chunks` | INT | Total number of chunks for this document |
| `chunk_text` | TEXT (LOB) | The chunk content |
| `token_count` | INT | Approximate token count for this chunk |
| `start_byte_offset` | INT | Start position in original extracted text |
| `end_byte_offset` | INT | End position in original extracted text |
| `overlap_tokens` | INT | Number of overlap tokens at start (0 for first chunk) |

---

### `analysis_reports`

One row per agent per document. Stores the synthesized report from each agent.

| Column | Type | Description |
|--------|------|-------------|
| `report_id` | VARCHAR(100) PK | UUID |
| `document_id` | VARCHAR(100) FK | References `documents.document_id` |
| `agent_id` | VARCHAR(100) | UUID of the agent instance |
| `agent_type` | VARCHAR(50) | `AgentType` enum value (e.g. `LEAD_ANALYST`) |
| `key_findings` | TEXT (LOB) | Most important discoveries |
| `strengths` | TEXT (LOB) | Positive aspects identified |
| `weaknesses` | TEXT (LOB) | Limitations and flaws identified |
| `recommendations` | TEXT (LOB) | Actionable improvement suggestions |
| `completed_at` | TIMESTAMP | When the agent finished |
| `chunks_analyzed` | INT | Number of chunks successfully analyzed |
| `chunks_failed` | INT | Number of chunks that failed after all retries |

---

### `chunk_analyses`

One row per chunk per agent. Stores the raw analysis of each individual chunk.

| Column | Type | Description |
|--------|------|-------------|
| `analysis_id` | VARCHAR(100) PK | UUID |
| `report_id` | VARCHAR(100) FK | References `analysis_reports.report_id` |
| `chunk_id` | VARCHAR(100) FK | References `document_chunks.chunk_id` |
| `chunk_sequence` | INT | Sequence number of the chunk |
| `findings` | TEXT (LOB) | Raw findings from this chunk |
| `context_summary` | TEXT (LOB) | Summary carried forward to next chunk |
| `analyzed_at` | TIMESTAMP | When this chunk was analyzed |

---

### `consensus_reports`

One row per document. The final synthesized output from the consensus engine.

| Column | Type | Description |
|--------|------|-------------|
| `report_id` | VARCHAR(100) PK | UUID prefixed with `consensus-` |
| `document_id` | VARCHAR(100) FK | References `documents.document_id` |
| `common_themes` | TEXT (LOB) | Themes across multiple agents |
| `agreements` | TEXT (LOB) | Points of consensus |
| `disagreements` | TEXT (LOB) | Conflicting findings |
| `unified_recommendations` | TEXT (LOB) | Synthesized panel recommendations |
| `attributed_insights` | TEXT (LOB) | Agent-specific unique contributions |
| `generated_at` | TIMESTAMP | When consensus was generated |
| `agent_reports_included` | INT | Number of agent reports used |

---

### `agent_progress`

Real-time chunk completion tracking per agent. Used by the `/status` endpoint.

| Column | Type | Description |
|--------|------|-------------|
| `progress_id` | VARCHAR(100) PK | UUID |
| `document_id` | VARCHAR(100) FK | References `documents.document_id` |
| `agent_id` | VARCHAR(100) | UUID of the agent instance |
| `agent_type` | VARCHAR(50) | `AgentType` enum value |
| `chunks_completed` | INT | Number of chunks finished so far |
| `total_chunks` | INT | Total chunks to process |
| `last_updated` | TIMESTAMP | Last update time |

---

## Entity Relationships

```
documents (1) ──────────── (1) extracted_documents
documents (1) ──────────── (N) document_chunks
documents (1) ──────────── (N) analysis_reports
documents (1) ──────────── (1) consensus_reports
documents (1) ──────────── (N) agent_progress
analysis_reports (1) ────── (N) chunk_analyses
```

---

## Database Initialization

The `init-db/01-init.sql` script runs automatically when the PostgreSQL container starts for the first time. It:

1. Creates the `uuid-ossp` extension (UUID generation functions)
2. Creates the `pg_trgm` extension (trigram text search)
3. Creates an `update_updated_at_column()` trigger function
4. Grants all privileges on the database, tables, and sequences to the `aipanelist` user
5. Sets default privileges for future tables

Hibernate then creates all tables and indexes based on the JPA entity annotations.

---

## Connecting to the Database

The database is exposed on port 5432 (configurable via `POSTGRES_PORT` in `.env`).

**Connection details:**

| Setting | Value |
|---------|-------|
| Host | `localhost` |
| Port | `5432` (or your `POSTGRES_PORT`) |
| Database | `aipanelist` |
| Username | `aipanelist` |
| Password | Value of `POSTGRES_PASSWORD` in `.env` |

**Connect with psql:**
```bash
docker-compose exec postgres psql -U aipanelist -d aipanelist
```
