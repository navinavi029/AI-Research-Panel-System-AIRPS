# Troubleshooting

---

## Startup Issues

### Docker is not running

**Symptom:** `start.bat` shows `[ERROR] Docker is not running`

**Fix:** Open Docker Desktop and wait for it to fully start (the whale icon in the system tray should stop animating).

---

### Port already in use

**Symptom:**
```
Error: Bind for 0.0.0.0:8080 failed: port is already allocated
```

**Fix:** Change `SERVER_PORT` in `.env` to a free port (e.g. `8090`), then restart.

```env
SERVER_PORT=8090
```

Same applies to `POSTGRES_PORT=5432` if PostgreSQL is already running locally.

---

### Application fails to start — database connection refused

**Symptom:** Logs show `Connection refused` or `Unable to acquire JDBC Connection`

**Cause:** The application started before PostgreSQL finished initializing.

**Fix:**
```bash
docker-compose restart app
```

Wait 30 seconds and check health:
```bash
docker-compose ps
```

---

### Build fails — Maven download errors

**Symptom:** Build output shows repeated download failures or `Could not resolve dependencies`

**Cause:** Network issue during first build when Maven downloads dependencies.

**Fix:** Check your internet connection and retry:
```bash
docker-compose up -d --build app
```

---

## API Issues

### NVIDIA API key not working

**Symptom:** Documents get stuck in `ANALYZING` or `FAILED` with API errors in logs

**Check:**
```bash
docker-compose logs app | grep -i "nvidia\|api\|key"
```

**Fix:**
1. Verify the key is valid at [build.nvidia.com](https://build.nvidia.com/)
2. Check `.env` — no quotes, no extra spaces:
   ```env
   NVIDIA_API_KEY=nvapi-xxxxxxxxxxxxxxxxxxxx
   ```
3. Restart after changing `.env`:
   ```bash
   docker-compose restart app
   ```

---

### Rate limit errors (429)

**Symptom:** Logs show `429 Too Many Requests` or `rate limit exceeded`

**Fix:** Reduce the rate limit in `docker-compose.yml`:
```yaml
NVIDIA_API_RATE_LIMIT: 30
```

Then rebuild:
```bash
docker-compose up -d --build app
```

---

### Document stuck in ANALYZING

**Symptom:** Status stays `ANALYZING` for a long time

**Check logs:**
```bash
docker-compose logs -f app
```

Look for:
- `ERROR` lines with the document ID
- Agent timeout messages
- NVIDIA API errors

**Common causes:**
- NVIDIA API is slow or rate-limited — wait it out
- Agent hit max retries — check for `APIException` in logs
- Thread pool exhausted — check for `RejectedExecutionException`

---

### Document status is FAILED

**Symptom:** `/status` returns `"status": "FAILED"`

**Check the error message:**
```bash
curl http://localhost:8080/api/documents/{documentId}/status
```

The response includes an `errorMessage` field with details.

**Common causes and fixes:**

| Error message | Cause | Fix |
|---------------|-------|-----|
| `Document exceeds maximum page limit` | PDF has more than 500 pages | Split the document |
| `Document exceeds maximum token limit` | Document has more than 1M tokens | Split the document |
| `Failed to extract text from PDF` | Corrupted or scanned PDF | Try a different PDF |
| `All agents failed to produce reports` | NVIDIA API issues | Check API key and rate limits |

---

## Performance Issues

### Processing is very slow

**Normal processing times:**
- Single-chunk document (< 100K tokens): 3–10 minutes
- Multi-chunk document: 10–30+ minutes

**If significantly slower:**

1. Check NVIDIA API latency — the free tier can be slow during peak hours
2. Check container resource usage:
   ```bash
   docker stats aipanelist-app aipanelist-postgres
   ```
3. Increase JVM memory if CPU is high and memory is near limit:
   ```env
   JAVA_OPTS=-Xms512m -Xmx4096m -XX:+UseG1GC
   ```

---

### Out of memory errors

**Symptom:** Container restarts, logs show `OutOfMemoryError`

**Fix:** Increase heap size in `.env`:
```env
JAVA_OPTS=-Xms512m -Xmx4096m -XX:+UseG1GC
```

Also increase the memory limit in `docker-compose.yml`:
```yaml
deploy:
  resources:
    limits:
      memory: 4G
```

---

## PDF Issues

### PDF upload rejected with INVALID_PDF

**Cause:** The file may be:
- Password-protected
- Corrupted
- A scanned image PDF with no text layer
- Not actually a PDF (wrong extension)

**Fix:** Open the file in a PDF viewer to confirm it's readable and not password-protected. For scanned PDFs, you'll need to run OCR first.

---

### PDF export returns 500

**Check logs:**
```bash
docker-compose logs app | grep "PDF_GENERATION\|PDFException\|iText"
```

**Common cause:** The consensus report has null fields. Check that the document status is `COMPLETE` before requesting the PDF.

---

## Database Issues

### Viewing database contents

```bash
# Connect to database
docker-compose exec postgres psql -U aipanelist -d aipanelist

# Check recent documents
SELECT document_id, filename, status, uploaded_at 
FROM documents 
ORDER BY uploaded_at DESC 
LIMIT 10;

# Check agent progress for a document
SELECT agent_type, chunks_completed, total_chunks 
FROM agent_progress 
WHERE document_id = 'your-document-id';
```

### Resetting the database

To wipe all data and start fresh:
```bash
docker-compose down -v
start.bat
```

This removes all Docker volumes (database data and uploaded files) and recreates everything from scratch.

---

## Getting More Information

### Enable debug logging

In `.env`:
```env
LOG_LEVEL_APP=DEBUG
LOG_LEVEL_ROOT=DEBUG
```

Restart:
```bash
docker-compose restart app
```

This shows detailed logs including:
- Each chunk being processed
- NVIDIA API request/response details
- Agent progress updates
- Database queries (if `SHOW_SQL=true` is also set)

### Check application health

```bash
curl http://localhost:8080/actuator/health
```

### View all running containers

```bash
docker-compose ps
```

### View container resource usage

```bash
docker stats aipanelist-app aipanelist-postgres
```
