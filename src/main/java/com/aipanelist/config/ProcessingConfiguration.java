package com.aipanelist.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for document processing.
 * 
 * Holds configuration values for document size limits, chunking parameters,
 * and processing timeouts.
 */
@Configuration
@Getter
public class ProcessingConfiguration {

    @Value("${app.processing.max-document-pages}")
    private int maxDocumentPages;

    @Value("${app.processing.max-document-tokens}")
    private int maxDocumentTokens;

    @Value("${app.processing.chunk-size-tokens}")
    private int chunkSizeTokens;

    @Value("${app.processing.chunk-overlap-tokens}")
    private int chunkOverlapTokens;

    @Value("${app.processing.chunk-timeout-minutes}")
    private int chunkTimeoutMinutes;
}
