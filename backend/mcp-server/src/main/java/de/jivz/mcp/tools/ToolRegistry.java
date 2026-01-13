package de.jivz.mcp.tools;

import de.jivz.mcp.model.ToolDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry für MCP Tools.
 *
 * Spring injiziert automatisch alle @Component implements Tool.
 * Hinzufügen eines neuen Tools erfordert keine Änderungen hier.
 *
 * Dependency Inversion Principle:
 * - Abhängig von Tool-Interface, nicht von konkreten Implementierungen
 */
@Component
@Slf4j
public class ToolRegistry {

    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    public ToolRegistry(List<Tool> toolList) {
        toolList.forEach(tool -> {
            tools.put(tool.getName(), tool);
            log.debug("Tool registriert: {}", tool.getName());
        });
        log.info("{} MCP Tools registriert: {}", tools.size(), tools.keySet());
    }

    /**
     * Tool nach Name finden.
     */
    public Optional<Tool> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    /**
     * Alle Tool-Definitionen abrufen.
     */
    public List<ToolDefinition> getDefinitions() {
        return tools.values().stream()
                .map(Tool::getDefinition)
                .toList();
    }

    /**
     * Prüfen, ob Tool existiert.
     */
    public boolean exists(String name) {
        return tools.containsKey(name);
    }

    /**
     * Anzahl registrierter Tools.
     */
    public int size() {
        return tools.size();
    }

    /**
     * Alle Tool-Namen abrufen.
     */
    public List<String> getToolNames() {
        return List.copyOf(tools.keySet());
    }
}

