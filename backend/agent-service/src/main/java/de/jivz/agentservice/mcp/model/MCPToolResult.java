package de.jivz.agentservice.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Ergebnis einer MCP Tool-Ausführung.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MCPToolResult {
    private boolean success;
    private Object result;
    private String error;
    private String toolName;
    private Map<String, Object> metadata;
    private long timestamp;
}

