package de.jivz.ai_challenge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTOs für Sonar Tool-Workflow mit MCP-Integration.
 *
 * Ermöglicht dem LLM, MCP-Tools aufzurufen und strukturierte Antworten zu geben.
 */
public class SonarToolDto {

    /**
     * Strukturierte Antwort vom Sonar LLM.
     *
     * step = "tool" → Modell möchte MCP-Tool(s) aufrufen
     * step = "final" → Modell gibt finale Antwort für den User
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SonarToolResponse {

        /**
         * "tool" - Modell möchte MCP-Tool(s) aufrufen
         * "final" - Modell gibt finale Antwort
         */
        private String step;

        /**
         * Liste der Tool-Aufrufe (nur wenn step == "tool")
         */
        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;

        /**
         * Finale Antwort für den User (nur wenn step == "final")
         * Bei step == "tool" kann dies leer oder null sein
         */
        private String answer;
    }

    /**
     * Ein einzelner Tool-Aufruf.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCall {

        /**
         * Name des MCP-Tools (z.B. "google_tasks_list", "google_tasks_create")
         */
        private String name;

        /**
         * Argumente für das Tool
         */
        private Map<String, Object> arguments;
    }

    /**
     * MCP Call Request - für POST /mcp/call
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class McpCallRequest {
        private String name;
        private Map<String, Object> arguments;
    }

    /**
     * MCP Call Response - Antwort von POST /mcp/call
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class McpCallResponse {
        private boolean success;
        private Object result;
        private String error;
    }
}

