package de.jivz.mcp.model;

import lombok.NoArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;

/**
 * Beschreibt Schema und Metadata eines Tools.
 * Tool-Definition für MCP-Protokoll.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ToolDefinition {

    private String name;
    private InputSchema inputSchema;
    private String description;
}
