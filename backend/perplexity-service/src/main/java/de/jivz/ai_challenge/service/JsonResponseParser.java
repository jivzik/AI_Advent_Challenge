package de.jivz.ai_challenge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.ai_challenge.dto.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service responsible for parsing and cleaning JSON responses from LLM.
 * Follows Single Responsibility Principle.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JsonResponseParser {

    private final ObjectMapper objectMapper;

    /**
     * Parses and cleans the JSON response from the LLM.
     *
     * @param rawResponse The raw response from Perplexity
     * @param request     The chat request to determine parsing mode
     * @return Parsed response (extracted text for simple format, or cleaned JSON for custom/auto schema)
     */
    public String parse(String rawResponse, ChatRequest request) {
        try {
            String cleaned = cleanMarkdown(rawResponse);
            log.debug("🧹 Cleaned JSON string: {}", cleaned.substring(0, Math.min(100, cleaned.length())));

            // Validate JSON
            JsonNode node = objectMapper.readTree(cleaned);

            // Custom schema or auto-schema: return entire JSON
            if (shouldReturnFullJson(request)) {
                log.info("✅ Successfully validated {} JSON response",
                        StringUtils.hasText(request.getJsonSchema()) ? "custom-schema" : "auto-schema");
                return cleaned;
            }

            // Simple format: extract "response" field
            return extractResponseField(node, cleaned);

        } catch (Exception e) {
            log.error("❌ Failed to parse JSON response. Raw: {}",
                    rawResponse.substring(0, Math.min(200, rawResponse.length())), e);
            return rawResponse; // Return raw response on error
        }
    }

    /**
     * Removes markdown code block syntax from JSON response.
     *
     * @param rawResponse The raw response with potential markdown
     * @return Cleaned JSON string
     */
    private String cleanMarkdown(String rawResponse) {
        String cleaned = rawResponse.trim();

        // Remove opening markdown fence
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring("```json".length()).trim();
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring("```".length()).trim();
        }

        // Remove closing markdown fence
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }

        return cleaned;
    }

    /**
     * Determines if the full JSON should be returned or just the response field.
     *
     * @param request The chat request
     * @return true if full JSON should be returned
     */
    private boolean shouldReturnFullJson(ChatRequest request) {
        return StringUtils.hasText(request.getJsonSchema()) || request.isAutoSchema();
    }

    /**
     * Extracts the "response" field from the JSON node.
     *
     * @param node    The JSON node
     * @param cleaned The cleaned JSON string (fallback)
     * @return The extracted response or full JSON if response field is missing
     */
    private String extractResponseField(JsonNode node, String cleaned) throws Exception {
        if (!node.has("response")) {
            log.warn("⚠️ JSON response missing 'response' field, returning full JSON: {}",
                    cleaned.substring(0, Math.min(100, cleaned.length())));
            return cleaned;
        }

        JsonNode responseNode = node.get("response");

        // If response is a string, return it directly
        if (responseNode.isTextual()) {
            log.info("✅ Successfully parsed JSON response (text)");
            return responseNode.asText();
        }

        // If response is an object/array, return it as JSON string
        String response = objectMapper.writeValueAsString(responseNode);
        log.info("✅ Successfully parsed JSON response (structured)");
        return response;
    }
}

