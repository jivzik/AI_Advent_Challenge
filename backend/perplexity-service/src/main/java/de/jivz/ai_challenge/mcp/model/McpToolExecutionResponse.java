package de.jivz.ai_challenge.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response from MCP tool execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolExecutionResponse {

    private boolean success;

    private String toolName;

    private Object result;

    private String error;
}

