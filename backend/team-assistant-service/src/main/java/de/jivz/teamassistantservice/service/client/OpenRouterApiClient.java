package de.jivz.teamassistantservice.service.client;


import de.jivz.teamassistantservice.config.OpenRouterProperties;

import de.jivz.teamassistantservice.dto.Message;
import de.jivz.teamassistantservice.dto.OpenRouterApiRequest;
import de.jivz.teamassistantservice.dto.OpenRouterApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Client für die OpenRouter API.
 * Verantwortlich für alle HTTP-Kommunikation mit OpenRouter.
 * Kapselt WebClient-Logik und Error-Handling.
 */
@Service
@Slf4j
public class OpenRouterApiClient {

    private final WebClient webClient;
    private final OpenRouterProperties properties;

    public OpenRouterApiClient(
            @Qualifier("openRouterWebClient") WebClient webClient,
            OpenRouterProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    /**
     * Sendet eine Chat-Anfrage an OpenRouter.
     *
     * @param messages Die Chat-Nachrichten
     * @param temperature Die Temperatur für die Generierung
     * @param maxTokens Maximale Anzahl von Tokens
     * @return Die Antwort des Modells
     */
    public String sendChatRequest(List<Message> messages, Double temperature, Integer maxTokens) {
        long startTime = System.currentTimeMillis();
        log.info("📤 Calling OpenRouter with {} messages", messages.size());

        try {
            List<OpenRouterApiRequest.ChatMessage> apiMessages = messages.stream()
                    .map(m -> OpenRouterApiRequest.ChatMessage.builder()
                            .role(m.getRole())
                            .content(m.getContent())
                            .build())
                    .collect(Collectors.toList());

            OpenRouterApiRequest request = OpenRouterApiRequest.builder()
                    .model(properties.getDefaultModel())
                    .messages(apiMessages)
                    .temperature(temperature != null ? temperature : properties.getDefaultTemperature())
                    .maxTokens(maxTokens != null ? maxTokens : properties.getDefaultMaxTokens())
                    .topP(properties.getDefaultTopP())
                    .build();

            OpenRouterApiResponse response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, r -> r.bodyToMono(String.class)
                            .flatMap(body -> Mono.error(new RuntimeException("Client error: " + body))))
                    .onStatus(HttpStatusCode::is5xxServerError, r -> r.bodyToMono(String.class)
                            .flatMap(body -> Mono.error(new RuntimeException("Server error: " + body))))
                    .bodyToMono(OpenRouterApiResponse.class)
                    .block();

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new RuntimeException("Empty response from OpenRouter");
            }

            String reply = response.getChoices().getFirst().getMessage().getContent();

            long duration = System.currentTimeMillis() - startTime;
            log.info("📥 OpenRouter response received in {} ms. Usage: {}", duration, response.getUsage());
            return reply;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Error calling OpenRouter after {} ms: {}", duration, e.getMessage());
            throw new RuntimeException("Failed to call OpenRouter API", e);
        }
    }

    /**
     * Sendet eine schnelle Anfrage für Kontext-Erkennung.
     * Verwendet niedrige max_tokens für schnelle Antwort.
     *
     * @param messages Die Nachrichten für die Kontext-Erkennung
     * @return Die Antwort des Modells
     */
    public String sendContextDetectionRequest(List<Message> messages) {
        log.debug("🔍 Sending context detection request");
        return sendChatRequest(messages, 0.1, 100);
    }
}

