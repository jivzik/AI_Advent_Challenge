package de.jivz.ai_challenge.service;

import de.jivz.ai_challenge.dto.ChatRequest;
import de.jivz.ai_challenge.dto.ChatResponse;
import de.jivz.ai_challenge.dto.Message;
import de.jivz.ai_challenge.dto.ResponseMetrics;
import de.jivz.ai_challenge.service.perplexity.PerplexityToolClient;
import de.jivz.ai_challenge.service.openrouter.OpenRouterToolClient;
import de.jivz.ai_challenge.service.openrouter.model.OpenRouterResponseWithMetrics;
import de.jivz.ai_challenge.service.perplexity.model.PerplexityResponseWithMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Agent service that orchestrates chat request handling.
 * Follows Single Responsibility Principle and delegates specific tasks to specialized services.
 * Responsibilities:
 * - Validate requests
 * - Orchestrate the conversation flow
 * - Coordinate between history, parsing, and LLM services
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final PerplexityToolClient perplexityToolClient;
    private final OpenRouterToolClient openRouterToolClient;
    private final ConversationHistoryService historyService;
    private final MessageHistoryManager historyManager;
    private final JsonResponseParser jsonResponseParser;

    /**
     * Handles a chat request and returns the response.
     *
     * @param request the chat request
     * @return the chat response
     * @throws IllegalArgumentException if the message is empty
     */
    public ChatResponse handle(ChatRequest request) {
        long requestStartTime = System.nanoTime();
        validateRequest(request);

        String conversationId = request.getConversationId();
        logRequestInfo(request, conversationId);

        // 1. Load conversation history
        List<Message> history = loadHistory(conversationId);

        // 2. Prepare history with user message (and JSON instruction if needed)
        historyManager.prepareHistory(history, request);

        // 3. Get response from LLM with metrics
        ChatResponse llmResponse = getLlmResponseWithMetrics(history, request.getTemperature(), request.getProvider(), request.getModel());
        String rawReply = llmResponse.getReply();
        ResponseMetrics metrics = llmResponse.getMetrics();

        // 4. Parse response (if JSON mode enabled)
        String parsedReply = parseResponse(rawReply, request);

        // 5. Save to history
        saveToHistory(history, parsedReply, conversationId);

        long requestEndTime = System.nanoTime();
        long totalRequestTimeMs = (requestEndTime - requestStartTime) / 1_000_000;
        log.info("⏱️ Total request processing time: {} ms", totalRequestTimeMs);

        return buildResponse(parsedReply, request.getProvider(), metrics);
    }

    /**
     * Validates the incoming request.
     *
     * @param request The chat request to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateRequest(ChatRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            log.warn("Received empty message from user: {}", request.getUserId());
            throw new IllegalArgumentException("Message cannot be empty");
        }
    }

    /**
     * Logs request information for debugging.
     *
     * @param request        The chat request
     * @param conversationId The conversation ID
     */
    private void logRequestInfo(ChatRequest request, String conversationId) {
        log.debug("Processing request for user: {}, conversationId: {}, jsonMode: {}",
                request.getUserId(), conversationId, request.isJsonMode());
    }

    /**
     * Loads conversation history from storage.
     *
     * @param conversationId The conversation identifier
     * @return The conversation history
     */
    private List<Message> loadHistory(String conversationId) {
        List<Message> history = historyService.getHistory(conversationId);
        log.info("📚 Loaded {} previous messages for conversation: {}",
                history.size(), conversationId);
        return history;
    }

    /**
     * Gets response from the LLM with metrics.
     *
     * @param history The conversation history
     * @param temperature The temperature parameter for response generation
     * @param provider The AI provider to use (perplexity or openrouter)
     * @param model The specific model to use (optional)
     * @return The response with metrics
     */
    private ChatResponse getLlmResponseWithMetrics(List<Message> history, Double temperature, String provider, String model) {
        if ("openrouter".equalsIgnoreCase(provider)) {
            OpenRouterResponseWithMetrics response = openRouterToolClient.requestCompletionWithMetrics(history, temperature, model);
            String rawReply = response.getReply();
            log.info("🔍 Raw reply from OpenRouter with model {} (first 200 chars): {}",
                    model, rawReply.substring(0, Math.min(200, rawReply.length())));

            ResponseMetrics metrics = ResponseMetrics.builder()
                    .inputTokens(response.getInputTokens())
                    .outputTokens(response.getOutputTokens())
                    .totalTokens(response.getTotalTokens())
                    .cost(response.getCost())
                    .responseTimeMs(response.getResponseTimeMs())
                    .model(response.getModel())
                    .provider("openrouter")
                    .build();

            return ChatResponse.builder().reply(rawReply).metrics(metrics).build();
        } else {
            PerplexityResponseWithMetrics response = perplexityToolClient.requestCompletionWithMetrics(history, temperature, null);
            String rawReply = response.getReply();
            log.info("🔍 Raw reply from Perplexity (first 200 chars): {}",
                    rawReply.substring(0, Math.min(200, rawReply.length())));

            ResponseMetrics metrics = ResponseMetrics.builder()
                    .inputTokens(response.getInputTokens())
                    .outputTokens(response.getOutputTokens())
                    .totalTokens(response.getTotalTokens())
                    .cost(response.getCost())
                    .responseTimeMs(response.getResponseTimeMs())
                    .model(response.getModel())
                    .provider("perplexity")
                    .build();

            return ChatResponse.builder().reply(rawReply).metrics(metrics).build();
        }
    }

    /**
     * Parses the response based on request mode.
     *
     * @param rawReply The raw response from LLM
     * @param request  The chat request
     * @return Parsed response
     */
    private String parseResponse(String rawReply, ChatRequest request) {
        if (!request.isJsonMode()) {
            return rawReply;
        }

        String parsedReply = jsonResponseParser.parse(rawReply, request);
        log.info("📦 Parsed reply (first 100 chars): {}",
                parsedReply.substring(0, Math.min(100, parsedReply.length())));
        return parsedReply;
    }

    /**
     * Saves the conversation to history.
     *
     * @param history        The conversation history
     * @param reply          The assistant's reply
     * @param conversationId The conversation identifier
     */
    private void saveToHistory(List<Message> history, String reply, String conversationId) {
        historyManager.addAssistantResponse(history, reply);
        historyService.saveHistory(conversationId, history);
        log.info("💾 Saved conversation history ({} messages) for conversationId: {}",
                history.size(), conversationId);
    }

    /**
     * Builds the chat response DTO with metrics.
     *
     * @param reply The parsed reply
     * @param provider The AI provider used
     * @param metrics The response metrics
     * @return The chat response
     */
    private ChatResponse buildResponse(String reply, String provider, ResponseMetrics metrics) {
        String toolName = "openrouter".equalsIgnoreCase(provider) ? "OpenRouterToolClient" : "PerplexityToolClient";
        return new ChatResponse(reply, toolName, Instant.now(), metrics);
    }
}
