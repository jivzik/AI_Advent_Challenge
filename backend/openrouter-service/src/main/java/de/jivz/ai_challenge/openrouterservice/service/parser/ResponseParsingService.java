package de.jivz.ai_challenge.openrouterservice.service.parser;

import de.jivz.ai_challenge.openrouterservice.dto.Message;
import de.jivz.ai_challenge.openrouterservice.dto.ToolResponse;
import de.jivz.ai_challenge.openrouterservice.service.PromptLoaderService;
import de.jivz.ai_challenge.openrouterservice.service.client.OpenRouterApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service für Response-Parsing mit automatischer Strategie-Auswahl und Retry-Logik.
 * Verwendet verschiedene Parser-Strategien basierend auf dem Response-Format.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ResponseParsingService {

    private final List<ResponseParserStrategy> parserStrategies;
    private final OpenRouterApiClient apiClient;
    private final PromptLoaderService promptLoader;

    /**
     * Parst die Response mit automatischer Strategie-Auswahl.
     * Bei Fehler wird ein Retry mit Korrektur-Prompt versucht.
     *
     * @param response Die zu parsende Response
     * @param messages Die bisherigen Nachrichten (für Retry)
     * @param temperature Die Temperatur für den Retry
     * @return Das geparste ToolResponse-Objekt oder null bei Fehler
     */
    public ToolResponse parseWithRetry(String response, List<Message> messages, Double temperature) {
        // Versuche zuerst mit verfügbaren Strategien
        ToolResponse result = parseWithStrategies(response);
        if (result != null) {
            return result;
        }

        // Retry mit Korrektur-Prompt
        log.warn("⚠️ Attempting retry for malformed response");

        messages.add(new Message("assistant", response));
        messages.add(new Message("user", promptLoader.getJsonCorrectionPrompt()));

        try {
            String retryResponse = apiClient.sendChatRequest(messages, temperature, null);
            result = parseWithStrategies(retryResponse);

            if (result != null) {
                log.info("✅ Retry successful");
                return result;
            }
        } catch (Exception retryException) {
            log.error("❌ Retry also failed: {}", retryException.getMessage());
        }

        return null;
    }

    /**
     * Versucht die Response mit allen verfügbaren Strategien zu parsen.
     *
     * @param response Die zu parsende Response
     * @return Das geparste ToolResponse-Objekt oder null
     */
    private ToolResponse parseWithStrategies(String response) {
        for (ResponseParserStrategy strategy : parserStrategies) {
            if (strategy.canParse(response)) {
                try {
                    return strategy.parse(response);
                } catch (ResponseParsingException e) {
                    log.debug("Strategy {} failed: {}", strategy.getClass().getSimpleName(), e.getMessage());
                    // Versuche nächste Strategie
                }
            }
        }
        return null;
    }
}

