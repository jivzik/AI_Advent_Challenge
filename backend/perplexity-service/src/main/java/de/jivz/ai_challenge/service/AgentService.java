package de.jivz.ai_challenge.service;

import de.jivz.ai_challenge.dto.ChatRequest;
import de.jivz.ai_challenge.dto.ChatResponse;
import de.jivz.ai_challenge.service.perplexity.PerplexityToolClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Agent service that handles chat requests.
 * Validates requests and delegates to the appropriate tool client.
 */
@Slf4j
@Service
public class AgentService {

    private final PerplexityToolClient perplexityToolClient;

    public AgentService(PerplexityToolClient perplexityToolClient) {
        this.perplexityToolClient = perplexityToolClient;
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

        log.debug("Processing request for user: {}", request.getUserId());
        String reply = perplexityToolClient.requestCompletion(request.getMessage());

        return new ChatResponse(
                reply,
                "PerplexityToolClient",
                Instant.now()
        );
    }
}
