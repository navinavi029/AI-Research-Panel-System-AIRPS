/**
 * Core entity classes and enums for the AI Panelist System.
 * 
 * <p>This package contains JPA entity classes representing the domain model
 * of the AI Panelist System, including documents, extracted text, chunks,
 * analysis reports, consensus reports, and progress tracking.</p>
 * 
 * <h2>Entity Overview:</h2>
 * <ul>
 *   <li>{@link com.aipanelist.model.Document} - Uploaded document metadata and status</li>
 *   <li>{@link com.aipanelist.model.ExtractedDocument} - Extracted text content from PDFs</li>
 *   <li>{@link com.aipanelist.model.DocumentChunk} - Chunks of large documents for processing</li>
 *   <li>{@link com.aipanelist.model.AnalysisReport} - Individual agent analysis reports</li>
 *   <li>{@link com.aipanelist.model.ChunkAnalysis} - Per-chunk analysis by agents</li>
 *   <li>{@link com.aipanelist.model.ConsensusReport} - Synthesized panel consensus</li>
 *   <li>{@link com.aipanelist.model.AgentProgress} - Agent processing progress tracking</li>
 * </ul>
 * 
 * <h2>Enums:</h2>
 * <ul>
 *   <li>{@link com.aipanelist.model.ProcessingStatus} - Document processing lifecycle states</li>
 *   <li>{@link com.aipanelist.model.AgentType} - Specialized AI agent types</li>
 * </ul>
 * 
 * @since 1.0.0
 */
package com.aipanelist.model;
