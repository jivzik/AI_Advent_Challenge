package de.jivz.rag.mcp;

import de.jivz.rag.dto.DocumentDto;
import de.jivz.rag.dto.SearchResultDto;
import de.jivz.rag.entity.Document;
import de.jivz.rag.mcp.McpModels.*;
import de.jivz.rag.repository.DocumentRepository;
import de.jivz.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Сервис для выполнения MCP инструментов.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ToolExecutorService {

    private final RagService ragService;
    private final DocumentRepository documentRepository;

    /**
     * Выполняет MCP инструмент по имени.
     */
    public ToolCallResponse execute(String toolName, Map<String, Object> arguments) {
        log.info("🔧 Executing tool: {} with args: {}", toolName, arguments);

        try {
            Object result = switch (toolName) {
                case "search_documents" -> executeSearchDocuments(arguments);
                case "list_documents" -> executeListDocuments(arguments);
                case "get_document_info" -> executeGetDocumentInfo(arguments);
                default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
            };

            log.info("✅ Tool {} executed successfully", toolName);
            return ToolCallResponse.builder()
                    .success(true)
                    .result(result)
                    .build();

        } catch (Exception e) {
            log.error("❌ Tool {} failed: {}", toolName, e.getMessage());
            return ToolCallResponse.builder()
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * search_documents: семантический поиск.
     */
    private List<SearchResultDto> executeSearchDocuments(Map<String, Object> args) {
        String query = getStringArg(args, "query");
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query is required");
        }

        int topK = getIntArg(args, "topK", 5);
        double threshold = getDoubleArg(args, "threshold", 0.5);
        Long documentId = getLongArg(args, "documentId", null);

        return ragService.search(query, topK, threshold, documentId);
    }

    /**
     * list_documents: список документов.
     */
    private List<DocumentDto> executeListDocuments(Map<String, Object> args) {
        String statusFilter = getStringArg(args, "status");

        if (statusFilter != null && !statusFilter.isBlank()) {
            try {
                Document.DocumentStatus status = Document.DocumentStatus.valueOf(statusFilter.toUpperCase());
                return documentRepository.findByStatus(status).stream()
                        .map(DocumentDto::fromEntity)
                        .toList();
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status filter: {}", statusFilter);
            }
        }

        return ragService.getAllDocuments();
    }

    /**
     * get_document_info: информация о документе.
     */
    private DocumentDto executeGetDocumentInfo(Map<String, Object> args) {
        Long documentId = getLongArg(args, "documentId", null);
        if (documentId == null) {
            throw new IllegalArgumentException("documentId is required");
        }

        DocumentDto doc = ragService.getDocument(documentId);
        if (doc == null) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }
        return doc;
    }

    // ========== Helper methods ==========

    private String getStringArg(Map<String, Object> args, String key) {
        Object value = args.get(key);
        return value != null ? value.toString() : null;
    }

    private int getIntArg(Map<String, Object> args, String key, int defaultValue) {
        Object value = args.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private double getDoubleArg(Map<String, Object> args, String key, double defaultValue) {
        Object value = args.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Long getLongArg(Map<String, Object> args, String key, Long defaultValue) {
        Object value = args.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}

