package de.jivz.ai_challenge.service.perplexity.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.jivz.ai_challenge.service.openrouter.model.OpenRouterRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for Perplexity API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerplexityRequest {

    private String model;
    @Builder.Default
    private List<Message> messages= new java.util.ArrayList<>();
    private Double temperature;
    @JsonProperty("max_tokens")
    private int maxTokens;

    /**
     * Message in the request.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Message {
        private String role;
        private String content;
    }
}

