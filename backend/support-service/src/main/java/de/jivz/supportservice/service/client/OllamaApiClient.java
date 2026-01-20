package de.jivz.supportservice.service.client;

import de.jivz.supportservice.config.OllamaProperties;
import de.jivz.supportservice.dto.Message;
import de.jivz.supportservice.dto.OllamaRequest;
import de.jivz.supportservice.dto.OllamaResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client für die lokale Ollama LLM API.
 * Verantwortlich für alle HTTP-Kommunikation mit Ollama.
 */
@Service
@Slf4j
public class OllamaApiClient {

    private final WebClient webClient;
    private final OllamaProperties properties;

    public OllamaApiClient(
            @Qualifier("ollamaWebClient") WebClient webClient,
            OllamaProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    /**
     * Sendet eine Chat-Anfrage an Ollama.
     *
     * @param messages Die Chat-Nachrichten
     * @param temperature Die Temperatur für die Generierung
     * @param maxTokens Maximale Anzahl von Tokens
     * @return Die Antwort des Modells
     */
    public String sendChatRequest(List<Message> messages, Double temperature, Integer maxTokens) {
        long startTime = System.currentTimeMillis();
        log.info("🤖 Calling Ollama with {} messages", messages.size());

        try {
            // Baue den Prompt aus den Messages
            String prompt = buildPromptFromMessages(messages);
            String systemPrompt = extractSystemPrompt(messages);

            // Baue Options Map
            Map<String, Object> options = new HashMap<>();
            options.put("temperature", temperature != null ? temperature : properties.getTemperature());
            options.put("num_predict", maxTokens != null ? maxTokens : properties.getMaxTokens());

            OllamaRequest request = OllamaRequest.builder()
                    .model(properties.getModel())
                    .prompt(prompt)
                    .system(systemPrompt)
                    .stream(false)
                    .options(options)
                    .build();

            OllamaResponse response = webClient.post()
                    .uri("/api/generate")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, r -> r.bodyToMono(String.class)
                            .flatMap(body -> Mono.error(new RuntimeException("Ollama client error: " + body))))
                    .onStatus(HttpStatusCode::is5xxServerError, r -> r.bodyToMono(String.class)
                            .flatMap(body -> Mono.error(new RuntimeException("Ollama server error: " + body))))
                    .bodyToMono(OllamaResponse.class)
                    .block();

            if (response == null || response.getResponse() == null) {
                throw new RuntimeException("Empty response from Ollama");
            }

            String reply = response.getResponse();

            long duration = System.currentTimeMillis() - startTime;
            log.info("🤖 Ollama response received in {} ms. Tokens: {}", duration, response.getEvalCount());
            return reply;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Error calling Ollama after {} ms: {}", duration, e.getMessage());
            throw new RuntimeException("Failed to call Ollama API", e);
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
        log.debug("🔍 Sending context detection request to Ollama");
        return sendChatRequest(messages, 0.1, 100);
    }

    /**
     * Baut einen Prompt aus den Messages.
     * System-Messages werden separat behandelt.
     */
    private String buildPromptFromMessages(List<Message> messages) {
        StringBuilder prompt = new StringBuilder();

        for (Message message : messages) {
            if (!"system".equals(message.getRole())) {
                if ("user".equals(message.getRole())) {
                    prompt.append("User: ").append(message.getContent()).append("\n\n");
                } else if ("assistant".equals(message.getRole())) {
                    prompt.append("Assistant: ").append(message.getContent()).append("\n\n");
                }
            }
        }

        return prompt.toString().trim();
    }

    /**
     * Extrahiert den System-Prompt aus den Messages.
     */
    private String extractSystemPrompt(List<Message> messages) {
        return messages.stream()
                .filter(m -> "system".equals(m.getRole()))
                .map(Message::getContent)
                .findFirst()
                .orElse(null);
    }
}

