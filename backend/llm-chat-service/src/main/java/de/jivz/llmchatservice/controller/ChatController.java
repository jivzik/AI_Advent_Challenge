package de.jivz.llmchatservice.controller;

import de.jivz.llmchatservice.config.LlmProperties;
import de.jivz.llmchatservice.dto.ChatRequest;
import de.jivz.llmchatservice.dto.ChatResponse;
import de.jivz.llmchatservice.service.LlmChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for chat API endpoints.
 * Provides endpoints for sending messages to local LLM and getting responses.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final LlmChatService llmChatService;
    private final LlmProperties llmProperties;

    /**
     * POST /api/chat - Send a message to the LLM and get a response.
     *
     * Request body:
     * {
     *   "message": "Hello, how are you?",
     *   "model": "llama2",              // optional
     *   "temperature": 0.7,             // optional
     *   "maxTokens": 2000,              // optional
     *   "systemPrompt": "You are...",   // optional
     *   "stream": false                 // optional
     * }
     *
     * @param request Chat request with message and optional parameters
     * @return Reactive response with LLM output and metadata
     */
    @PostMapping(value = "/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ChatResponse>> chat(@RequestBody ChatRequest request) {
        log.info("Received chat request from client");

        // Validate request
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return Mono.just(ResponseEntity
                    .badRequest()
                    .body(ChatResponse.builder()
                            .error("Message cannot be empty")
                            .done(false)
                            .build()));
        }

        // Process chat request
        return llmChatService.chat(request)
                .map(response -> {
                    if (response.getError() != null) {
                        log.error("Chat request failed: {}", response.getError());
                        return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(response);
                    }
                    return ResponseEntity.ok(response);
                })
                .doOnError(error -> log.error("Unexpected error in chat endpoint: {}", error.getMessage(), error))
                .onErrorResume(error -> Mono.just(ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ChatResponse.builder()
                                .error("Internal server error: " + error.getMessage())
                                .done(false)
                                .build())));
    }

    /**
     * GET /api/status - Check service health and configuration.
     *
     * @return Service status and configuration information
     */
    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "llm-chat-service");
        status.put("status", "UP");
        status.put("ollamaBaseUrl", llmProperties.getBaseUrl());
        status.put("defaultModel", llmProperties.getModel());
        status.put("defaultTemperature", llmProperties.getTemperature());
        status.put("defaultMaxTokens", llmProperties.getMaxTokens());
        status.put("timeoutSeconds", llmProperties.getTimeoutSeconds());

        log.debug("Status check requested");
        return ResponseEntity.ok(status);
    }

    /**
     * GET /api/models - Get available LLM models information.
     *
     * @return Information about configured model
     */
    @GetMapping(value = "/models", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> models() {
        Map<String, Object> modelInfo = new HashMap<>();
        modelInfo.put("currentModel", llmProperties.getModel());
        modelInfo.put("baseUrl", llmProperties.getBaseUrl());
        modelInfo.put("note", "To change model, use 'model' parameter in chat request or update OLLAMA_MODEL env variable");

        log.debug("Model info requested");
        return ResponseEntity.ok(modelInfo);
    }

}
