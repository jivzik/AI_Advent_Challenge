package de.jivz.ai_challenge.openrouterservice.service;

import de.jivz.ai_challenge.openrouterservice.config.OpenRouterProperties;
import de.jivz.ai_challenge.openrouterservice.dto.ChatRequest;
import de.jivz.ai_challenge.openrouterservice.dto.ChatResponse;
import de.jivz.ai_challenge.openrouterservice.dto.OpenRouterApiRequest;
import de.jivz.ai_challenge.openrouterservice.dto.OpenRouterApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * WebClient-basierter Chat Service für OpenRouter
 * Verwendet den bewährten Ansatz aus dem perplexity-service
 */
@Slf4j
@Service
public class OpenRouterAiChatService {

    private final WebClient webClient;
    private final OpenRouterProperties properties;

    public OpenRouterAiChatService(
            @Qualifier("openRouterWebClient") WebClient webClient,
            OpenRouterProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
        log.info("OpenRouterAiChatService initialized with WebClient");
        log.info("Default model: {}", properties.getDefaultModel());
    }

    /**
     * Einfache Chat-Anfrage mit Standard-Parametern
     */
    public ChatResponse chat(String message) {
        return chat(message, null, null, null);
    }

    /**
     * Chat-Anfrage mit allen Parametern
     */
    public ChatResponse chat(String message, String model, Double temperature, Integer maxTokens) {
        log.info("Processing chat request - Message length: {}", message.length());

        String resolvedModel = model != null ? model : properties.getDefaultModel();
        Double resolvedTemperature = temperature != null ? temperature : properties.getDefaultTemperature();
        Integer resolvedMaxTokens = maxTokens != null ? maxTokens : properties.getDefaultMaxTokens();

        OpenRouterApiRequest request = OpenRouterApiRequest.builder()
                .model(resolvedModel)
                .messages(List.of(
                        OpenRouterApiRequest.ChatMessage.builder()
                                .role("user")
                                .content(message)
                                .build()
                ))
                .temperature(resolvedTemperature)
                .maxTokens(resolvedMaxTokens)
                .topP(properties.getDefaultTopP())
                .build();

        log.info("Calling OpenRouter API - Model: {}, Temperature: {}", resolvedModel, resolvedTemperature);

        long startTime = System.currentTimeMillis();

        try {
            OpenRouterApiResponse response = callApi(request);
            long responseTime = System.currentTimeMillis() - startTime;

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new RuntimeException("Empty or invalid response from OpenRouter API");
            }

            OpenRouterApiResponse.Choice firstChoice = response.getChoices().get(0);
            String reply = firstChoice.getMessage().getContent();
            String finishReason = firstChoice.getFinishReason();

            log.info("Chat completed successfully in {} ms", responseTime);
            log.info("Model used: {}, Finish reason: {}", response.getModel(), finishReason);

            // Log usage if available
            if (response.getUsage() != null) {
                OpenRouterApiResponse.Usage usage = response.getUsage();
                log.info("Tokens - Input: {}, Output: {}, Total: {}",
                        usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
            }

            return ChatResponse.builder()
                    .reply(reply)
                    .model(response.getModel())
                    .responseTimeMs(responseTime)
                    .finishReason(finishReason)
                    .build();

        } catch (Exception e) {
            log.error("Chat request failed: {}", e.getMessage(), e);
            throw new RuntimeException("Chat request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Chat mit Chat-Request DTO
     */
    public ChatResponse chatWithRequest(ChatRequest request) {
        log.info("Processing ChatRequest - Model: {}, Message length: {}",
                request.getModel(), request.getMessage().length());

        String message = request.getMessage();

        // Wenn Conversation History vorhanden, mit einbeziehen
        if (request.getConversationHistory() != null && !request.getConversationHistory().isEmpty()) {
            message = buildConversationContext(request.getConversationHistory()) + "\n\nNeu: " + message;
        }

        return chat(message, request.getModel(), request.getTemperature(), request.getMaxTokens());
    }

    /**
     * Erstellt Kontext aus Conversation History
     */
    private String buildConversationContext(List<String> conversationHistory) {
        StringBuilder context = new StringBuilder("Conversation History:\n");
        for (int i = 0; i < conversationHistory.size(); i++) {
            context.append(i + 1).append(". ").append(conversationHistory.get(i)).append("\n");
        }
        return context.toString();
    }

    /**
     * API Call via WebClient
     */
    private OpenRouterApiResponse callApi(OpenRouterApiRequest request) {
        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("OpenRouter API client error: {} - Body: {}",
                                            clientResponse.statusCode(), errorBody);
                                    return Mono.error(new RuntimeException(
                                            "OpenRouter API error: " + clientResponse.statusCode() +
                                                    " - " + errorBody));
                                })
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("OpenRouter API server error: {} - Body: {}",
                                            clientResponse.statusCode(), errorBody);
                                    return Mono.error(new RuntimeException(
                                            "OpenRouter API server error: " + clientResponse.statusCode() +
                                                    " - " + errorBody));
                                })
                )
                .bodyToMono(OpenRouterApiResponse.class)
                .block();
    }
}

