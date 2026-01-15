package de.jivz.teamassistantservice.service.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts tools used from LLM JSON responses.
 * Handles the "toolsUsed" field in ToolResponse.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LlmToolsUsedExtractor implements MetadataExtractor<String> {

    private final ObjectMapper objectMapper;

    @Override
    public List<String> extract(String jsonResponse) {
        List<String> toolsUsed = new ArrayList<>();

        if (jsonResponse == null || jsonResponse.isBlank()) {
            return toolsUsed;
        }

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode toolsNode = root.path("toolsUsed");

            if (toolsNode.isArray()) {
                for (JsonNode tool : toolsNode) {
                    if (tool.isTextual() && !tool.asText().isBlank()) {
                        toolsUsed.add(tool.asText());
                    }
                }
            }

            log.debug("🔧 Extracted {} tools from LLM response", toolsUsed.size());

        } catch (Exception e) {
            log.debug("Could not extract toolsUsed from JSON: {}", e.getMessage());
        }

        return toolsUsed;
    }

    @Override
    public String getMetadataType() {
        return "toolsUsed";
    }
}

