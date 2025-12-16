package de.jivz.ai_challenge.service.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}

