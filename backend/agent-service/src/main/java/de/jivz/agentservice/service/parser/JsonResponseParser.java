package de.jivz.agentservice.service.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.jivz.agentservice.dto.ToolResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Parser für JSON-formatierte Responses.
 * Bereinigt Markdown-Blöcke und parst das JSON zu ToolResponse.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JsonResponseParser implements ResponseParserStrategy {

    private final ObjectMapper objectMapper;

    @Override
    public boolean canParse(String response) {
        if (response == null || response.isBlank()) {
            return false;
        }

        String cleaned = cleanJsonResponse(response);
        return cleaned.trim().startsWith("{") || cleaned.trim().startsWith("[");
    }

    @Override
    public ToolResponse parse(String response) throws ResponseParsingException {
        String cleaned = cleanJsonResponse(response);

        try {
            ToolResponse toolResponse = objectMapper.readValue(cleaned, ToolResponse.class);
            log.debug("✅ Successfully parsed JSON response");
            return toolResponse;
        } catch (JsonProcessingException e) {
            log.warn("⚠️ Failed to parse JSON: {}", e.getMessage());
            throw new ResponseParsingException("Failed to parse JSON response", e);
        }
    }

    /**
     * Bereinigt die Antwort von Markdown-Blöcken.
     *
     * @param response Die zu bereinigende Response
     * @return Die bereinigte Response
     */
    private String cleanJsonResponse(String response) {
        if (response == null) {
            return null;
        }

        String cleaned = response.trim();

        // Entfernt ```json ... ```
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }
}

