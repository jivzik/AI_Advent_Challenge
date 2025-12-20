package de.ai.advent.mcp.docker.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP Tool Definition - unified with McpTool from mcp-service
 * Flat structure: name, description, inputSchema
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerplexityFunctionDefinition {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("inputSchema")
    private Map<String, Object> inputSchema;
}

