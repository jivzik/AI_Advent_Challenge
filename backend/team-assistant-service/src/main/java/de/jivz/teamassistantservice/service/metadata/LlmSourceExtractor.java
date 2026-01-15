package de.jivz.teamassistantservice.service.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts document sources from LLM JSON responses.
 * Handles the "sources" field in ToolResponse.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LlmSourceExtractor implements MetadataExtractor<String> {

    private final ObjectMapper objectMapper;

    @Override
    public List<String> extract(String jsonResponse) {
        List<String> sources = new ArrayList<>();

        if (jsonResponse == null || jsonResponse.isBlank()) {
            return sources;
        }

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode sourcesNode = root.path("sources");

            if (sourcesNode.isArray()) {
                for (JsonNode source : sourcesNode) {
                    if (source.isTextual() && !source.asText().isBlank()) {
                        sources.add(source.asText());
                    }
                }
            }

            log.debug("📚 Extracted {} sources from LLM response", sources.size());

        } catch (Exception e) {
            log.debug("Could not extract sources from JSON: {}", e.getMessage());
        }

        return sources;
    }

    @Override
    public String getMetadataType() {
        return "sources";
    }
}

