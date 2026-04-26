# Consensus Engine

After all 6 agents complete their analysis, the consensus engine makes one final call to the NVIDIA model to synthesize all reports into a single unified output.

---

## How It Works

```
Collect all AnalysisReports (up to 6)
        │
        ▼
Build synthesis prompt
(all agent findings formatted together)
        │
        ▼
Call NVIDIA model
(temperature=0.7, max_tokens=4000)
        │
        ▼
Parse response into 5 sections
        │
        ▼
Store ConsensusReport entity
        │
        ▼
Document status → COMPLETE
```

---

## System Prompt

The consensus engine uses this system prompt:

> *"You are an expert at synthesizing multiple expert analyses into a unified consensus report. Analyze the provided agent reports and identify common themes, areas of agreement, areas of disagreement, and generate unified recommendations. Attribute insights to specific agent types."*

---

## Model Parameters

| Parameter | Value |
|-----------|-------|
| Model | `meta/llama-3.3-70b-instruct` |
| Temperature | `0.7` |
| Max tokens | `4000` |

---

## Output Sections

The consensus report contains 5 sections:

### Common Themes
Themes and patterns that appeared across multiple agent analyses. These are the recurring observations that multiple agents independently identified, giving them higher confidence.

### Agreements
Specific points where agents reached consensus — findings that multiple agents explicitly agreed on. These represent the most reliable conclusions from the analysis.

### Disagreements
Areas where agents had differing perspectives or conflicting findings. These are important to surface because they highlight genuine ambiguity or complexity in the research.

### Unified Recommendations
Synthesized recommendations from the full panel. These are not just a list of each agent's recommendations — they are synthesized into a coherent, prioritized set of actionable suggestions.

### Attributed Insights
Unique contributions attributed to specific agents. These are findings that only one agent identified, which may represent specialized insights from that agent's particular analytical lens.

---

## Graceful Degradation

The consensus engine is designed to work even when some agents fail:

| Scenario | Behavior |
|----------|---------|
| All 6 agents succeed | Consensus uses all 6 reports |
| 4–5 agents succeed | Consensus uses available reports, `agentReportsIncluded` reflects actual count |
| 1–3 agents succeed | Consensus still generates, but quality may be reduced |
| 0 agents succeed | Document status set to `FAILED` — no consensus generated |

The `agentReportsIncluded` field in the consensus report tells you how many agent reports were actually used.

---

## Consensus Report Fields

| Field | Type | Description |
|-------|------|-------------|
| `reportId` | string | Unique ID prefixed with `consensus-` |
| `documentId` | string | The document this report belongs to |
| `commonThemes` | string | Themes across multiple agents |
| `agreements` | string | Points of consensus |
| `disagreements` | string | Conflicting findings |
| `unifiedRecommendations` | string | Synthesized panel recommendations |
| `attributedInsights` | string | Agent-specific unique contributions |
| `generatedAt` | datetime | When the consensus was generated |
| `agentReportsIncluded` | integer | Number of agent reports used |
