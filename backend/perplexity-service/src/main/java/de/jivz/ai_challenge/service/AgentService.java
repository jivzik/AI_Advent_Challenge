package de.jivz.ai_challenge.service;

import de.jivz.ai_challenge.dto.ChatRequest;
import de.jivz.ai_challenge.dto.ChatResponse;
import de.jivz.ai_challenge.dto.Message;
import de.jivz.ai_challenge.service.perplexity.PerplexityToolClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Agent service that orchestrates chat request handling.
 * Follows Single Responsibility Principle and delegates specific tasks to specialized services.
 *
 * Responsibilities:
 * - Validate requests
 * - Orchestrate the conversation flow
 * - Coordinate between history, parsing, and LLM services
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private static final String TOOL_NAME = "PerplexityToolClient";

    private final PerplexityToolClient perplexityToolClient;
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
        validateRequest(request);

        String conversationId = request.getConversationId();
        logRequestInfo(request, conversationId);

        // 1. Load conversation history
        List<Message> history = loadHistory(conversationId);

        // 2. Prepare history with user message (and JSON instruction if needed)
        historyManager.prepareHistory(history, request);

        // 3. Get response from LLM
        String rawReply = getLlmResponse(history);

        // 4. Parse response (if JSON mode enabled)
        String parsedReply = parseResponse(rawReply, request);

        // 5. Save to history
        saveToHistory(history, parsedReply, conversationId);

        return buildResponse(parsedReply);
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
     * Gets response from the LLM.
     *
     * @param history The conversation history
     * @return The raw response from LLM
     */
    private String getLlmResponse(List<Message> history) {
        String rawReply = perplexityToolClient.requestCompletion(history);
        log.info("🔍 Raw reply from Perplexity (first 200 chars): {}",
                rawReply.substring(0, Math.min(200, rawReply.length())));
        return rawReply;
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
     * Builds the chat response DTO.
     *
     * @param reply The parsed reply
     * @return The chat response
     */
    private ChatResponse buildResponse(String reply) {
        return new ChatResponse(reply, TOOL_NAME, Instant.now());
    }
}

