package com.aipanelist.integration;

import com.aipanelist.config.NVIDIAConfiguration;
import com.aipanelist.model.APIException;
import com.aipanelist.model.ModelRequest;
import com.aipanelist.model.ModelResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of NVIDIAModelClient for communicating with NVIDIA model APIs.
 * 
 * Features:
 * - Connection pooling with max 10 concurrent connections
 * - Rate limiting with configurable requests per minute (default 60)
 * - Retry logic: 3 attempts with exponential backoff (1s, 2s, 4s) for 5xx errors
 * - Handles 429 rate limit responses using Retry-After header
 * - Descriptive errors for 4xx responses and network failures
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7
 */
@Component
public class NVIDIAModelClientImpl implements NVIDIAModelClient {

    private static final Logger logger = LoggerFactory.getLogger(NVIDIAModelClientImpl.class);
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;
    private static final int TIMEOUT_SECONDS = 300; // 5 minutes

    private final NVIDIAConfiguration config;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final CircuitBreaker circuitBreaker;
    private final Semaphore rateLimiter;
    private volatile int requestsPerMinute;

    public NVIDIAModelClientImpl(NVIDIAConfiguration config, 
                                  CircuitBreakerRegistry circuitBreakerRegistry,
                                  ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("nvidia-api");
        this.requestsPerMinute = config.getRateLimit();
        this.rateLimiter = new Semaphore(requestsPerMinute);
        
        // Configure connection pooling
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(config.getMaxConnections());
        connectionManager.setDefaultMaxPerRoute(config.getMaxConnections());
        
        // Configure request timeouts
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(30))
                .setResponseTimeout(Timeout.ofSeconds(TIMEOUT_SECONDS))
                .build();
        
        this.httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
        
        logger.info("NVIDIAModelClient initialized with endpoint: {}, max connections: {}, rate limit: {}/min",
                config.getApiEndpoint(), config.getMaxConnections(), requestsPerMinute);
    }

    @Override
    public ModelResponse sendRequest(ModelRequest request) throws APIException {
        return circuitBreaker.executeSupplier(() -> {
            try {
                return sendRequestWithRetry(request);
            } catch (APIException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private ModelResponse sendRequestWithRetry(ModelRequest request) throws APIException {
        int attempt = 0;
        APIException lastException = new APIException("No attempts made");
        
        while (attempt < MAX_RETRIES) {
            try {
                // Apply rate limiting
                applyRateLimiting();
                
                // Send the request
                ModelResponse response = executeRequest(request);
                
                // Release rate limiter permit after successful request
                schedulePermitRelease();
                
                return response;
                
            } catch (APIException e) {
                lastException = e;
                attempt++;
                
                // Handle 429 rate limit
                if (e.getStatusCode() == 429) {
                    handleRateLimitError(e, attempt);
                    continue;
                }
                
                // Retry on 5xx errors
                if (e.isRetryable() && attempt < MAX_RETRIES) {
                    long backoffMs = INITIAL_BACKOFF_MS * (1L << (attempt - 1));
                    logger.warn("Attempt {} failed with status {}: {}. Retrying in {}ms",
                            attempt, e.getStatusCode(), e.getMessage(), backoffMs);
                    sleep(backoffMs);
                    continue;
                }
                
                // Don't retry 4xx errors (except 429)
                if (e.getStatusCode() >= 400 && e.getStatusCode() < 500) {
                    throw e;
                }
            }
        }
        
        throw new APIException(
                "Failed after " + MAX_RETRIES + " attempts: " + lastException.getMessage(),
                lastException.getStatusCode(),
                lastException.getErrorCode(),
                lastException
        );
    }

    private ModelResponse executeRequest(ModelRequest request) throws APIException {
        HttpPost httpPost = new HttpPost(config.getApiEndpoint() + "/chat/completions");
        
        try {
            // Set headers
            httpPost.setHeader("Authorization", "Bearer " + config.getApiKey());
            httpPost.setHeader("Content-Type", "application/json");
            
            // Set request body
            String requestBody = objectMapper.writeValueAsString(request);
            httpPost.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));
            
            logger.debug("Sending request to NVIDIA API: {}", config.getApiEndpoint());
            
            // Execute request
            try (ClassicHttpResponse response = httpClient.executeOpen(null, httpPost, null)) {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                
                logger.debug("Received response with status code: {}", statusCode);
                
                // Handle successful response
                if (statusCode == 200) {
                    ModelResponse modelResponse = objectMapper.readValue(responseBody, ModelResponse.class);
                    logger.debug("Successfully parsed model response");
                    return modelResponse;
                }
                
                // Handle error responses
                String errorMessage = parseErrorMessage(responseBody, statusCode);
                
                if (statusCode == 429) {
                    String retryAfter = response.getFirstHeader("Retry-After") != null ?
                            response.getFirstHeader("Retry-After").getValue() : "60";
                    throw new APIException(
                            "Rate limit exceeded. Retry after " + retryAfter + " seconds",
                            statusCode,
                            "RATE_LIMIT_EXCEEDED"
                    );
                }
                
                if (statusCode >= 400 && statusCode < 500) {
                    throw new APIException(
                            "Client error: " + errorMessage,
                            statusCode,
                            "CLIENT_ERROR"
                    );
                }
                
                if (statusCode >= 500) {
                    throw new APIException(
                            "Server error: " + errorMessage,
                            statusCode,
                            "SERVER_ERROR"
                    );
                }
                
                throw new APIException(
                        "Unexpected status code: " + statusCode + ", message: " + errorMessage,
                        statusCode,
                        "UNEXPECTED_ERROR"
                );
            }
            
        } catch (IOException e) {
            logger.error("Network error while calling NVIDIA API", e);
            throw new APIException("Network error: " + e.getMessage(), e);
        } catch (APIException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while calling NVIDIA API", e);
            throw new APIException("Unexpected error: " + e.getMessage(), e);
        }
    }

    private String parseErrorMessage(String responseBody, int statusCode) {
        try {
            // Try to parse error from response body
            var errorNode = objectMapper.readTree(responseBody);
            if (errorNode.has("error")) {
                var error = errorNode.get("error");
                if (error.has("message")) {
                    return error.get("message").asText();
                }
            }
            return responseBody;
        } catch (Exception e) {
            return "Status code: " + statusCode;
        }
    }

    private void applyRateLimiting() throws APIException {
        try {
            // Try to acquire permit
            if (!rateLimiter.tryAcquire(30, TimeUnit.SECONDS)) {
                throw new APIException(
                        "Rate limit exceeded - could not acquire permit within 30 seconds",
                        429,
                        "RATE_LIMIT_EXCEEDED"
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new APIException("Rate limiting interrupted", e);
        }
    }

    private void schedulePermitRelease() {
        // Release permit after the rate limit window
        long delayMs = 60000 / requestsPerMinute;
        new Thread(() -> {
            sleep(delayMs);
            rateLimiter.release();
        }).start();
    }

    private void handleRateLimitError(APIException e, int attempt) throws APIException {
        // Extract retry-after from error message if available
        String message = e.getMessage();
        int retryAfterSeconds = 60; // default
        
        if (message.contains("Retry after")) {
            try {
                String[] parts = message.split("Retry after ");
                if (parts.length > 1) {
                    retryAfterSeconds = Integer.parseInt(parts[1].split(" ")[0]);
                }
            } catch (Exception ex) {
                logger.warn("Could not parse Retry-After value, using default 60 seconds");
            }
        }
        
        if (attempt < MAX_RETRIES) {
            logger.warn("Rate limit hit. Waiting {} seconds before retry", retryAfterSeconds);
            sleep(retryAfterSeconds * 1000L);
        } else {
            throw e;
        }
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Sleep interrupted", e);
        }
    }

    @Override
    public boolean isAvailable() {
        return circuitBreaker.getState() == CircuitBreaker.State.CLOSED;
    }

    @Override
    public void configureRateLimiting(int requestsPerMinute) {
        logger.info("Updating rate limit from {} to {} requests per minute",
                this.requestsPerMinute, requestsPerMinute);
        this.requestsPerMinute = requestsPerMinute;
        
        // Drain existing permits and reset
        rateLimiter.drainPermits();
        rateLimiter.release(requestsPerMinute);
    }
}
