package de.jivz.teamassistantservice.service.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts document sources from RAG tool results.
 * Filters only project documentation (excludes FAQ, tutorials).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RagToolSourceExtractor implements MetadataExtractor<String> {

    private final ObjectMapper objectMapper;

    @Override
    public List<String> extract(String toolResult) {
        List<String> sources = new ArrayList<>();

        if (toolResult == null || toolResult.isBlank()) {
            return sources;
        }

        try {
            JsonNode results = objectMapper.readTree(toolResult);

            if (results.isArray()) {
                for (JsonNode result : results) {
                    String docName = result.path("documentName").asText();

                    if (isValidDocumentName(docName) && isProjectDocument(docName)) {
                        sources.add(docName);
                        log.debug("📚 Found project document: {}", docName);
                    }
                }
            }

            log.debug("📚 Extracted {} project sources from RAG result", sources.size());

        } catch (Exception e) {
            log.debug("Could not extract sources from RAG result: {}", e.getMessage());
        }

        return sources;
    }

    @Override
    public String getMetadataType() {
        return "rag-sources";
    }

    /**
     * Validates document name.
     */
    private boolean isValidDocumentName(String docName) {
        return docName != null
                && !docName.isBlank()
                && !docName.equals("null");
    }

    /**
     * Filters only project documentation.
     * Team Service should only reference:
     * - ARCHITECTURE.md
     * - API.md
     * - DATABASE.md
     * - TECHNICAL_DEBT.md
     * - DEPLOYMENT.md
     * - README.md
     *
     * Should NOT reference:
     * - FAQ documents (for Support Service)
     * - Technical docs (QUICKSTART, TUTORIAL)
     */
    private boolean isProjectDocument(String docName) {
        if (docName == null || docName.isBlank()) {
            return false;
        }

        String upper = docName.toUpperCase();

        // Exclude technical/tutorial documents and FAQ
        String[] excludePatterns = {
                "QUICKSTART",
                "TUTORIAL",
                "TOOL_CALLING",
                "FAQ",
                "WEBSHOP_FAQ"
        };

        for (String exclude : excludePatterns) {
            if (upper.contains(exclude)) {
                return false;
            }
        }

        // Only allow project documentation
        String[] projectPatterns = {
                "ARCHITECTURE",
                "API",
                "DATABASE",
                "TECHNICAL_DEBT",
                "DEPLOYMENT",
                "README"
        };

        for (String pattern : projectPatterns) {
            if (upper.contains(pattern)) {
                return true;
            }
        }

        return false;
    }
}

