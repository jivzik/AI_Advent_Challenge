package de.jivz.rag.mcp.tools;

import de.jivz.rag.dto.SearchResultDto;
import de.jivz.rag.mcp.McpModels.*;
import de.jivz.rag.service.RagFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool: search_documents
 * Семантический поиск по документам.
 */
@Component
@RequiredArgsConstructor
public class SearchDocumentsTool implements Tool {

    private static final String NAME = "search_documents";

    private final RagFacade ragFacade;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ToolDefinition getDefinition() {
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
                .name(NAME)
                .description("Семантический поиск по загруженным документам. " +
                        "Находит релевантные фрагменты текста по смысловому сходству с запросом.")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .required(List.of("query"))
                        .build())
                .build();
    }

    @Override
    public List<SearchResultDto> execute(Map<String, Object> arguments) {
        ToolArguments args = ToolArguments.of(arguments);

        String query = args.getRequiredString("query");
        int topK = args.getInt("topK", 5);
        double threshold = args.getDouble("threshold", 0.5);
        Long documentId = args.getLong("documentId", null);

        return ragFacade.search(query, topK, threshold, documentId);
    }
}