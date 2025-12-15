package de.jivz.ai_challenge.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request to execute an MCP tool
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolExecutionRequest {

    private String toolName;

    private Map<String, Object> arguments;
}

