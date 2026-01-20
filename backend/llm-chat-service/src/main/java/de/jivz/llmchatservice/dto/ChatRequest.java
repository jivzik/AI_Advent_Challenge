package de.jivz.llmchatservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for chat API endpoint.
 * Accepts user message and optional configuration overrides.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /**
     * User's message to send to the LLM
     */
    @JsonProperty("message")
    private String message;

    /**
     * Optional: Override default model for this request
     * Example: "mistral", "codellama", "neural-chat"
     */
    @JsonProperty("model")
    private String model;

    /**
     * Optional: Override default temperature (0.0 - 2.0)
     * Controls randomness in response generation
     */
    @JsonProperty("temperature")
    private Double temperature;

    /**
     * Optional: Override default max tokens
     * Controls maximum length of generated response
     */
    @JsonProperty("maxTokens")
    private Integer maxTokens;

    /**
     * Optional: System prompt to set LLM behavior
     * Example: "You are a helpful coding assistant"
     */
    @JsonProperty("systemPrompt")
    private String systemPrompt;

    /**
     * Optional: Enable streaming response
     * Default: false (returns complete response)
     */
    @JsonProperty("stream")
    private Boolean stream;

}
