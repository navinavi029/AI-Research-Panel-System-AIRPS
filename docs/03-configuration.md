# Configuration

All configuration is managed through the `.env` file in the project root. Copy `.env.example` to `.env` and edit it before starting the project.

---

## Required

| Variable | Description |
|----------|-------------|
| `NVIDIA_API_KEY` | Your NVIDIA API key. Get a free key at [build.nvidia.com](https://build.nvidia.com/). The system will not start without this. |

---

## Server

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8080` | The HTTP port the application listens on. Change this if port 8080 is already in use on your machine. |

---

## NVIDIA API

| Variable | Default | Description |
|----------|---------|-------------|
| `NVIDIA_API_ENDPOINT` | `https://integrate.api.nvidia.com/v1` | NVIDIA API base URL. Only change this if you are using a custom or self-hosted endpoint. |
| `NVIDIA_API_MODEL` | `meta/llama-3.3-70b-instruct` | The model used for all 6 agents and the consensus engine. |
| `NVIDIA_API_RATE_LIMIT` | `60` | Maximum requests per minute sent to the NVIDIA API. Reduce this if you hit rate limit errors. |
| `NVIDIA_API_MAX_CONNECTIONS` | `10` | Maximum concurrent HTTP connections to the NVIDIA API. |

---

## Database

| Variable | Default | Description |
|----------|---------|-------------|
| `POSTGRES_PASSWORD` | `changeme` | Password for the PostgreSQL database. Change this for any non-local deployment. |
| `POSTGRES_PORT` | `5432` | The host port PostgreSQL is exposed on. Change this if port 5432 is already in use. |

---

## Logging

| Variable | Default | Options | Description |
|----------|---------|---------|-------------|
| `LOG_LEVEL_ROOT` | `INFO` | `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR` | Root logging level for all libraries and frameworks. |
| `LOG_LEVEL_APP` | `DEBUG` | `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR` | Logging level for application code (`com.aipanelist.*`). |

Set `LOG_LEVEL_APP=DEBUG` to see detailed processing logs including chunk progress, agent status, and NVIDIA API calls.

---

## JVM

| Variable | Default | Description |
|----------|---------|-------------|
| `JAVA_OPTS` | `-Xms512m -Xmx2048m -XX:+UseG1GC` | JVM startup options. Increase `-Xmx` if you process very large documents and see out-of-memory errors. |

**Recommended values by machine RAM:**

| Available RAM | Recommended `JAVA_OPTS` |
|---------------|------------------------|
| 4GB | `-Xms512m -Xmx1536m -XX:+UseG1GC` |
| 8GB | `-Xms512m -Xmx2048m -XX:+UseG1GC` |
| 16GB+ | `-Xms512m -Xmx4096m -XX:+UseG1GC` |

---

## Advanced Processing Settings

These are set directly in `docker-compose.yml` under the `app` service environment section. They rarely need changing.

| Variable | Default | Description |
|----------|---------|-------------|
| `PROCESSING_CHUNK_SIZE` | `100000` | Target token count per document chunk. Larger chunks mean fewer API calls but higher per-call cost. |
| `PROCESSING_MAX_RETRIES` | `3` | Number of retry attempts per failed chunk analysis. |
| `PROCESSING_RETRY_DELAY_MS` | `1000` | Initial retry delay in milliseconds. Doubles on each retry (exponential backoff). |
| `ASYNC_CORE_POOL_SIZE` | `10` | Core thread pool size for async processing. |
| `ASYNC_MAX_POOL_SIZE` | `20` | Maximum thread pool size. Each agent analysis runs in its own thread. |
| `ASYNC_QUEUE_CAPACITY` | `100` | Number of tasks that can be queued when all threads are busy. |

---

## PgAdmin (Optional)

PgAdmin is a web-based database management tool. It is included in `docker-compose.yml` but only starts when the `dev` profile is active.

To start it manually:

```bash
docker-compose --profile dev up -d pgadmin
```

| Variable | Default | Description |
|----------|---------|-------------|
| `PGADMIN_EMAIL` | `admin@aipanelist.local` | Login email for PgAdmin |
| `PGADMIN_PASSWORD` | `admin` | Login password for PgAdmin |
| `PGADMIN_PORT` | `5050` | Host port for PgAdmin UI |

Access at: `http://localhost:5050`

---

## Example `.env` File

```env
# Required
NVIDIA_API_KEY=nvapi-xxxxxxxxxxxxxxxxxxxx

# Database
POSTGRES_PASSWORD=mysecurepassword
POSTGRES_PORT=5432

# Server
SERVER_PORT=8080

# Logging
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=DEBUG

# JVM
JAVA_OPTS=-Xms512m -Xmx2048m -XX:+UseG1GC
```
