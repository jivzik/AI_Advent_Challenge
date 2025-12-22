package de.jivz.rag.mcp;

import de.jivz.rag.mcp.McpModels.*;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис для определения MCP Tools.
 *
 * Доступные инструменты:
 * - search_documents: семантический поиск по документам
 * - list_documents: список загруженных документов
 * - get_document_info: информация о документе
 */
@Service
public class ToolsDefinitionService {

    /**
     * Возвращает список всех доступных MCP инструментов.
     */
    public List<ToolDefinition> getToolDefinitions() {
        return List.of(
                createSearchDocumentsTool(),
                createListDocumentsTool(),
                createGetDocumentInfoTool()
        );
    }

    /**
     * Tool: search_documents
     * Семантический поиск по загруженным документам.
     */
    private ToolDefinition createSearchDocumentsTool() {
        Map<String, PropertyDefinition> properties = new LinkedHashMap<>();

        properties.put("query", PropertyDefinition.builder()
                .type("string")
                .description("Поисковый запрос (текст для семантического поиска)")
                .build());

        properties.put("topK", PropertyDefinition.builder()
                .type("integer")
                .description("Количество результатов (по умолчанию: 5)")
                .defaultValue(5)
                .build());

        properties.put("threshold", PropertyDefinition.builder()
                .type("number")
                .description("Минимальный порог сходства от 0.0 до 1.0 (по умолчанию: 0.5)")
                .defaultValue(0.5)
                .build());

        properties.put("documentId", PropertyDefinition.builder()
                .type("integer")
                .description("ID документа для поиска только в нём (опционально)")
                .build());

        return ToolDefinition.builder()
                .name("search_documents")
                .description("Семантический поиск по загруженным документам. " +
                        "Находит релевантные фрагменты текста по смысловому сходству с запросом. " +
                        "Используй для поиска информации в базе знаний.")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .required(List.of("query"))
                        .build())
                .build();
    }

    /**
     * Tool: list_documents
     * Список всех загруженных документов.
     */
    private ToolDefinition createListDocumentsTool() {
        Map<String, PropertyDefinition> properties = new LinkedHashMap<>();

        properties.put("status", PropertyDefinition.builder()
                .type("string")
                .description("Фильтр по статусу: PROCESSING, READY, ERROR (опционально)")
                .build());

        return ToolDefinition.builder()
                .name("list_documents")
                .description("Получить список всех загруженных документов в базе знаний. " +
                        "Возвращает информацию о файлах, их статусе и количестве чанков.")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .required(List.of())
                        .build())
                .build();
    }

    /**
     * Tool: get_document_info
     * Детальная информация о документе.
     */
    private ToolDefinition createGetDocumentInfoTool() {
        Map<String, PropertyDefinition> properties = new LinkedHashMap<>();

        properties.put("documentId", PropertyDefinition.builder()
                .type("integer")
                .description("ID документа для получения информации")
                .build());

        return ToolDefinition.builder()
                .name("get_document_info")
                .description("Получить детальную информацию о конкретном документе, " +
                        "включая его статус, размер и количество чанков.")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .required(List.of("documentId"))
                        .build())
                .build();
    }
}

