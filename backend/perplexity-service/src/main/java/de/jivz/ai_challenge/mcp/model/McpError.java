package de.jivz.ai_challenge.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP Error object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpError {

    private int code;

    private String message;

    private Object data;
}

