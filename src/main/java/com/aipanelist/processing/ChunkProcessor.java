package com.aipanelist.processing;

import com.aipanelist.model.DocumentChunk;
import com.aipanelist.model.ExtractedDocument;

import java.util.List;

/**
 * Service interface for chunking large documents into manageable segments.
 * 
 * Divides documents that exceed context window limits into chunks with semantic
 * boundaries and overlap for context continuity. Chunks are created at section
 * headers, paragraph breaks, or sentence endings to preserve meaning.
 */
public interface ChunkProcessor {
    
    /**
     * Chunks a document if it exceeds the token threshold.
     * Documents under 100,000 tokens are returned as a single chunk.
     * Larger documents are divided into multiple chunks with semantic boundaries.
     * 
     * @param document the extracted document to chunk
     * @return a list of document chunks (single chunk if under threshold)
     */
    List<DocumentChunk> chunkDocument(ExtractedDocument document);
    
    /**
     * Identifies semantic boundaries in text for intelligent chunking.
     * Detects section headers, paragraph breaks, and sentence endings.
     * 
     * @param text the text to analyze for boundaries
     * @return a list of semantic boundaries with their positions and types
     */
    List<SemanticBoundary> identifyBoundaries(String text);
    
    /**
     * Creates a document chunk with the specified parameters.
     * 
     * @param documentId the document identifier
     * @param text the chunk text content
     * @param sequence the sequence number of this chunk (1-based)
     * @param totalChunks the total number of chunks in the document
     * @param startOffset the byte offset where this chunk starts in the original text
     * @param endOffset the byte offset where this chunk ends in the original text
     * @param overlapTokens the number of tokens overlapping with the previous chunk
     * @return the created DocumentChunk entity
     */
    DocumentChunk createChunk(String documentId, String text, int sequence, int totalChunks,
                             long startOffset, long endOffset, int overlapTokens);
    
    /**
     * Represents a semantic boundary in text for chunking purposes.
     */
    class SemanticBoundary {
        private final int position;
        private final BoundaryType type;
        
        public SemanticBoundary(int position, BoundaryType type) {
            this.position = position;
            this.type = type;
        }
        
        public int getPosition() {
            return position;
        }
        
        public BoundaryType getType() {
            return type;
        }
        
        public enum BoundaryType {
            SECTION_HEADER,  // Highest priority
            PARAGRAPH_BREAK, // Medium priority
            SENTENCE_ENDING  // Lowest priority
        }
    }
}
