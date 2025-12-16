package de.jivz.mcp.service;

import de.jivz.mcp.model.McpTool;

import java.util.List;
import java.util.Map;

/**
 * Interface für Tool-Provider.
 * Ermöglicht verschiedene Implementierungen (native, externe APIs, etc.)
 */
public interface ToolProvider {

    /**
     * Gibt den Namen des Tool-Providers zurück
     */
    String getProviderName();

    /**
     * Gibt eine Liste aller verfügbaren Tools dieses Providers zurück
     */
    List<McpTool> getTools();

    /**
     * Führt ein Tool aus
     *
     * @param toolName Name des Tools
     * @param arguments Argumente für das Tool
     * @return Ergebnis der Tool-Ausführung
     * @throws IllegalArgumentException wenn das Tool nicht existiert
     */
    Object executeTool(String toolName, Map<String, Object> arguments);

    /**
     * Prüft, ob dieser Provider ein bestimmtes Tool unterstützt
     */
    boolean supportsTool(String toolName);
}

