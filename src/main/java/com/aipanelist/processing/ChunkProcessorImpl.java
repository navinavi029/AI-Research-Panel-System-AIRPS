package com.aipanelist.processing;

import com.aipanelist.model.DocumentChunk;
import com.aipanelist.model.ExtractedDocument;
import com.aipanelist.repository.DocumentChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of ChunkProcessor for dividing large documents into chunks.
 * 
 * Uses semantic boundary detection to split documents at logical points:
 * section headers, paragraph breaks, and sentence endings. Includes overlap
 * between chunks to preserve context continuity.
 */
@Service
public class ChunkProcessorImpl implements ChunkProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(ChunkProcessorImpl.class);
    
    // Chunking parameters
    private static final int CHUNK_TOKEN_THRESHOLD = 100_000;
    private static final int TARGET_CHUNK_SIZE = 100_000;
    private static final int OVERLAP_TOKENS = 500;
    private static final int CHARACTERS_PER_TOKEN = 4;
    
    // Regex patterns for semantic boundary detection
    private static final Pattern SECTION_HEADER_HASH = Pattern.compile("^#+\\s+.+$", Pattern.MULTILINE);
    private static final Pattern SECTION_HEADER_COLON = Pattern.compile("^[A-Z][^.]+:$", Pattern.MULTILINE);
    private static final Pattern PARAGRAPH_BREAK = Pattern.compile("\\n\\n+");
    private static final Pattern SENTENCE_ENDING = Pattern.compile("\\.\\s+");
    
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentProcessor documentProcessor;
    
    public ChunkProcessorImpl(DocumentChunkRepository documentChunkRepository,
                             DocumentProcessor documentProcessor) {
        this.documentChunkRepository = documentChunkRepository;
        this.documentProcessor = documentProcessor;
    }
    
    @Override
    public List<DocumentChunk> chunkDocument(ExtractedDocument document) {
        logger.info("Chunking document: {} ({} tokens)", 
            document.getDocumentId(), document.getTokenCount());
        
        // If document is under threshold, return as single chunk
        if (document.getTokenCount() <= CHUNK_TOKEN_THRESHOLD) {
            logger.debug("Document under threshold, creating single chunk");
            DocumentChunk singleChunk = createChunk(
                document.getDocumentId(),
                document.getExtractedText(),
                1,
                1,
                0,
                document.getExtractedText().length(),
                0
            );
            documentChunkRepository.save(singleChunk);
            return List.of(singleChunk);
        }
        
        // Document exceeds threshold, perform chunking
        logger.debug("Document exceeds threshold, performing semantic chunking");
        List<DocumentChunk> chunks = performSemanticChunking(document);
        
        // Save all chunks
        documentChunkRepository.saveAll(chunks);
        
        logger.info("Created {} chunks for document: {}", chunks.size(), document.getDocumentId());
        return chunks;
    }
    
    @Override
    public List<SemanticBoundary> identifyBoundaries(String text) {
        List<SemanticBoundary> boundaries = new ArrayList<>();
        
        // Find section headers (markdown-style)
        Matcher hashMatcher = SECTION_HEADER_HASH.matcher(text);
        while (hashMatcher.find()) {
            boundaries.add(new SemanticBoundary(
                hashMatcher.start(),
                SemanticBoundary.BoundaryType.SECTION_HEADER
            ));
        }
        
        // Find section headers (colon-style)
        Matcher colonMatcher = SECTION_HEADER_COLON.matcher(text);
        while (colonMatcher.find()) {
            boundaries.add(new SemanticBoundary(
                colonMatcher.start(),
                SemanticBoundary.BoundaryType.SECTION_HEADER
            ));
        }
        
        // Find paragraph breaks
        Matcher paragraphMatcher = PARAGRAPH_BREAK.matcher(text);
        while (paragraphMatcher.find()) {
            boundaries.add(new SemanticBoundary(
                paragraphMatcher.start(),
                SemanticBoundary.BoundaryType.PARAGRAPH_BREAK
            ));
        }
        
        // Find sentence endings
        Matcher sentenceMatcher = SENTENCE_ENDING.matcher(text);
        while (sentenceMatcher.find()) {
            boundaries.add(new SemanticBoundary(
                sentenceMatcher.end(),
                SemanticBoundary.BoundaryType.SENTENCE_ENDING
            ));
        }
        
        // Sort boundaries by position
        boundaries.sort((a, b) -> Integer.compare(a.getPosition(), b.getPosition()));
        
        return boundaries;
    }
    
    @Override
    public DocumentChunk createChunk(String documentId, String text, int sequence, int totalChunks,
                                    long startOffset, long endOffset, int overlapTokens) {
        String chunkId = UUID.randomUUID().toString();
        int tokenCount = documentProcessor.calculateTokenCount(text);
        
        return new DocumentChunk(
            chunkId,
            documentId,
            sequence,
            totalChunks,
            text,
            tokenCount,
            startOffset,
            endOffset,
            overlapTokens
        );
    }
    
    /**
     * Performs semantic chunking on a document that exceeds the token threshold.
     */
    private List<DocumentChunk> performSemanticChunking(ExtractedDocument document) {
        String text = document.getExtractedText();
        List<SemanticBoundary> boundaries = identifyBoundaries(text);
        List<DocumentChunk> chunks = new ArrayList<>();
        
        int targetChunkChars = TARGET_CHUNK_SIZE * CHARACTERS_PER_TOKEN;
        int overlapChars = OVERLAP_TOKENS * CHARACTERS_PER_TOKEN;
        
        int currentStart = 0;
        int chunkSequence = 1;
        
        while (currentStart < text.length()) {
            // Calculate target end position
            int targetEnd = Math.min(currentStart + targetChunkChars, text.length());
            
            // Find best semantic boundary near target end
            int actualEnd = findBestBoundary(boundaries, currentStart, targetEnd, text.length());
            
            // Extract chunk text
            String chunkText = text.substring(currentStart, actualEnd);
            
            // Calculate overlap tokens (0 for first chunk)
            int overlapTokens = (chunkSequence == 1) ? 0 : OVERLAP_TOKENS;
            
            // Create chunk (we'll update totalChunks later)
            DocumentChunk chunk = createChunk(
                document.getDocumentId(),
                chunkText,
                chunkSequence,
                0, // Placeholder, will be updated
                currentStart,
                actualEnd,
                overlapTokens
            );
            
            chunks.add(chunk);
            
            // Move to next chunk start with overlap
            // Ensure we always make progress (don't go backwards)
            int nextStart = actualEnd - overlapChars;
            if (nextStart <= currentStart) {
                // If overlap would cause us to go backwards, just move forward
                nextStart = actualEnd;
            }
            currentStart = nextStart;
            chunkSequence++;
        }
        
        // Update total chunks count for all chunks
        int totalChunks = chunks.size();
        for (DocumentChunk chunk : chunks) {
            chunk.setTotalChunks(totalChunks);
        }
        
        return chunks;
    }
    
    /**
     * Finds the best semantic boundary near the target end position.
     * Prioritizes: section headers > paragraph breaks > sentence endings.
     */
    private int findBestBoundary(List<SemanticBoundary> boundaries, int start, int targetEnd, int textLength) {
        // If we're at the end of the text, use it
        if (targetEnd >= textLength) {
            return textLength;
        }
        
        // Define search window around target end (±10% of target chunk size)
        int windowSize = TARGET_CHUNK_SIZE * CHARACTERS_PER_TOKEN / 10;
        int windowStart = Math.max(start, targetEnd - windowSize);
        int windowEnd = Math.min(textLength, targetEnd + windowSize);
        
        // Find boundaries within the window
        SemanticBoundary bestBoundary = null;
        
        for (SemanticBoundary boundary : boundaries) {
            int pos = boundary.getPosition();
            
            // Skip boundaries before the window
            if (pos < windowStart) {
                continue;
            }
            
            // Stop if we've passed the window
            if (pos > windowEnd) {
                break;
            }
            
            // Update best boundary based on priority
            if (bestBoundary == null || isBetterBoundary(boundary, bestBoundary, targetEnd)) {
                bestBoundary = boundary;
            }
        }
        
        // If we found a good boundary, use it
        if (bestBoundary != null) {
            return bestBoundary.getPosition();
        }
        
        // Fallback: use target end if no boundary found
        return targetEnd;
    }
    
    /**
     * Determines if a boundary is better than the current best boundary.
     * Prioritizes by type first, then by proximity to target.
     */
    private boolean isBetterBoundary(SemanticBoundary candidate, SemanticBoundary current, int targetEnd) {
        // Higher priority type wins
        if (candidate.getType().ordinal() < current.getType().ordinal()) {
            return true;
        }
        
        // Same type: prefer closer to target
        if (candidate.getType() == current.getType()) {
            int candidateDistance = Math.abs(candidate.getPosition() - targetEnd);
            int currentDistance = Math.abs(current.getPosition() - targetEnd);
            return candidateDistance < currentDistance;
        }
        
        return false;
    }
}
