package de.jivz.rag.mcp.tools;

import de.jivz.rag.mcp.McpModels.ToolDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реестр MCP инструментов.
 *
 * Spring автоматически инжектирует все @Component implements Tool.
 * Добавление нового инструмента не требует изменений здесь.
 */
@Component
@Slf4j
public class ToolRegistry {

    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    public ToolRegistry(List<Tool> toolList) {
        toolList.forEach(tool -> {
            tools.put(tool.getName(), tool);
            log.debug("Registered tool: {}", tool.getName());
        });
        log.info("Registered {} MCP tools: {}", tools.size(), tools.keySet());
    }

    /**
     * Найти инструмент по имени.
     */
    public Optional<Tool> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    /**
     * Получить все определения инструментов.
     */
    public List<ToolDefinition> getDefinitions() {
        return tools.values().stream()
                .map(Tool::getDefinition)
                .toList();
    }

    /**
     * Проверить существование инструмента.
     */
    public boolean exists(String name) {
        return tools.containsKey(name);
    }
}