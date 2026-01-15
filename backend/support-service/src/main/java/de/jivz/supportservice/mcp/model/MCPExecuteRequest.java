package de.jivz.supportservice.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request für MCP Tool-Ausführung.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MCPExecuteRequest {
    private String toolName;
    private Map<String, Object> arguments;
}

