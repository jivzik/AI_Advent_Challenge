package de.jivz.ai_challenge.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Model representing an MCP Tool
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpTool {

    private String name;

    private String description;

    @JsonProperty("inputSchema")
    private Map<String, Object> inputSchema;
}


