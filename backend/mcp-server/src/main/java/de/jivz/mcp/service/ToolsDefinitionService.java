package de.jivz.mcp.service;

import de.jivz.mcp.model.ToolDefinition;
import de.jivz.mcp.tools.ToolRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service für Tool-Definitionen.
 *
 * Single Responsibility Principle:
 * - Nur verantwortlich für Bereitstellung von Tool-Schemas
 *
 * Delegation Pattern:
 * - Delegiert an ToolRegistry
 * - Definitionen kommen direkt aus Tool-Klassen
 */
@Service
@RequiredArgsConstructor
public class ToolsDefinitionService {

    private final ToolRegistry toolRegistry;

    /**
     * Gibt Liste aller verfügbaren MCP Tools zurück.
     */
    public List<ToolDefinition> getToolDefinitions() {
        return toolRegistry.getDefinitions();
    }

    /**
     * Prüft, ob ein Tool existiert.
     */
    public boolean toolExists(String toolName) {
        return toolRegistry.exists(toolName);
    }

    /**
     * Gibt die Anzahl registrierter Tools zurück.
     */
    public int getToolCount() {
        return toolRegistry.size();
    }
}

