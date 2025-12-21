package de.jivz.ai_challenge.openrouterservice.mcp;

import de.jivz.ai_challenge.openrouterservice.mcp.model.MCPToolResult;
import de.jivz.ai_challenge.openrouterservice.mcp.model.ToolDefinition;

import java.util.List;
import java.util.Map;

/**
 * Interface für MCP Service Implementierungen.
 * Jeder MCP Server (google, filesystem, etc.) implementiert dieses Interface.
 */
public interface MCPService {

    /**
     * Gibt den Namen des MCP Servers zurück (z.B. "google", "filesystem").
     */
    String getServerName();

    /**
     * Führt ein Tool auf dem MCP Server aus.
     *
     * @param toolName Name des Tools (ohne Server-Prefix)
     * @param params Parameter für das Tool
     * @return Ergebnis der Ausführung
     */
    MCPToolResult execute(String toolName, Map<String, Object> params);

    /**
     * Gibt die Liste der verfügbaren Tools mit Beschreibungen zurück.
     */
    List<ToolDefinition> getToolDefinitions();
}

