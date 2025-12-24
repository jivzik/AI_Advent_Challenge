package de.jivz.rag.mcp.tools;

import de.jivz.rag.dto.DocumentDto;
import de.jivz.rag.mcp.McpModels.*;
import de.jivz.rag.service.RagFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool: get_document_info
 * Детальная информация о документе.
 */
@Component
@RequiredArgsConstructor
public class GetDocumentInfoTool implements Tool {

    private static final String NAME = "get_document_info";

    private final RagFacade ragFacade;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ToolDefinition getDefinition() {
        Map<String, PropertyDefinition> properties = new LinkedHashMap<>();

        properties.put("documentId", PropertyDefinition.builder()
                .type("integer")
                .description("ID документа для получения информации")
                .build());

        return ToolDefinition.builder()
                .name(NAME)
                .description("Получить детальную информацию о конкретном документе, " +
                        "включая его статус, размер и количество чанков.")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .required(List.of("documentId"))
                        .build())
                .build();
    }

    @Override
    public DocumentDto execute(Map<String, Object> arguments) {
        ToolArguments args = ToolArguments.of(arguments);

        Long documentId = args.getRequiredLong("documentId");

        return ragFacade.getDocument(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
    }
}