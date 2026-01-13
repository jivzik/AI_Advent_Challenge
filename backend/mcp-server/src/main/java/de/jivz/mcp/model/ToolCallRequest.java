package de.jivz.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request für Tool-Aufruf.
 * Enthält Tool-Name und Argumente.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolCallRequest {
    private String name;
    private String toolName;
    private Map<String, Object> arguments;

    /**
     * Gibt den effektiven Tool-Namen zurück (name oder toolName).
     */
    public String getEffectiveName() {
        return name != null ? name : toolName;
    }
}

