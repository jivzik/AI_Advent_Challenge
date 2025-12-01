package de.jivz.ai_challenge.service;

import de.jivz.ai_challenge.dto.ChatRequest;
import de.jivz.ai_challenge.dto.ChatResponse;
import de.jivz.ai_challenge.dto.Message;
import de.jivz.ai_challenge.service.perplexity.PerplexityToolClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Agent service that handles chat requests.
 * Validates requests and delegates to the appropriate tool client.
 */
@Slf4j
@Service
public class AgentService {

    private final PerplexityToolClient perplexityToolClient;
    private final ConversationHistoryService historyService;

    public AgentService(PerplexityToolClient perplexityToolClient,
                       ConversationHistoryService historyService) {
        this.perplexityToolClient = perplexityToolClient;
        this.historyService = historyService;
    }

    /**
     * Handles a chat request and returns the response.
     *
     * @param request the chat request
     * @return the chat response
     * @throws IllegalArgumentException if the message is empty
     */
    public ChatResponse handle(ChatRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            log.warn("Received empty message from user: {}", request.getUserId());
            throw new IllegalArgumentException("Message cannot be empty");
        }

        String conversationId = request.getConversationId();
        log.debug("Processing request for user: {}, conversationId: {}",
                 request.getUserId(), conversationId);

        // 1. Load conversation history from storage
        List<Message> history = historyService.getHistory(conversationId);
        log.info("📚 Loaded {} previous messages for conversation: {}",
                history.size(), conversationId);

        // 2. Append new user message to history
        history.add(new Message("user", request.getMessage()));

        // 3. Send complete message history to Perplexity API
        String reply = perplexityToolClient.requestCompletion(history);

        // 4. Append assistant response to history
        history.add(new Message("assistant", reply));

        // 5. Save updated conversation history
        historyService.saveHistory(conversationId, history);
        log.info("💾 Saved conversation history ({} messages) for conversationId: {}",
                history.size(), conversationId);

        return new ChatResponse(
                reply,
                "PerplexityToolClient",
                Instant.now()
        );
    }
}
