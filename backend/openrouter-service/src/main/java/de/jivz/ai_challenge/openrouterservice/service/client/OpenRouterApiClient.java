package de.jivz.ai_challenge.openrouterservice.service.client;

import de.jivz.ai_challenge.openrouterservice.config.OpenRouterProperties;
import de.jivz.ai_challenge.openrouterservice.dto.Message;
import de.jivz.ai_challenge.openrouterservice.dto.OpenRouterApiRequest;
import de.jivz.ai_challenge.openrouterservice.dto.OpenRouterApiResponse;
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

            String reply = response.getChoices().get(0).getMessage().getContent();
            log.info("📥 OpenRouter response received");
            return reply;

        } catch (Exception e) {
            log.error("❌ Error calling OpenRouter: {}", e.getMessage());
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

    /**
     * Sendet eine Audio-Transkriptions-Anfrage an OpenRouter (Gemini Flash).
     * Verwendet multimodal input mit Base64-encodiertem Audio im input_audio Format.
     *
     * @param transcriptionPrompt Der Prompt für die Transkription
     * @param base64Audio Das Base64-encodierte Audio
     * @param audioFormat Das Audio-Format (z.B. "mp3", "wav")
     * @return Die Transkription
     */
    public String sendAudioTranscriptionRequest(String transcriptionPrompt, String base64Audio, String audioFormat) {
        log.info("🎤 Calling OpenRouter for audio transcription with Gemini Flash 2.0");

        try {
            // Build request mit text + audio content (OpenRouter format)
            OpenRouterApiRequest.ContentPart textPart = OpenRouterApiRequest.ContentPart.builder()
                    .type("text")
                    .text(transcriptionPrompt)
                    .build();

            OpenRouterApiRequest.ContentPart audioPart = OpenRouterApiRequest.ContentPart.builder()
                    .type("input_audio")
                    .inputAudio(OpenRouterApiRequest.InputAudio.builder()
                            .data(base64Audio)
                            .format(audioFormat)
                            .build())
                    .build();

            // Create message with multimodal content (List<ContentPart>)
            OpenRouterApiRequest.ChatMessage message = new OpenRouterApiRequest.ChatMessage();
            message.setRole("user");
            message.setContent(List.of(textPart, audioPart)); // Set as Object (List)

            OpenRouterApiRequest request = OpenRouterApiRequest.builder()
                    .model(properties.getTranscriptionModel())
                    .messages(List.of(message))
                    .temperature(0.1) // Low temperature for accurate transcription
                    .maxTokens(2000) // Enough for typical transcriptions
                    .build();

            log.debug("📤 Sending request to OpenRouter with model: {}", properties.getTranscriptionModel());

            OpenRouterApiResponse response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, r -> r.bodyToMono(String.class)
                            .flatMap(body -> {
                                log.error("❌ Transcription client error: {}", body);
                                return Mono.error(new RuntimeException("Transcription client error: " + body));
                            }))
                    .onStatus(HttpStatusCode::is5xxServerError, r -> r.bodyToMono(String.class)
                            .flatMap(body -> {
                                log.error("❌ Transcription server error: {}", body);
                                return Mono.error(new RuntimeException("Transcription server error: " + body));
                            }))
                    .bodyToMono(OpenRouterApiResponse.class)
                    .block();

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new RuntimeException("Empty response from transcription API");
            }

            String transcription = response.getChoices().get(0).getMessage().getContent();
            log.info("📝 Transcription received: {} characters", transcription.length());
            return transcription.trim();

        } catch (Exception e) {
            log.error("❌ Error during audio transcription: {}", e.getMessage());
            throw new RuntimeException("Failed to transcribe audio", e);
        }
    }
}

