package com.aipanelist.integration;

import com.aipanelist.config.NVIDIAConfiguration;
import com.aipanelist.model.ModelRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for NVIDIAModelClient implementation.
 * 
 * Tests the NVIDIA model client implementation including:
 * - Model request creation
 * - Request structure validation
 * - Circuit breaker configuration and behavior
 */
class NVIDIAModelClientTest {

    private NVIDIAConfiguration config;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Configure circuit breaker with test settings matching requirements
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)  // 50% failure threshold
                .waitDurationInOpenState(Duration.ofMinutes(1))  // 1-minute open duration
                .slidingWindowSize(10)  // 10-request sliding window
                .minimumNumberOfCalls(5)
                .build();

        circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);
        objectMapper = new ObjectMapper();

        // Create real configuration object
        config = new NVIDIAConfiguration();
        ReflectionTestUtils.setField(config, "apiEndpoint", "https://test.api.nvidia.com/v1");
        ReflectionTestUtils.setField(config, "apiKey", "test-key");
        ReflectionTestUtils.setField(config, "rateLimit", 60);
        ReflectionTestUtils.setField(config, "maxConnections", 10);
    }

    @Test
    void testModelRequestCreation() {
        // Test creating a valid model request
        ModelRequest request = ModelRequest.builder()
                .model("nvidia/llama-3.1-nemotron-70b-instruct")
                .messages(List.of(
                        ModelRequest.Message.builder()
                                .role("user")
                                .content("Test message")
                                .build()
                ))
                .temperature(0.7)
                .maxTokens(1000)
                .stream(false)
                .build();

        assertThat(request).isNotNull();
        assertThat(request.getModel()).isEqualTo("nvidia/llama-3.1-nemotron-70b-instruct");
        assertThat(request.getMessages()).hasSize(1);
        assertThat(request.getMessages().get(0).getContent()).isEqualTo("Test message");
        assertThat(request.getMessages().get(0).getRole()).isEqualTo("user");
        assertThat(request.getTemperature()).isEqualTo(0.7);
        assertThat(request.getMaxTokens()).isEqualTo(1000);
        assertThat(request.getStream()).isFalse();
    }

    @Test
    void testModelRequestWithMultipleMessages() {
        // Test creating a request with multiple messages
        ModelRequest request = ModelRequest.builder()
                .model("nvidia/llama-3.1-nemotron-70b-instruct")
                .messages(List.of(
                        ModelRequest.Message.builder()
                                .role("system")
                                .content("You are a helpful assistant")
                                .build(),
                        ModelRequest.Message.builder()
                                .role("user")
                                .content("Hello")
                                .build()
                ))
                .build();

        assertThat(request).isNotNull();
        assertThat(request.getMessages()).hasSize(2);
        assertThat(request.getMessages().get(0).getRole()).isEqualTo("system");
        assertThat(request.getMessages().get(1).getRole()).isEqualTo("user");
    }

    @Test
    void testCircuitBreakerConfiguration() {
        // Verify circuit breaker is configured with correct settings
        NVIDIAModelClientImpl client = new NVIDIAModelClientImpl(
                config, circuitBreakerRegistry, objectMapper);

        // Verify client is available (circuit is closed initially)
        assertThat(client.isAvailable()).isTrue();

        // Verify circuit breaker exists with correct name
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("nvidia-api");
        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Verify circuit breaker configuration
        CircuitBreakerConfig cbConfig = circuitBreaker.getCircuitBreakerConfig();
        assertThat(cbConfig.getFailureRateThreshold()).isEqualTo(50.0f);
        assertThat(cbConfig.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(Duration.ofMinutes(1).toMillis());
        assertThat(cbConfig.getSlidingWindowSize()).isEqualTo(10);
    }

    @Test
    void testCircuitBreakerReturnsDescriptiveErrorWhenOpen() {
        // Create client with circuit breaker
        NVIDIAModelClientImpl client = new NVIDIAModelClientImpl(
                config, circuitBreakerRegistry, objectMapper);

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("nvidia-api");

        // Manually transition circuit breaker to OPEN state
        circuitBreaker.transitionToOpenState();

        // Verify circuit is open
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(client.isAvailable()).isFalse();

        // Create a test request
        ModelRequest request = ModelRequest.builder()
                .model("nvidia/llama-3.1-nemotron-70b-instruct")
                .messages(List.of(
                        ModelRequest.Message.builder()
                                .role("user")
                                .content("Test message")
                                .build()
                ))
                .build();

        // Attempt to send request when circuit is open should throw descriptive error
        assertThatThrownBy(() -> client.sendRequest(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("CircuitBreaker 'nvidia-api' is OPEN");
    }

    @Test
    void testRateLimitingConfiguration() {
        // Create client and verify rate limiting is configured
        NVIDIAModelClientImpl client = new NVIDIAModelClientImpl(
                config, circuitBreakerRegistry, objectMapper);

        // Verify rate limiting can be reconfigured
        client.configureRateLimiting(120);

        // Note: We can't easily test the actual rate limiting behavior in a unit test
        // without making real API calls, but we verify the configuration is accepted
        assertThat(client).isNotNull();
    }
}
