package de.jivz.rag.mcp;

import lombok.*;
import java.util.List;
import java.util.Map;

/**
 * Модели для MCP протокола.
 */
public class McpModels {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ToolDefinition {
        private String name;
        private String description;
        private InputSchema inputSchema;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InputSchema {
        private String type;
        private Map<String, PropertyDefinition> properties;
        private List<String> required;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PropertyDefinition {
        private String type;
        private String description;
        @Builder.Default
        private Object defaultValue = null;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ToolCallRequest {
        private String name;
        private String toolName;  // Альтернативное имя (совместимость с perplexity-service)
        private Map<String, Object> arguments;

        /**
         * Возвращает эффективное имя инструмента (name или toolName).
         */
        public String getEffectiveName() {
            return name != null ? name : toolName;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ToolCallResponse {
        private boolean success;
        private Object result;
        private String error;
    }
}

