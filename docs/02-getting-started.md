# Getting Started

## Prerequisites

You only need two things:

1. **Docker Desktop** — [Download here](https://www.docker.com/products/docker-desktop)
   - Windows: Docker Desktop 4.0+
   - Make sure Docker Desktop is running before starting the project

2. **NVIDIA API Key** (free) — [Get one at build.nvidia.com](https://build.nvidia.com/)
   - Sign up for a free account
   - Navigate to API Keys and generate a key
   - The free tier is sufficient to run the system

No Java, Maven, PostgreSQL, or any other local dependency is required. Everything runs inside Docker containers.

---

## First-Time Setup

### Step 1 — Clone the repository

```bash
git clone https://github.com/navinavi029/AI-Research-Panel-System-AIRPS.git
cd AI-Research-Panel-System-AIRPS
```

### Step 2 — Run the start script

```cmd
start.bat
```

The script will:

1. Check that Docker is running
2. Create a `.env` file from `.env.example` if one doesn't exist
3. Detect if your NVIDIA API key is not configured and prompt you to enter it
4. Build the Docker images and start all containers
5. Wait for the health check to pass (can take 1–2 minutes on first run while Maven downloads dependencies)
6. Open Swagger UI in your browser automatically

### Step 3 — Verify it's running

Open your browser and go to:

```
http://localhost:8080/actuator/health
```

You should see:

```json
{"status":"UP"}
```

### Step 4 — Open the API documentation

```
http://localhost:8080/swagger-ui.html
```

This gives you an interactive interface to test all endpoints directly from the browser.

---

## Stopping the Project

```cmd
stop.bat
```

This stops the containers without removing them. Your data (uploaded documents, analysis results) is preserved in Docker volumes.

The next time you run `start.bat`, startup will be much faster since the images are already built and the containers already exist.

---

## Subsequent Runs

After the first setup, just run:

```cmd
start.bat
```

It will detect your existing `.env` and start the containers directly.

---

## What Happens on First Run

The first run takes longer because:

1. Docker pulls the base images (`maven:3.9-eclipse-temurin-21-alpine`, `eclipse-temurin:21-jre-alpine`, `postgres:15-alpine`)
2. Maven downloads all project dependencies inside the build container (~200MB)
3. The application compiles and packages

Subsequent runs skip all of this and start in under 30 seconds.

---

## Uploading Your First Document

Once the system is running, upload a PDF:

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@your-paper.pdf"
```

You'll receive a `documentId`. Use it to track progress:

```bash
curl http://localhost:8080/api/documents/{documentId}/status
```

Wait until `status` is `COMPLETE`, then retrieve results:

```bash
# JSON report
curl http://localhost:8080/api/documents/{documentId}/results

# PDF report
curl -OJ http://localhost:8080/api/documents/{documentId}/results/pdf
```

See [API Reference](04-api-reference.md) for full details.
