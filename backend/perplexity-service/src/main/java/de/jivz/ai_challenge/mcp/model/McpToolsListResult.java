package de.jivz.ai_challenge.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result from tools/list MCP call
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpToolsListResult {

    private List<McpTool> tools;
}

