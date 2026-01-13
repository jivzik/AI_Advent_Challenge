package de.jivz.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Property-Definition für Tool-Parameter.
 * Beschreibt Typ und Eigenschaften eines einzelnen Parameters.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyDefinition {
    private String type;
    private String description;
    @Builder.Default
    private Object defaultValue = null;
}

