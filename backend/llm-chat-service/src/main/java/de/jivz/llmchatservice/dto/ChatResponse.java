package de.jivz.llmchatservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for chat API endpoint.
 * Contains LLM response and metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * Generated response from the LLM
     */
    @JsonProperty("response")
    private String response;

    /**
     * Model used for generation
     */
    @JsonProperty("model")
    private String model;

    /**
     * Timestamp when response was generated
     */
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    /**
     * Time taken to generate response in milliseconds
     */
    @JsonProperty("processingTimeMs")
    private Long processingTimeMs;

    /**
     * Number of tokens in the generated response
     */
    @JsonProperty("tokensGenerated")
    private Integer tokensGenerated;

    /**
     * Indicates if response generation completed successfully
     */
    @JsonProperty("done")
    private Boolean done;

    /**
     * Error message if request failed
     */
    @JsonProperty("error")
    private String error;

    /**
     * Additional metadata from Ollama response
     */
    @JsonProperty("metadata")
    private Object metadata;

}
