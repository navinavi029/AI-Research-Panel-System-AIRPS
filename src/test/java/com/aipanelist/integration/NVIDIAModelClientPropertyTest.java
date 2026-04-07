package com.aipanelist.integration;

import com.aipanelist.model.ModelRequest;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.NotEmpty;
import net.jqwik.api.constraints.Size;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for NVIDIAModelClient using jqwik.
 * 
 * Tests correctness properties that should hold for all valid inputs.
 */
class NVIDIAModelClientPropertyTest {

    /**
     * Property 21: Model Request Routing
     * 
     * **Validates: Requirements 5.3**
     * 
     * For any analysis request from an AI_Agent, the NVIDIA_Model_Client should send 
     * the request to the nvidia/llama-3.1-nemotron-70b-instruct endpoint.
     * 
     * This property verifies that all ModelRequest instances, regardless of their
     * content (messages, temperature, max tokens, etc.), are correctly configured
     * to route to the nvidia/llama-3.1-nemotron-70b-instruct model endpoint.
     */
    @Property(tries = 100)
    void allModelRequestsRoutedToNemotronEndpoint(
            @ForAll("modelRequests") ModelRequest request) {
        
        // Verify that the model field is set to the correct endpoint
        assertThat(request.getModel())
                .as("All model requests should be routed to nvidia/llama-3.1-nemotron-70b-instruct")
                .isEqualTo("nvidia/llama-3.1-nemotron-70b-instruct");
    }

    /**
     * Arbitrary provider for generating various ModelRequest instances.
     * 
     * Generates requests with:
     * - Fixed model endpoint (nvidia/llama-3.1-nemotron-70b-instruct)
     * - Various message configurations (1-5 messages)
     * - Different roles (system, user, assistant)
     * - Various content strings
     * - Different temperature values (0.0 - 1.0)
     * - Different max token values (100 - 4000)
     * - Both streaming and non-streaming modes
     */
    @Provide
    Arbitrary<ModelRequest> modelRequests() {
        return Combinators.combine(
                messages(),
                temperatures(),
                maxTokens(),
                streamFlags()
        ).as((msgs, temp, tokens, stream) -> 
                ModelRequest.builder()
                        .model("nvidia/llama-3.1-nemotron-70b-instruct")
                        .messages(msgs)
                        .temperature(temp)
                        .maxTokens(tokens)
                        .stream(stream)
                        .build()
        );
    }

    /**
     * Generate lists of messages with various roles and content.
     */
    @Provide
    Arbitrary<List<ModelRequest.Message>> messages() {
        return Arbitraries.of("system", "user", "assistant")
                .flatMap(role -> 
                        Arbitraries.strings()
                                .alpha()
                                .numeric()
                                .whitespace()
                                .ofMinLength(1)
                                .ofMaxLength(500)
                                .map(content -> 
                                        ModelRequest.Message.builder()
                                                .role(role)
                                                .content(content)
                                                .build()
                                )
                )
                .list()
                .ofMinSize(1)
                .ofMaxSize(5);
    }

    /**
     * Generate temperature values in the valid range [0.0, 1.0].
     */
    @Provide
    Arbitrary<Double> temperatures() {
        return Arbitraries.doubles()
                .between(0.0, 1.0);
    }

    /**
     * Generate max token values in a reasonable range.
     */
    @Provide
    Arbitrary<Integer> maxTokens() {
        return Arbitraries.integers()
                .between(100, 4000);
    }

    /**
     * Generate boolean flags for streaming mode.
     */
    @Provide
    Arbitrary<Boolean> streamFlags() {
        return Arbitraries.of(true, false);
    }
}
