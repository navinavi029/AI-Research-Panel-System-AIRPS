# Error Handling

---

## Error Response Format

All API errors return a consistent JSON structure:

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable description of what went wrong",
  "timestamp": "2026-04-26T09:03:47",
  "documentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "details": {
    "key": "additional context"
  }
}
```

`documentId` and `details` are omitted when not applicable (`@JsonInclude(NON_NULL)`).

---

## HTTP Status Codes

| Code | Meaning | When it occurs |
|------|---------|---------------|
| `400` | Bad Request | Invalid file, wrong type, empty upload |
| `404` | Not Found | Document ID doesn't exist |
| `409` | Conflict | Requesting results before processing is complete |
| `413` | Payload Too Large | File exceeds 50MB |
| `422` | Unprocessable Entity | Text extraction failed (corrupted content) |
| `500` | Internal Server Error | Unexpected application error |
| `502` | Bad Gateway | NVIDIA API returned an error response |
| `503` | Service Unavailable | Max retries exceeded or circuit breaker open |

---

## Error Codes

### Upload Errors

| Code | HTTP | Description |
|------|------|-------------|
| `INVALID_FILE` | 400 | File is empty or missing from the request |
| `INVALID_FILE_TYPE` | 400 | MIME type is not `application/pdf` |
| `INVALID_PDF` | 400 | File is corrupted, password-protected, or not a valid PDF |
| `FILE_TOO_LARGE` | 413 | File exceeds 50MB (52,428,800 bytes) |
| `UPLOAD_FAILED` | 500 | Server-side error storing the file |

### Document Errors

| Code | HTTP | Description |
|------|------|-------------|
| `DOCUMENT_NOT_FOUND` | 404 | No document exists with the given ID |
| `DOCUMENT_PROCESSING` | 409 | Document is still being processed — poll `/status` |
| `STATUS_RETRIEVAL_FAILED` | 500 | Error retrieving document status |
| `RESULTS_RETRIEVAL_FAILED` | 500 | Error retrieving analysis results |

### Processing Errors

| Code | HTTP | Description |
|------|------|-------------|
| `EXTRACTION_FAILED` | 422 | Text extraction from PDF failed |
| `CONSENSUS_GENERATION_FAILED` | 500 | Consensus engine failed to synthesize reports |
| `ALL_AGENTS_FAILED` | 500 | All 6 agents failed to produce reports |
| `MAX_RETRIES_EXCEEDED` | 503 | NVIDIA API calls exceeded retry limit |

### Export Errors

| Code | HTTP | Description |
|------|------|-------------|
| `PDF_GENERATION_FAILED` | 500 | Error generating the PDF report |
| `EXPORT_FAILED` | 500 | Unexpected error during PDF export |

### External API Errors

| Code | HTTP | Description |
|------|------|-------------|
| `EXTERNAL_API_ERROR` | 502 | NVIDIA API returned an error response |

---

## Retry Behavior

### Per-Chunk Retries (Agent Level)

Each chunk analysis is retried up to 3 times on failure:

| Attempt | Wait | Condition |
|---------|------|-----------|
| Initial | — | First attempt |
| Retry 1 | 1 second | After first failure |
| Retry 2 | 2 seconds | After second failure |
| Retry 3 | 4 seconds | After third failure |
| Give up | — | Mark chunk as failed, continue to next |

**Retryable errors:** 5xx responses, network timeouts, connection errors  
**Non-retryable errors:** 4xx responses (except 429)

### Rate Limit Handling (429)

When the NVIDIA API returns HTTP 429:
1. Read the `Retry-After` header value
2. Wait for the specified duration
3. Retry the request (counts as a retry attempt)

### NVIDIA API Client Retries

The `NVIDIAModelClientImpl` also has its own retry logic independent of the agent-level retries:

| Attempt | Wait |
|---------|------|
| Initial | — |
| Retry 1 | 1 second |
| Retry 2 | 2 seconds |
| Retry 3 | 4 seconds |

---

## Circuit Breaker

The NVIDIA API client is protected by a Resilience4j circuit breaker named `nvidia-api`.

```
CLOSED (normal) ──── too many failures ──▶ OPEN (fail fast)
                                                │
                                           60 seconds
                                                │
                                                ▼
                                         HALF-OPEN (test)
                                                │
                              ┌─────────────────┴──────────────────┐
                              │                                    │
                         success                               failure
                              │                                    │
                              ▼                                    ▼
                           CLOSED                               OPEN
```

| Setting | Value |
|---------|-------|
| Failure rate threshold | 50% |
| Sliding window size | 10 calls |
| Minimum calls before evaluation | 5 |
| Open state duration | 60 seconds |
| Half-open permitted calls | 3 |

When the circuit is open, all NVIDIA API calls fail immediately with a `503 Service Unavailable` response without actually hitting the API.

---

## Graceful Degradation

The system is designed to produce results even when some components fail:

| Scenario | Behavior |
|----------|---------|
| 1–5 agents fail | Consensus generates from remaining successful agents |
| All agents fail | Document status → `FAILED` |
| Some chunks fail per agent | Agent synthesizes from successful chunks, notes gaps |
| All chunks fail per agent | That agent's report is omitted from consensus |
| Consensus engine fails | Document status → `FAILED` |

---

## Logging

All errors are logged with structured context:

```
2026-04-26 09:03:47 [async-1] ERROR c.a.agents.LeadAnalystAgent - 
  Lead Analyst Agent abc-123 failed to analyze chunk 2: 
  APIException: NVIDIA API returned 503
```

Log fields included:
- Timestamp
- Thread name
- Log level
- Logger class
- `documentId` (where applicable)
- `agentId` (where applicable)
- Error message and stack trace

Set `LOG_LEVEL_APP=DEBUG` in `.env` to see detailed processing logs.
