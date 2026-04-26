# AI Agents

The system uses 6 specialized AI agents that analyze each document in parallel. Every agent is built on the same base class (`AIAgent`) but has a distinct system prompt that defines its analytical focus.

---

## Base Agent Behavior

All agents share the same processing pattern:

### Chunk Processing Loop

```
For each chunk (sequential order):
    1. Build prompt:
       - System prompt (agent specialization)
       - Context summary from previous chunks (max 800 chars)
       - Current chunk text
    2. Call NVIDIA API
    3. On failure: retry up to 3× with exponential backoff
    4. Store ChunkAnalysis result
    5. Update AgentProgress

After all chunks:
    Synthesize all chunk analyses into one AnalysisReport
    Store AnalysisReport to database
```

### Context Carryover

Each chunk prompt includes a summary of findings from all previous chunks (truncated to 800 characters). This ensures the agent maintains coherence across a multi-chunk document — for example, it won't re-introduce concepts already covered or lose track of the document's narrative.

### Retry Behavior

| Attempt | Wait before retry |
|---------|------------------|
| 1st retry | 1 second |
| 2nd retry | 2 seconds |
| 3rd retry | 4 seconds |
| After 3rd failure | Mark chunk as failed, continue |

**Retryable:** 5xx errors, network timeouts, 429 rate limits  
**Non-retryable:** 4xx errors (except 429)

### Report Structure

Each agent produces an `AnalysisReport` with:

| Field | Description |
|-------|-------------|
| `keyFindings` | The most important discoveries from the analysis |
| `strengths` | Positive aspects of the research |
| `weaknesses` | Limitations, flaws, or gaps identified |
| `recommendations` | Actionable suggestions for improvement |
| `chunksAnalyzed` | Number of chunks successfully analyzed |
| `chunksFailed` | Number of chunks that failed after all retries |

---

## Agent 1 — Lead Analyst

**Type:** `LEAD_ANALYST`

**Role:** Deep critical analysis of research validity and overall scientific quality.

**Focus areas:**
- Methodological rigor and appropriateness
- Whether conclusions are supported by the evidence
- Alternative explanations and confounding factors
- Significance and impact of the research findings
- Key limitations and how they affect the results
- Overall contribution to the field

**Key questions this agent answers:**
- Is the research question clearly defined and significant?
- Are the methods appropriate for answering the research question?
- Is the evidence sufficient to support the conclusions?
- Are alternative explanations adequately addressed?
- What are the key limitations?

**Output emphasis:** High-level strategic assessment — the "big picture" view of research quality.

---

## Agent 2 — General Analyst

**Type:** `GENERAL_ANALYST`

**Role:** Holistic review covering all dimensions of the document.

**Focus areas:**
- Content quality, organization, and logical flow
- Clarity and accessibility of writing
- Completeness and thoroughness of coverage
- Internal consistency across sections
- Effectiveness of figures, tables, and supplementary materials
- Overall reader experience

**Key questions this agent answers:**
- Is the document well-organized and easy to follow?
- Are key concepts clearly explained and defined?
- Is the writing clear and appropriate for the audience?
- Are all necessary sections present and adequately developed?
- Is the document internally consistent?

**Output emphasis:** Broad multi-dimensional assessment — covers everything the other agents don't specialize in.

---

## Agent 3 — Methodology Reviewer

**Type:** `METHODOLOGY_REVIEWER`

**Role:** Expert evaluation of research design, statistical approaches, and data analysis.

**Focus areas:**
- Appropriateness of experimental design for the research question
- Sampling strategies and sample size justification
- Statistical test selection and assumption validation
- Reproducibility and transparency of methods
- Bias control and confounding variable management
- Data handling and quality control procedures

**Key questions this agent answers:**
- Is the research design appropriate for the research question?
- Are the methods clearly described and reproducible?
- Is the sample size adequate and justified?
- Are statistical tests appropriate for the data and hypotheses?
- Are statistical assumptions met?
- Are potential sources of bias adequately controlled?

**Output emphasis:** Technical methodological assessment — the most rigorous quantitative evaluation.

---

## Agent 4 — Literature Reviewer

**Type:** `LITERATURE_REVIEWER`

**Role:** Assessment of scholarly context, citations, and theoretical framework.

**Focus areas:**
- Comprehensiveness and currency of the literature review
- Citation quality, accuracy, and completeness
- Theoretical framework coherence and justification
- Novelty and contribution relative to prior work
- Gaps in literature coverage
- Integration of prior research into the current study

**Key questions this agent answers:**
- Is the literature review comprehensive and up-to-date?
- Are key theoretical concepts grounded in literature?
- Are citations appropriate and sufficient?
- How does this work relate to and build upon prior research?
- Are there important gaps in literature coverage?
- Does the research make a clear contribution to the field?

**Output emphasis:** Scholarly positioning — how the work fits within the broader research landscape.

---

## Agent 5 — Quick Screener

**Type:** `QUICK_SCREENER`

**Role:** Rapid triage, key claims identification, and red flag detection.

**Focus areas:**
- Main research questions and primary claims
- Immediate red flags or critical concerns
- Whether key claims are adequately supported
- High-priority aspects requiring deeper scrutiny
- Obvious errors or inconsistencies

**Key questions this agent answers:**
- What are the main research questions and claims?
- Are there any immediate red flags or concerns?
- What are the most important findings?
- Are key claims adequately supported?
- What aspects require deeper scrutiny?
- Are there any obvious errors or inconsistencies?

**Output emphasis:** Rapid triage — highlights the most critical issues for quick decision-making.

---

## Agent 6 — Fact Extractor

**Type:** `FACT_EXTRACTOR`

**Role:** Structured extraction of facts, data points, and key details.

**Focus areas:**
- Key facts, data points, and numerical results
- Sample characteristics (size, demographics, parameters)
- Methodological details and instruments used
- Primary outcomes and effect sizes
- Dates, locations, and contextual details

**Key questions this agent answers:**
- What are the key factual claims and data points?
- What are the main numerical results and statistics?
- What are the sample characteristics?
- What methods and procedures were used?
- What are the primary outcomes and effect sizes?

**Output emphasis:** Structured factual information — organized for easy reference rather than interpretation.

---

## Agent Comparison

| Agent | Depth | Breadth | Technical | Contextual | Speed |
|-------|-------|---------|-----------|------------|-------|
| Lead Analyst | ★★★★★ | ★★★☆☆ | ★★★★☆ | ★★★☆☆ | ★★★☆☆ |
| General Analyst | ★★★☆☆ | ★★★★★ | ★★☆☆☆ | ★★★☆☆ | ★★★★☆ |
| Methodology Reviewer | ★★★★★ | ★★☆☆☆ | ★★★★★ | ★★☆☆☆ | ★★★☆☆ |
| Literature Reviewer | ★★★★☆ | ★★★☆☆ | ★★☆☆☆ | ★★★★★ | ★★★☆☆ |
| Quick Screener | ★★☆☆☆ | ★★★★☆ | ★★★☆☆ | ★★☆☆☆ | ★★★★★ |
| Fact Extractor | ★★★☆☆ | ★★★★☆ | ★★★☆☆ | ★★☆☆☆ | ★★★★☆ |
