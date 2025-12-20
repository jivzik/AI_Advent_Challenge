package de.jivz.ai_challenge.mcp;

import de.jivz.ai_challenge.mcp.model.MCPToolResult;
import de.jivz.ai_challenge.mcp.model.ToolDefinition;

import java.util.List;
import java.util.Map;

public interface MCPService {
    /**
     * Возвращает имя сервера (например, "filesystem")
     */
    String getServerName();

    /**
     * Выполняет инструмент на удаленном MCP сервере
     */
    MCPToolResult execute(String toolName, Map<String, Object> params);

    /**
     * Возвращает список доступных инструментов с описаниями
     */
    List<ToolDefinition> getToolDefinitions();

}