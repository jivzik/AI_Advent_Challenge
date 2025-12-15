package de.jivz.ai_challenge.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP JSON-RPC Response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpResponse<T> {

    private String jsonrpc;

    private Integer id;

    private T result;

    private McpError error;
}

