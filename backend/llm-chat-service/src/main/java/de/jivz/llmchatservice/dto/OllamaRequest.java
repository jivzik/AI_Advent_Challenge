package de.jivz.llmchatservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Internal DTO for Ollama API requests.
 * Maps to Ollama's /api/generate or /api/chat endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OllamaRequest {

    /**
     * Model to use for generation
     */
    @JsonProperty("model")
    private String model;

    /**
     * User prompt/message
     */
    @JsonProperty("prompt")
    private String prompt;

    /**
     * System prompt (optional)
     */
    @JsonProperty("system")
    private String system;

    /**
     * Enable streaming responses
     */
    @JsonProperty("stream")
    private Boolean stream;

    /**
     * Model parameters (temperature, top_p, etc.)
     */
    @JsonProperty("options")
    private Map<String, Object> options;

}
