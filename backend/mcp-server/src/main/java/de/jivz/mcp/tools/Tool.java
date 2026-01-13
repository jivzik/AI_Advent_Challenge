package de.jivz.mcp.tools;

import de.jivz.mcp.model.ToolDefinition;

import java.util.Map;

/**
 * MCP Tool Interface - Strategy Pattern.
 *
 * Vereint Definition (Schema) und Ausführung in einem einzigen Interface.
 * Jedes Tool kennt seine Schema und weiß, wie es sich selbst ausführt.
 *
 * Open/Closed Principle:
 * - Neues Tool = neue Klasse @Component implements Tool
 * - Keine Änderungen in ToolExecutor oder ToolRegistry erforderlich
 *
 * Single Responsibility Principle:
 * - Jedes Tool ist für genau eine Funktionalität verantwortlich
 */
public interface Tool {

    /**
     * Eindeutiger Name des Tools.
     */
    String getName();

    /**
     * Tool-Definition für MCP-Protokoll (Schema).
     */
    ToolDefinition getDefinition();

    /**
     * Tool ausführen.
     *
     * @param arguments Aufrufargumente
     * @return Ausführungsergebnis
     * @throws ToolExecutionException bei Fehler
     */
    Object execute(Map<String, Object> arguments);
}

