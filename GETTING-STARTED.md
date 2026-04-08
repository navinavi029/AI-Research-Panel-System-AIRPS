# Getting Started with AI Research Panel System

> **Quick Start**: This guide will get you up and running in under 10 minutes. No Java, Maven, or PostgreSQL installation required - everything runs in Docker!

## What You'll Need

Before starting, make sure you have:

1. **Docker Desktop** installed and running
   - [Download for Windows](https://www.docker.com/products/docker-desktop)
   - [Download for Mac](https://www.docker.com/products/docker-desktop)
   - [Download for Linux](https://docs.docker.com/engine/install/)

2. **NVIDIA API Key** (free)
   - Visit [https://build.nvidia.com/](https://build.nvidia.com/)
   - Sign up for a free account
   - Generate an API key

That's it! No other dependencies needed.

---

## Step-by-Step Setup

### Step 1: Verify Docker is Running

Open a terminal and run:

```bash
docker --version
```

You should see something like `Docker version 24.0.0` or higher.

If you get an error, start Docker Desktop and wait for it to fully launch.

---

### Step 2: Get the NVIDIA API Key

1. Go to [https://build.nvidia.com/](https://build.nvidia.com/)
2. Click "Sign In" or "Get Started"
3. Create a free account (or sign in if you have one)
4. Navigate to the **API Keys** section
5. Click **"Generate API Key"**
6. Copy the key - you'll need it in the next step

**Important**: Keep this key secure and never commit it to version control!

---

### Step 3: Configure Your Environment

#### Option A: Windows Users (Easiest)

1. Open the project folder in File Explorer
2. Double-click `docker-start.bat`
3. The script will guide you through:
   - Creating the `.env` file
   - Entering your NVIDIA API key
   - Choosing deployment mode (select **1** for Production)
4. Wait for services to start (this may take 2-3 minutes on first run)

#### Option B: Manual Setup (All Platforms)

1. Create your environment file:
   ```bash
   cp .env.example .env
   ```

2. Open `.env` in a text editor and set your API key:
   ```env
   NVIDIA_API_KEY=your_actual_api_key_here
   ```

3. Save the file

---

### Step 4: Start the Application

#### Windows:
```bash
docker-start.bat
```
Choose option **1** (Production mode)

#### Linux/Mac:
```bash
make up
```

Or manually:
```bash
docker-compose up -d
```

**What's happening?**
- Docker downloads required images (first time only)
- PostgreSQL database starts
- Application builds and starts
- Health checks verify everything is working

This takes 2-3 minutes on first run, 30 seconds on subsequent starts.

---

### Step 5: Verify It's Working

#### Check Application Health

Open your browser and go to:
```
http://localhost:8080/actuator/health
```

You should see:
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

#### Or Use the Health Check Script

**Windows:**
```bash
docker-health.bat
```

**Linux/Mac:**
```bash
make health
```

---

### Step 6: Upload Your First Document

Now let's test the system with a research paper!

#### Using cURL (Command Line)

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@path/to/your/research-paper.pdf"
```

Replace `path/to/your/research-paper.pdf` with the actual path to a PDF file.

#### Using Swagger UI (Browser)

1. Open [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
2. Find the **POST /api/documents/upload** endpoint
3. Click **"Try it out"**
4. Click **"Choose File"** and select a PDF
5. Click **"Execute"**

**Response:**
```json
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "filename": "research-paper.pdf",
  "status": "UPLOADED"
}
```

**Save the `documentId`** - you'll need it to check status and get results!

---

### Step 7: Check Processing Status

The AI agents are now analyzing your document. This takes 5-15 minutes depending on document size.

#### Check Status

```bash
curl http://localhost:8080/api/documents/{documentId}/status
```

Replace `{documentId}` with your actual document ID.

**Example Response:**
```json
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ANALYZING",
  "progress": {
    "totalChunks": 3,
    "agentProgress": [
      {
        "agentType": "QUICK_SCREENER",
        "chunksCompleted": 3,
        "totalChunks": 3
      },
      {
        "agentType": "FACT_EXTRACTOR",
        "chunksCompleted": 2,
        "totalChunks": 3
      }
    ]
  }
}
```

**Status Values:**
- `UPLOADED` - Document received
- `PROCESSING` - Extracting text
- `ANALYZING` - AI agents working
- `DELIBERATING` - Creating consensus
- `COMPLETE` - Ready to view results!
- `FAILED` - Something went wrong (check logs)

---

### Step 8: Get Your Results

Once status is `COMPLETE`, retrieve the consensus report:

```bash
curl http://localhost:8080/api/documents/{documentId}/results
```

**Response:**
```json
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "consensusReport": {
    "commonThemes": [
      "Strong methodology with appropriate statistical analysis",
      "Limited sample size may affect generalizability"
    ],
    "agreements": [
      "All agents agree the research question is well-defined"
    ],
    "disagreements": [
      "Lead Analyst questions the interpretation of results"
    ],
    "unifiedRecommendations": [
      "Consider expanding sample size in future studies"
    ],
    "keyInsights": [
      "Novel approach to data collection"
    ]
  }
}
```

#### Get Detailed Agent Reports

To see what each individual AI agent found:

```bash
curl http://localhost:8080/api/documents/{documentId}/results/detailed
```

This includes reports from all 6 specialized agents plus the consensus.

---

## What's Next?

### Explore the API

Visit the interactive API documentation:
```
http://localhost:8080/swagger-ui.html
```

### View Logs

**Windows:**
```bash
docker-logs.bat
```

**Linux/Mac:**
```bash
make logs
```

### Stop the Application

**Windows:**
```bash
docker-stop.bat
```

**Linux/Mac:**
```bash
make down
```

### Development Mode

Want to modify the code? Start in development mode:

**Windows:**
```bash
docker-start.bat
# Choose option 2 (Development)
```

**Linux/Mac:**
```bash
make dev
```

Development mode includes:
- Hot reload on code changes
- Remote debugging on port 5005
- PgAdmin at http://localhost:5050
- Verbose logging

---

## Common Issues

### "Docker is not running"

**Solution**: Start Docker Desktop and wait for it to fully launch (look for the whale icon in your system tray).

### "Port 8080 is already in use"

**Solution**: Another application is using port 8080. Either:
1. Stop the other application
2. Change the port in `.env`:
   ```env
   SERVER_PORT=8081
   ```

### "NVIDIA API authentication failed"

**Solution**: 
1. Verify your API key is correct in `.env`
2. Check the key is still valid at https://build.nvidia.com/
3. Ensure there are no extra spaces or quotes around the key

### "Database connection failed"

**Solution**: Wait 30-60 seconds for PostgreSQL to fully start, then restart the app:
```bash
docker-compose restart app
```

### "File upload failed"

**Solution**: 
1. Check file is a valid PDF
2. Ensure file is under 50MB
3. Verify file is not corrupted

---

## Getting Help

### Check Logs

Logs contain detailed error messages:

```bash
# View all logs
docker-compose logs -f

# View just application logs
docker-compose logs -f app

# View last 100 lines
docker-compose logs --tail=100 app
```

### Documentation

- **Full README**: [README.md](README.md)
- **Docker Guide**: [README-DOCKER.md](README-DOCKER.md)
- **API Testing**: [SWAGGER-TESTING-GUIDE.md](SWAGGER-TESTING-GUIDE.md)
- **Contributing**: [CONTRIBUTING.md](CONTRIBUTING.md)

### Community

- **Report Issues**: [GitHub Issues](https://github.com/navinavi029/AI-Research-Panel-System-AIRPS/issues)
- **Ask Questions**: [GitHub Discussions](https://github.com/navinavi029/AI-Research-Panel-System-AIRPS/discussions)

---

## Quick Reference

### Essential Commands

| Action | Windows | Linux/Mac |
|--------|---------|-----------|
| Start | `docker-start.bat` | `make up` |
| Stop | `docker-stop.bat` | `make down` |
| Logs | `docker-logs.bat` | `make logs` |
| Health | `docker-health.bat` | `make health` |
| Restart | `docker-compose restart` | `make restart` |

### API Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/documents/upload` | POST | Upload PDF |
| `/api/documents/{id}/status` | GET | Check status |
| `/api/documents/{id}/results` | GET | Get consensus |
| `/api/documents/{id}/results/detailed` | GET | Get all reports |
| `/actuator/health` | GET | Health check |
| `/swagger-ui.html` | GET | API docs |

### Status Flow

```
UPLOADED → PROCESSING → ANALYZING → DELIBERATING → COMPLETE
```

---

## Success! 🎉

You now have a fully functional AI Research Panel System running locally!

Try uploading a research paper and watch as 6 specialized AI agents analyze it from different perspectives, then collaborate to produce a comprehensive consensus report.

**Happy analyzing!**
