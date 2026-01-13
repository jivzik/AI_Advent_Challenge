package de.jivz.ai_challenge.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

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
    private Long timestamp;
}
