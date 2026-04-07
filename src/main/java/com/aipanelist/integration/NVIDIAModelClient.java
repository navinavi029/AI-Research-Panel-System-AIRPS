package com.aipanelist.integration;

import com.aipanelist.model.APIException;
import com.aipanelist.model.ModelRequest;
import com.aipanelist.model.ModelResponse;

/**
 * Client interface for communicating with NVIDIA model APIs.
 * 
 * Provides methods for sending analysis requests to nvidia/llama-3.1-nemotron-70b-instruct
 * endpoint with built-in retry logic, rate limiting, and error handling.
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7
 */
public interface NVIDIAModelClient {

    /**
     * Send a request to the NVIDIA model API.
     * 
     * Implements retry logic with exponential backoff for 5xx errors,
     * handles 429 rate limit responses using Retry-After header,
     * and returns descriptive errors for 4xx responses and network failures.
     * 
     * @param request the model request containing messages and parameters
     * @return the model response with generated content
     * @throws APIException if the request fails after all retry attempts
     */
    ModelResponse sendRequest(ModelRequest request) throws APIException;

    /**
     * Check if the NVIDIA API is currently available.
     * 
     * @return true if the API is available, false otherwise
     */
    boolean isAvailable();

    /**
     * Configure the rate limiting for API requests.
     * 
     * @param requestsPerMinute the maximum number of requests per minute
     */
    void configureRateLimiting(int requestsPerMinute);
}
