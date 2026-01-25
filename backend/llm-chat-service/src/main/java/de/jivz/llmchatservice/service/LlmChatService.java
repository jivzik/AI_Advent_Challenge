package de.jivz.llmchatservice.service;

import de.jivz.llmchatservice.config.LlmProperties;
import de.jivz.llmchatservice.dto.ChatRequest;
import de.jivz.llmchatservice.dto.ChatResponse;
import de.jivz.llmchatservice.dto.OllamaRequest;
import de.jivz.llmchatservice.dto.OllamaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for integrating with Ollama LLM via WebClient.
 * Handles request/response transformation and API communication.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmChatService {

    private final WebClient ollamaWebClient;
    private final LlmProperties llmProperties;

    /**
     * Sends a chat message to Ollama and returns the response.
     *
     * @param request Chat request with message and optional parameters
     * @return Mono of ChatResponse containing LLM response and metadata
     */
    public Mono<ChatResponse> chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();

        log.info("Processing chat request with message: {}",
                request.getMessage() != null && request.getMessage().length() > 50
                        ? request.getMessage().substring(0, 50) + "..."
                        : request.getMessage());

        // Build Ollama request with parameters
        OllamaRequest ollamaRequest = buildOllamaRequest(request);

        // Call Ollama API
        return ollamaWebClient
                .post()
                .uri("/api/generate")
                .bodyValue(ollamaRequest)
                .retrieve()
                .bodyToMono(OllamaResponse.class)
                .map(ollamaResponse -> mapToResponse(ollamaResponse, startTime))
                .doOnSuccess(response -> log.info("Chat completed in {} ms", response.getProcessingTimeMs()))
                .doOnError(error -> log.error("Error calling Ollama API: {}", error.getMessage(), error))
                .onErrorResume(error -> Mono.just(buildErrorResponse(error, startTime)));
    }

    /**
     * Builds Ollama API request from chat request.
     */
    private OllamaRequest buildOllamaRequest(ChatRequest request) {
        // Determine model to use
        String model = request.getModel() != null
                ? request.getModel()
                : llmProperties.getModel();

        // Build options map with OPTIMIZED PARAMETERS
        Map<String, Object> options = new HashMap<>();

        // Temperature
        Double temperature = request.getTemperature() != null
                ? request.getTemperature()
                : llmProperties.getTemperature();
        options.put("temperature", temperature);
        options.put("stream",false);

        // Max tokens (num_predict in Ollama)
        Integer maxTokens = request.getMaxTokens() != null
                ? request.getMaxTokens()
                : llmProperties.getMaxTokens();
        options.put("num_predict", maxTokens);

        // 🆕 OPTIMIZATION PARAMETERS
        options.put("num_ctx", 4096);           // Context window size
        options.put("top_k", 40);               // Limit vocabulary
        options.put("top_p", 0.9);              // Nucleus sampling
        options.put("repeat_penalty", 1.1);     // Avoid repetition

        // 🆕 OPTIMIZED SYSTEM PROMPT (if not provided)
        String systemPrompt = request.getSystemPrompt();
        if (systemPrompt == null || systemPrompt.trim().isEmpty()) {
            systemPrompt = "Ты AI ассистент для разработчиков Spring Boot и Vue.js.\n\n" +
                    "Правила ответов:\n" +
                    "1. Отвечай кратко и точно\n" +
                    "2. Примеры кода в markdown формате\n" +
                    "3. Указывай на потенциальные проблемы\n\n" +
                    "Формат: объяснение + код (если нужен).";
        }

        return OllamaRequest.builder()
                .model(model)
                .prompt(request.getMessage())
                .system(systemPrompt)
                .stream(request.getStream() != null ? request.getStream() : false)
                .options(options)
                .build();
    }

    /**
     * Maps Ollama response to ChatResponse DTO.
     */
    private ChatResponse mapToResponse(OllamaResponse ollamaResponse, long startTime) {
        long processingTime = System.currentTimeMillis() - startTime;

        // Build metadata map
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("totalDuration", ollamaResponse.getTotalDuration());
        metadata.put("loadDuration", ollamaResponse.getLoadDuration());
        metadata.put("promptEvalCount", ollamaResponse.getPromptEvalCount());
        metadata.put("promptEvalDuration", ollamaResponse.getPromptEvalDuration());
        metadata.put("evalDuration", ollamaResponse.getEvalDuration());

        return ChatResponse.builder()
                .response(ollamaResponse.getResponse())
                .model(ollamaResponse.getModel())
                .timestamp(LocalDateTime.now())
                .processingTimeMs(processingTime)
                .tokensGenerated(ollamaResponse.getEvalCount())
                .done(ollamaResponse.getDone())
                .metadata(metadata)
                .build();
    }

    /**
     * Builds error response when API call fails.
     */
    private ChatResponse buildErrorResponse(Throwable error, long startTime) {
        long processingTime = System.currentTimeMillis() - startTime;

        return ChatResponse.builder()
                .response(null)
                .model(llmProperties.getModel())
                .timestamp(LocalDateTime.now())
                .processingTimeMs(processingTime)
                .tokensGenerated(0)
                .done(false)
                .error("Failed to get response from Ollama: " + error.getMessage())
                .build();
    }

}