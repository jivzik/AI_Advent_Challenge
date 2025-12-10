package de.jivz.ai_challenge.service.openrouter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for OpenRouter API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenRouterRequest {

    private String model;
    @Builder.Default
    private List<ChatMessage> messages = new java.util.ArrayList<>();
    private Double temperature;
    private Integer maxTokens;
    private Double topP;
    private List<Tool> tools;

    /**
     * Chat message in the request.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatMessage {
        private String role;
        private String content;
    }

    /**
     * Tool definition (optional).
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Tool {
        private String type;
        private String name;
        private String description;
        private Object parameters;
    }
}