package de.jivz.ai_challenge.service;

import de.jivz.ai_challenge.dto.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing conversation history.
 * Uses in-memory storage (Map) for demo purposes.
 */
@Slf4j
@Service
public class ConversationHistoryService {

    // In-Memory-Map: conversationId -> List of Messages
    private final Map<String, List<Message>> conversations = new ConcurrentHashMap<>();

    /**
     * Retrieves the conversation history for a given conversationId.
     * If no history exists, returns an empty list.
     *
     * @param conversationId the conversation identifier
     * @return list of messages in the conversation
     */
    public List<Message> getHistory(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            log.debug("No conversationId provided, returning empty history");
            return new ArrayList<>();
        }

        List<Message> history = conversations.get(conversationId);
        if (history == null) {
            log.debug("No history found for conversationId: {}", conversationId);
            return new ArrayList<>();
        }

        log.debug("Retrieved {} messages for conversationId: {}", history.size(), conversationId);
        return new ArrayList<>(history); // Return a copy to prevent external modification
    }

    /**
     * Adds a message to the conversation history.
     *
     * @param conversationId the conversation identifier
     * @param role the role (user or assistant)
     * @param content the message content
     */
    public void addMessage(String conversationId, String role, String content) {
        if (conversationId == null || conversationId.isBlank()) {
            log.warn("Cannot add message: conversationId is null or empty");
            return;
        }

        conversations.computeIfAbsent(conversationId, k -> new ArrayList<>())
                .add(new Message(role, content));

        log.debug("Added {} message to conversationId: {}", role, conversationId);
    }

    /**
     * Saves the complete conversation history.
     *
     * @param conversationId the conversation identifier
     * @param history the complete list of messages
     */
    public void saveHistory(String conversationId, List<Message> history) {
        if (conversationId == null || conversationId.isBlank()) {
            log.warn("Cannot save history: conversationId is null or empty");
            return;
        }

        conversations.put(conversationId, new ArrayList<>(history));
        log.debug("Saved {} messages for conversationId: {}", history.size(), conversationId);
    }

    /**
     * Clears the history for a specific conversation.
     *
     * @param conversationId the conversation identifier
     */
    public void clearHistory(String conversationId) {
        if (conversationId != null && !conversationId.isBlank()) {
            conversations.remove(conversationId);
            log.info("Cleared history for conversationId: {}", conversationId);
        }
    }

    /**
     * Gets the total number of active conversations.
     *
     * @return number of conversations
     */
    public int getConversationCount() {
        return conversations.size();
    }
}

