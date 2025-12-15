package de.jivz.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents the result of tools/list request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolsListResult {

    @JsonProperty("tools")
    private List<McpTool> tools;
}

