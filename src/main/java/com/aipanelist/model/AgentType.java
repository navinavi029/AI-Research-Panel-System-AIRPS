package com.aipanelist.model;

/**
 * Enum representing the specialized types of AI agents in the panel.
 * Each agent type focuses on a specific aspect of document analysis.
 */
public enum AgentType {
    /**
     * Deep critical analysis, research validity, and overall study quality
     */
    LEAD_ANALYST,
    
    /**
     * Comprehensive review covering all aspects of the document
     */
    GENERAL_ANALYST,
    
    /**
     * Research methodology, statistical approaches, and data analysis techniques
     */
    METHODOLOGY_REVIEWER,
    
    /**
     * Literature context, citations, theoretical framework, and related work
     */
    LITERATURE_REVIEWER,
    
    /**
     * Initial screening, key claims identification, and specific aspect analysis
     */
    QUICK_SCREENER,
    
    /**
     * Fact extraction, data points, and structured summaries
     */
    FACT_EXTRACTOR
}
