package de.jivz.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response für Tool-Aufruf.
 * Enthält Ergebnis oder Fehler.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolCallResponse {
    private boolean success;
    private Object result;
    private String error;
}

