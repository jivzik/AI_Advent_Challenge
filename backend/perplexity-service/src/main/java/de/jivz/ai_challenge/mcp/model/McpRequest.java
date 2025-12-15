package de.jivz.ai_challenge.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP JSON-RPC Request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpRequest {

    private String jsonrpc = "2.0";

    private Integer id;

    private String method;

    private Map<String, Object> params;
}

