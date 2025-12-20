package de.ai.advent.mcp.docker.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Стандартный ответ для REST API
 *
 * Унифицированная схема для всех REST endpoints и обработки ошибок
 * Совместима с ToolExecutionResponse из mcp-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolCallResponse {

    private String toolName;
    private boolean success;
    private Object result;
    private String error;
    private long timestamp;

    public ToolCallResponse(Object result) {
        this.success = true;
        this.result = result;
        this.timestamp = System.currentTimeMillis();
    }

    public ToolCallResponse(boolean success, String error) {
        this.success = success;
        this.error = error;
        this.timestamp = System.currentTimeMillis();
    }

    public ToolCallResponse(boolean success, Object result, String toolName) {
        this.toolName = toolName;
        this.success = success;
        this.result = result;
        this.timestamp = System.currentTimeMillis();
    }

    public ToolCallResponse(boolean success, String error, String toolName) {
        this.toolName = toolName;
        this.success = success;
        this.error = error;
        this.timestamp = System.currentTimeMillis();
    }
}

