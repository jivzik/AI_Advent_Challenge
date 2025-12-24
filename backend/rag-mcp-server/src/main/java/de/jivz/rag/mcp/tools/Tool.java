package de.jivz.rag.mcp.tools;

import de.jivz.rag.mcp.McpModels.ToolDefinition;

import java.util.Map;

/**
 * Интерфейс MCP инструмента.
 *
 * Объединяет определение (schema) и выполнение в одном месте.
 * Каждый Tool сам знает свою схему и как себя выполнять.
 *
 * Open/Closed Principle:
 * - Новый инструмент = новый класс @Component implements Tool
 * - Никаких изменений в ToolExecutorService или ToolsDefinitionService
 */
public interface Tool {

    /**
     * Уникальное имя инструмента.
     */
    String getName();

    /**
     * Определение инструмента для MCP протокола (schema).
     */
    ToolDefinition getDefinition();

    /**
     * Выполнить инструмент.
     *
     * @param arguments аргументы вызова
     * @return результат выполнения
     * @throws ToolExecutionException при ошибке
     */
    Object execute(Map<String, Object> arguments);
}