package de.jivz.ai_challenge.mcp.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTOs für MCP Service Kommunikation
 */
public class McpDto {

    /**
     * MCP Tool DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class McpTool {
        private String name;
        private String description;
        private Map<String, Object> inputSchema;
    }

    /**
     * Tool Execution Request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolExecutionRequest {
        private String toolName;
        private Map<String, Object> arguments;
    }

    /**
     * Tool Execution Response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolExecutionResponse {
        private boolean success;
        private Object result;
        private String toolName;
        private String error;
    }

    /**
     * Provider Info DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderInfo {
        private List<String> providers;
        private Map<String, Object> statistics;
    }

    /**
     * MCP Status DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class McpStatus {
        private String status;
        private String type;
        private String version;

        @JsonProperty("total_providers")
        private Integer totalProviders;

        @JsonProperty("total_tools")
        private Integer totalTools;

        @JsonProperty("tools_by_provider")
        private Map<String, Integer> toolsByProvider;
    }

    /**
     * MCP Tools Response DTO
     * Для десериализации ответа с списком инструментов
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class McpToolsResponse {
        private boolean success;
        private List<McpTool> tools;
        private Integer count;
        private String timestamp;
    }

    /**
     * Perplexity Tool Result DTO
     * Результат выполнения инструмента Perplexity с полной информацией
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerplexityToolResult {
        private boolean success;
        private String tool;
        private Result result;
        private String timestamp;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Result {
            private boolean success;
            private String answer;
            private String model;
            private Usage usage;
            private List<String> citations;


            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class Usage {
                @JsonProperty("prompt_tokens")
                private Integer promptTokens;

                @JsonProperty("completion_tokens")
                private Integer completionTokens;

                @JsonProperty("total_tokens")
                private Integer totalTokens;

                @JsonProperty("search_context_size")
                private String searchContextSize;

                private Cost cost;

                @Data
                @Builder
                @NoArgsConstructor
                @AllArgsConstructor
                public static class Cost {
                    @JsonProperty("input_tokens_cost")
                    private Double inputTokensCost;

                    @JsonProperty("output_tokens_cost")
                    private Double outputTokensCost;

                    @JsonProperty("request_cost")
                    private Double requestCost;

                    @JsonProperty("total_cost")
                    private Double totalCost;
                }
            }
        }
    }
}

