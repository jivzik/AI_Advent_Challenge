package de.jivz.llmchatservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Internal DTO for Ollama API responses.
 * Maps to Ollama's /api/generate or /api/chat response format.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaResponse {

    /**
     * Model used for generation
     */
    @JsonProperty("model")
    private String model;

    /**
     * Generated response text
     */
    @JsonProperty("response")
    private String response;

    /**
     * Indicates if generation is complete
     */
    @JsonProperty("done")
    private Boolean done;

    /**
     * Context array for conversation continuity (optional)
     */
    @JsonProperty("context")
    private List<Integer> context;

    /**
     * Total duration in nanoseconds
     */
    @JsonProperty("total_duration")
    private Long totalDuration;

    /**
     * Load duration in nanoseconds
     */
    @JsonProperty("load_duration")
    private Long loadDuration;

    /**
     * Prompt evaluation count
     */
    @JsonProperty("prompt_eval_count")
    private Integer promptEvalCount;

    /**
     * Prompt evaluation duration in nanoseconds
     */
    @JsonProperty("prompt_eval_duration")
    private Long promptEvalDuration;

    /**
     * Evaluation count (tokens generated)
     */
    @JsonProperty("eval_count")
    private Integer evalCount;

    /**
     * Evaluation duration in nanoseconds
     */
    @JsonProperty("eval_duration")
    private Long evalDuration;

}
