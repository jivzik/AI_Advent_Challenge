package de.jivz.mcp.service;

import de.jivz.mcp.model.ToolCallResponse;
import de.jivz.mcp.model.ToolDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * MCP Server Service - Haupt-Orchestrator.
 *
 * Facade Pattern:
 * - Vereinfacht Zugriff auf Tool-System
 * - Delegiert an ToolExecutorService und ToolsDefinitionService
 *
 * Single Responsibility Principle:
 * - Koordination und High-Level API
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class McpServerService {

    private final ToolExecutorService toolExecutorService;
    private final ToolsDefinitionService toolsDefinitionService;

    /**
     * Liste aller verfügbaren Tools abrufen.
     */
    public List<ToolDefinition> listTools() {
        List<ToolDefinition> tools = toolsDefinitionService.getToolDefinitions();
        log.debug("{} verfügbare Tools aufgelistet", tools.size());
        return tools;
    }

    /**
     * Tool ausführen.
     */
    public ToolCallResponse executeTool(String toolName, Map<String, Object> arguments) {
        log.info("Tool ausführen: {} mit Argumenten: {}", toolName, arguments);
        return toolExecutorService.execute(toolName, arguments != null ? arguments : Map.of());
    }

    /**
     * Tool-Anzahl abrufen.
     */
    public int getToolCount() {
        return toolsDefinitionService.getToolCount();
    }

    /**
     * Prüfen, ob Tool existiert.
     */
    public boolean hasTools() {
        return toolsDefinitionService.getToolCount() > 0;
    }

    /**
     * Statistiken abrufen.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_tools", getToolCount());
        stats.put("tools", toolsDefinitionService.getToolDefinitions().stream()
                .map(ToolDefinition::getName)
                .toList());
        return stats;
    }
}

