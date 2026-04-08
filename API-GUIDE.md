# API Testing Guide

This guide shows you how to test the AI Research Panel System API using Swagger UI or cURL.

## Quick Start

**Open Swagger UI in your browser:**
```
http://localhost:8080/swagger-ui.html
```

**Available Endpoints:**
- POST `/api/documents/upload` - Upload a PDF
- GET `/api/documents/{documentId}/status` - Check processing status
- GET `/api/documents/{documentId}/results` - Get consensus report
- GET `/api/documents/{documentId}/results/detailed` - Get detailed results

---

## Step-by-Step Testing Tutorial

### Step 1: Upload a PDF Document

1. **Find the Upload Endpoint:**
   - Look for `POST /api/documents/upload`
   - Click on it to expand

2. **Click "Try it out" button** (top right of the endpoint section)

3. **Upload a PDF file:**
   - Click "Choose File" button
   - Select a PDF file from your computer (max 50MB, 500 pages)
   - The file should be a research paper or any PDF document

4. **Click "Execute" button**

5. **Check the Response:**
   - **Success (200):** You'll see a JSON response like:
     ```json
     {
       "documentId": "abc123-def456-ghi789",
       "filename": "research-paper.pdf",
       "status": "UPLOADED"
     }
     ```
   - **Copy the `documentId`** - you'll need it for the next steps!

   - **Error (400):** Invalid file (wrong type, too large, corrupted)
   - **Error (500):** Server error

---

### Step 2: Check Processing Status

1. **Find the Status Endpoint:**
   - Look for `GET /api/documents/{documentId}/status`
   - Click on it to expand

2. **Click "Try it out" button**

3. **Enter the documentId:**
   - Paste the `documentId` you copied from Step 1
   - Example: `abc123-def456-ghi789`

4. **Click "Execute" button**

5. **Check the Response:**
   ```json
   {
     "documentId": "abc123-def456-ghi789",
     "status": "PROCESSING",
     "agentProgress": [
       {
         "agentId": "lead-analyst-1",
         "agentType": "LEAD_ANALYST",
         "chunksCompleted": 5,
         "totalChunks": 10,
         "progressPercentage": 50.0
       },
       {
         "agentId": "general-analyst-1",
         "agentType": "GENERAL_ANALYST",
         "chunksCompleted": 3,
         "totalChunks": 10,
         "progressPercentage": 30.0
       }
       // ... more agents
     ],
     "errorMessages": [],
     "estimatedTimeRemaining": "5 minutes"
   }
   ```

6. **Status Values:**
   - `UPLOADED` - File uploaded, waiting to start
   - `PROCESSING` - AI agents are analyzing
   - `COMPLETED` - Analysis finished
   - `FAILED` - Something went wrong

7. **Keep checking until status is `COMPLETED`**

---

### Step 3: Get Consensus Report

1. **Find the Results Endpoint:**
   - Look for `GET /api/documents/{documentId}/results`
   - Click on it to expand

2. **Click "Try it out" button**

3. **Enter the documentId:**
   - Paste the same `documentId` from Step 1

4. **Click "Execute" button**

5. **Check the Response:**
   - **If still processing:** Returns current status (same as Step 2)
   - **If completed:** Returns the consensus report:
     ```json
     {
       "reportId": "report-123",
       "documentId": "abc123-def456-ghi789",
       "commonThemes": [
         "Strong methodology",
         "Limited sample size",
         "Novel approach"
       ],
       "agreements": [
         "All agents agree the research design is sound"
       ],
       "disagreements": [
         "Lead Analyst questions statistical significance"
       ],
       "unifiedRecommendations": [
         "Expand sample size",
         "Add control group"
       ],
       "attributedInsights": {
         "LEAD_ANALYST": ["Key finding 1", "Key finding 2"],
         "METHODOLOGY_REVIEWER": ["Methodology insight 1"]
       },
       "generatedAt": "2026-04-07T09:30:00Z",
       "agentReportsIncluded": 6
     }
     ```

---

### Step 4: Get Detailed Results (Optional)

1. **Find the Detailed Results Endpoint:**
   - Look for `GET /api/documents/{documentId}/results/detailed`
   - Click on it to expand

2. **Click "Try it out" button**

3. **Enter the documentId:**
   - Paste the same `documentId`

4. **Click "Execute" button**

5. **Check the Response:**
   - Returns all 6 individual agent reports PLUS the consensus report
   - Much more detailed than Step 3
   ```json
   {
     "documentId": "abc123-def456-ghi789",
     "agentReports": [
       {
         "reportId": "report-lead-1",
         "agentType": "LEAD_ANALYST",
         "keyFindings": ["Finding 1", "Finding 2"],
         "strengths": ["Strength 1"],
         "weaknesses": ["Weakness 1"],
         "recommendations": ["Recommendation 1"],
         "completedAt": "2026-04-07T09:25:00Z",
         "chunksAnalyzed": 10,
         "chunksFailed": 0
       },
       // ... 5 more agent reports
     ],
     "consensusReport": {
       // Same as Step 3
     }
   }
   ```

---

## Understanding the Response Codes

### HTTP Status Codes

- **200 OK** - Request successful
- **400 Bad Request** - Invalid input (wrong file type, too large, etc.)
- **404 Not Found** - Document ID doesn't exist
- **500 Internal Server Error** - Something went wrong on the server

### Response Schemas

Click on any endpoint's response section to see the full schema:
- Expand "200" to see success response structure
- Expand "400" to see error response structure
- Expand "404" to see not found response structure

---

## Testing Tips

### 1. Test with Different File Types

**Valid:**
```
✓ research-paper.pdf
✓ document.pdf
```

**Invalid (should return 400):**
```
✗ document.docx (not PDF)
✗ image.jpg (not PDF)
✗ huge-file.pdf (over 50MB)
```

### 2. Test Error Scenarios

**Test 404 Error:**
- Use a fake documentId: `fake-id-12345`
- Should return: `Document not found`

**Test 400 Error:**
- Upload a non-PDF file
- Upload a file over 50MB
- Should return: `Invalid file type` or `File too large`

### 3. Monitor Processing

- Keep refreshing the status endpoint
- Watch the `progressPercentage` increase
- Check `estimatedTimeRemaining`

### 4. Compare Reports

- Get consensus report (Step 3)
- Get detailed results (Step 4)
- Compare the insights from different agents

---

## Using cURL (Alternative to Swagger UI)

If you prefer command line:

### Upload a PDF:
```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@/path/to/your/document.pdf" \
  -H "Content-Type: multipart/form-data"
```

### Check Status:
```bash
curl http://localhost:8080/api/documents/YOUR_DOCUMENT_ID/status
```

### Get Results:
```bash
curl http://localhost:8080/api/documents/YOUR_DOCUMENT_ID/results
```

### Get Detailed Results:
```bash
curl http://localhost:8080/api/documents/YOUR_DOCUMENT_ID/results/detailed
```

---

## Troubleshooting

### Swagger UI not loading?
- Check containers are running: `docker ps`
- Check application logs: `docker logs aipanelist-app`
- Verify URL: `http://localhost:8080/swagger-ui.html`

### Upload fails?
- Check file is PDF format
- Check file size is under 50MB
- Check file is not corrupted

### Status always shows PROCESSING?
- Check NVIDIA API key is valid in `.env` file
- Check application logs for errors
- The test API key won't actually process - you need a real NVIDIA API key

### 404 Not Found?
- Double-check the documentId
- Make sure you copied the full ID from upload response
- Check if document was actually uploaded successfully

---

## Next Steps

1. **Get a real NVIDIA API key:**
   - Visit: https://build.nvidia.com/
   - Sign up for free
   - Get your API key
   - Update `.env` file: `NVIDIA_API_KEY=your-real-key`
   - Restart containers: `docker-compose restart`

2. **Test with real documents:**
   - Upload actual research papers
   - Wait for processing to complete
   - Review the AI-generated analysis

3. **Integrate with your application:**
   - Use the API endpoints in your frontend
   - Build a web interface
   - Create automated workflows

---

## Quick Reference

| Endpoint | Method | Purpose | Required Input |
|----------|--------|---------|----------------|
| `/api/documents/upload` | POST | Upload PDF | PDF file |
| `/api/documents/{id}/status` | GET | Check progress | Document ID |
| `/api/documents/{id}/results` | GET | Get consensus | Document ID |
| `/api/documents/{id}/results/detailed` | GET | Get all reports | Document ID |

**Base URL:** `http://localhost:8080`

**Swagger UI:** `http://localhost:8080/swagger-ui.html`

**API Docs JSON:** `http://localhost:8080/v3/api-docs`
