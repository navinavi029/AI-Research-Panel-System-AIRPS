package com.aipanelist.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for NVIDIA API integration.
 * 
 * Holds configuration values for connecting to NVIDIA model APIs,
 * including endpoint URL, API key, model name, and rate limiting settings.
 */
@Configuration
public class NVIDIAConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(NVIDIAConfiguration.class);

    @Value("${nvidia.api.endpoint}")
    private String apiEndpoint;

    @Value("${nvidia.api.key}")
    private String apiKey;

    @Value("${nvidia.api.model}")
    private String modelName;

    @Value("${nvidia.api.rate-limit}")
    private int rateLimit;

    @Value("${nvidia.api.max-connections}")
    private int maxConnections;

    @PostConstruct
    public void logConfiguration() {
        logger.info("NVIDIA Configuration loaded - Model: {}, Endpoint: {}, Rate Limit: {}/min", 
                modelName, apiEndpoint, rateLimit);
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModelName() {
        return modelName;
    }

    public int getRateLimit() {
        return rateLimit;
    }

    public int getMaxConnections() {
        return maxConnections;
    }
}
