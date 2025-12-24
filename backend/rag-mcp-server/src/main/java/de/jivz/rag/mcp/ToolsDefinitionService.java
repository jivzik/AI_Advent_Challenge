package de.jivz.rag.mcp;

import de.jivz.rag.mcp.McpModels.ToolDefinition;
import de.jivz.rag.mcp.tools.ToolRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис определений MCP инструментов.
 *
 * Делегирует в ToolRegistry.
 * Определения берутся напрямую из классов Tool.
 */
@Service
@RequiredArgsConstructor
public class ToolsDefinitionService {

    private final ToolRegistry toolRegistry;

    /**
     * Возвращает список всех доступных MCP инструментов.
     */
    public List<ToolDefinition> getToolDefinitions() {
        return toolRegistry.getDefinitions();
    }
}