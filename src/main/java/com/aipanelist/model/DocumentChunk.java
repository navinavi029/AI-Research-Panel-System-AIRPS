package com.aipanelist.model;

import jakarta.persistence.*;

/**
 * Entity representing a chunk of a document that has been divided for processing.
 * Large documents are split into chunks to fit within model context windows.
 */
@Entity
@Table(name = "document_chunks")
public class DocumentChunk {
    
    @Id
    @Column(name = "chunk_id", nullable = false, length = 100)
    private String chunkId;
    
    @Column(name = "document_id", nullable = false, length = 100)
    private String documentId;
    
    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;
    
    @Column(name = "total_chunks", nullable = false)
    private int totalChunks;
    
    @Lob
    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    private String chunkText;
    
    @Column(name = "token_count", nullable = false)
    private int tokenCount;
    
    @Column(name = "start_byte_offset", nullable = false)
    private long startByteOffset;
    
    @Column(name = "end_byte_offset", nullable = false)
    private long endByteOffset;
    
    @Column(name = "overlap_tokens", nullable = false)
    private int overlapTokens;
    
    public DocumentChunk() {
    }
    
    public DocumentChunk(String chunkId, String documentId, int sequenceNumber, int totalChunks,
                        String chunkText, int tokenCount, long startByteOffset, long endByteOffset, int overlapTokens) {
        this.chunkId = chunkId;
        this.documentId = documentId;
        this.sequenceNumber = sequenceNumber;
        this.totalChunks = totalChunks;
        this.chunkText = chunkText;
        this.tokenCount = tokenCount;
        this.startByteOffset = startByteOffset;
        this.endByteOffset = endByteOffset;
        this.overlapTokens = overlapTokens;
    }
    
    public String getChunkId() {
        return chunkId;
    }
    
    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }
    
    public String getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    
    public int getSequenceNumber() {
        return sequenceNumber;
    }
    
    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
    
    public int getTotalChunks() {
        return totalChunks;
    }
    
    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }
    
    public String getChunkText() {
        return chunkText;
    }
    
    public void setChunkText(String chunkText) {
        this.chunkText = chunkText;
    }
    
    public int getTokenCount() {
        return tokenCount;
    }
    
    public void setTokenCount(int tokenCount) {
        this.tokenCount = tokenCount;
    }
    
    public long getStartByteOffset() {
        return startByteOffset;
    }
    
    public void setStartByteOffset(long startByteOffset) {
        this.startByteOffset = startByteOffset;
    }
    
    public long getEndByteOffset() {
        return endByteOffset;
    }
    
    public void setEndByteOffset(long endByteOffset) {
        this.endByteOffset = endByteOffset;
    }
    
    public int getOverlapTokens() {
        return overlapTokens;
    }
    
    public void setOverlapTokens(int overlapTokens) {
        this.overlapTokens = overlapTokens;
    }
}
