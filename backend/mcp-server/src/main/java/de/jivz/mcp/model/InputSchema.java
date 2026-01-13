package de.jivz.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Input-Schema für Tool-Parameter.
 * Definiert die erwarteten Parameter eines Tools.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InputSchema {
    private String type;
    private Map<String, PropertyDefinition> properties;
    private List<String> required;
}

