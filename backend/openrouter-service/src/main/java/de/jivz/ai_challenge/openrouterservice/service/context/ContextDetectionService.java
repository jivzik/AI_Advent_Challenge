package de.jivz.ai_challenge.openrouterservice.service.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.ai_challenge.openrouterservice.dto.Message;
import de.jivz.ai_challenge.openrouterservice.mcp.model.ToolDefinition;
import de.jivz.ai_challenge.openrouterservice.service.PromptLoaderService;
import de.jivz.ai_challenge.openrouterservice.service.client.OpenRouterApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service für LLM-basierte Kontext-Erkennung.
 * Verwendet ein schnelles Modell um den Kontext der Benutzeranfrage zu klassifizieren.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContextDetectionService {

    private static final double MIN_CONFIDENCE = 0.5;
    private static final String DEFAULT_CONTEXT = "default";

    private final OpenRouterApiClient apiClient;
    private final PromptLoaderService promptLoader;
    private final ObjectMapper objectMapper;

    /**
     * Erkennt den Kontext einer Benutzeranfrage.
     *
     * @param userMessage Die Benutzeranfrage
     * @param tools Die verfügbaren MCP-Tools
     * @return Der erkannte Kontext (docker, tasks, calendar, default)
     */
    public String detectContext(String userMessage, List<ToolDefinition> tools) {
        try {
            String detectionPrompt = promptLoader.buildContextDetectionPrompt(userMessage, tools);
            if (detectionPrompt == null) {
                log.warn("Context detection prompt not available, using default");
                return DEFAULT_CONTEXT;
            }

            List<Message> messages = List.of(
                    new Message("system", "You are a context classifier. Respond only with JSON."),
                    new Message("user", detectionPrompt)
            );

            String response = apiClient.sendContextDetectionRequest(messages);
            log.debug("Context detection response: {}", response);

            return parseContext(response);

        } catch (Exception e) {
            log.error("Error detecting context: {}", e.getMessage());
            return DEFAULT_CONTEXT;
        }
    }

    /**
     * Extrahiert den Kontext aus der LLM-Antwort.
     *
     * @param response Die LLM-Antwort
     * @return Der erkannte Kontext
     */
    private String parseContext(String response) {
        if (response == null || response.isBlank()) {
            return DEFAULT_CONTEXT;
        }

        try {
            var node = objectMapper.readTree(response);
            String context = node.path("context").asText(DEFAULT_CONTEXT);
            double confidence = node.path("confidence").asDouble(0.0);

            log.info("🎯 Context: {} (confidence: {})", context, confidence);

            // Nur akzeptieren wenn Confidence hoch genug
            if (confidence >= MIN_CONFIDENCE) {
                return context;
            }

            return DEFAULT_CONTEXT;

        } catch (JsonProcessingException e) {
            log.warn("Could not parse context response: {}", response);
            return DEFAULT_CONTEXT;
        }
    }
}

