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

        @com.fasterxml.jackson.annotation.JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;

        @com.fasterxml.jackson.annotation.JsonProperty("tool_call_id")
        private String toolCallId;
    }

    /**
     * Tool definition (optional).
     * OpenAI/OpenRouter format: { "type": "function", "function": { "name": ..., "description": ..., "parameters": ... } }
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Tool {
        @Builder.Default
        private String type = "function";
        private FunctionDefinition function;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class FunctionDefinition {
            private String name;
            private String description;
            private Object parameters;
        }
    }

    /**
     * Tool call from LLM response.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ToolCall {
        private String id;
        private String type;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class Function {
            private String name;
            private String arguments;
        }

        private Function function;
    }
}