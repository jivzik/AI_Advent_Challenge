package de.jivz.rag.mcp.tools;

import de.jivz.rag.dto.DocumentDto;
import de.jivz.rag.repository.entity.Document.DocumentStatus;
import de.jivz.rag.mcp.McpModels.*;
import de.jivz.rag.repository.DocumentRepository;
import de.jivz.rag.service.RagFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tool: list_documents
 * Список загруженных документов.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ListDocumentsTool implements Tool {

    private static final String NAME = "list_documents";

    private final RagFacade ragFacade;
    private final DocumentRepository documentRepository;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ToolDefinition getDefinition() {
        Map<String, PropertyDefinition> properties = new LinkedHashMap<>();

        properties.put("status", PropertyDefinition.builder()
                .type("string")
                .description("Фильтр по статусу: PROCESSING, READY, ERROR (опционально)")
                .build());

        return ToolDefinition.builder()
                .name(NAME)
                .description("Получить список всех загруженных документов в базе знаний. " +
                        "Возвращает информацию о файлах, их статусе и количестве чанков.")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .required(List.of())
                        .build())
                .build();
    }

    @Override
    public List<DocumentDto> execute(Map<String, Object> arguments) {
        ToolArguments args = ToolArguments.of(arguments);

        return args.getString("status")
                .filter(s -> !s.isBlank())
                .flatMap(this::parseStatus)
                .map(this::findByStatus)
                .orElseGet(ragFacade::getAllDocuments);
    }

    private Optional<DocumentStatus> parseStatus(String statusStr) {
        try {
            return Optional.of(DocumentStatus.valueOf(statusStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid status filter: {}", statusStr);
            return Optional.empty();
        }
    }

    private List<DocumentDto> findByStatus(DocumentStatus status) {
        return documentRepository.findByStatus(status).stream()
                .map(DocumentDto::fromEntity)
                .toList();
    }
}