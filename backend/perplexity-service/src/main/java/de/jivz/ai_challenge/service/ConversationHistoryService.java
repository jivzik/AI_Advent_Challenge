package de.jivz.ai_challenge.service;

import de.jivz.ai_challenge.dto.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing conversation history.
 *
 * ⭐ UPDATED: Now integrates with PostgreSQL for long-term storage.
 *
 * Architecture:
 * - PostgreSQL: Source of truth, stores ALL messages permanently
 * - ConcurrentHashMap: Fast cache for active conversations
 *
 * Flow:
 * 1. getHistory() first checks PostgreSQL
 * 2. If found in DB, loads and caches in RAM
 * 3. If not in DB, returns empty (will be created on first message)
 * 4. saveHistory() saves to both RAM cache and PostgreSQL (via MemoryService)
 */
@Slf4j
@Service
public class ConversationHistoryService {

    // In-Memory cache: conversationId -> List of Messages
    // Used for fast access during active conversations
    private final Map<String, List<Message>> conversations = new ConcurrentHashMap<>();

    // PostgreSQL persistence layer
    private MemoryService memoryService;

    /**
     * Constructor with lazy loading to avoid circular dependency.
     * MemoryService depends on this service indirectly, so we use @Lazy.
     */
    @Autowired
    public ConversationHistoryService(@Lazy MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /**
     * ⭐ UPDATED: Retrieves conversation history with PostgreSQL integration.
     *
     * Load strategy:
     * 1. Check RAM cache first (fastest)
     * 2. If not in cache, load from PostgreSQL
     * 3. Cache loaded history in RAM for subsequent requests
     * 4. If not in DB either, return empty list
     *
     * This ensures conversations persist across server restarts.
     *
     * @param conversationId the conversation identifier
     * @return list of messages in the conversation
     */
    public List<Message> getHistory(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            log.debug("No conversationId provided, returning empty history");
            return new ArrayList<>();
        }

        // 1. Check RAM cache first
        List<Message> cached = conversations.get(conversationId);
        if (cached != null) {
            log.debug("📦 Retrieved {} messages from RAM cache for: {}", cached.size(), conversationId);
            return new ArrayList<>(cached); // Return copy to prevent external modification
        }

        // 2. Try loading from PostgreSQL
        try {
            List<Message> fromDb = memoryService.getFullHistory(conversationId);
            if (!fromDb.isEmpty()) {
                // Cache in RAM for fast access
                conversations.put(conversationId, new ArrayList<>(fromDb));
                log.info("💾 Loaded {} messages from PostgreSQL and cached for: {}",
                        fromDb.size(), conversationId);
                return new ArrayList<>(fromDb);
            }
        } catch (Exception e) {
            log.warn("⚠️ Failed to load from PostgreSQL, falling back to RAM-only: {}", e.getMessage());
        }

        // 3. Not found anywhere - return empty (will be created on first message)
        log.debug("No history found for conversationId: {}", conversationId);
        return new ArrayList<>();
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
     * ⭐ NOTE: This only updates RAM cache.
     * PostgreSQL persistence is handled by MemoryService.saveMessage()
     * called from AgentService after each message.
     *
     * This separation ensures:
     * - Individual messages are saved to DB with full metadata
     * - RAM cache stays synchronized for fast access
     *
     * @param conversationId the conversation identifier
     * @param history the complete list of messages
     */
    public void saveHistory(String conversationId, List<Message> history) {
        if (conversationId == null || conversationId.isBlank()) {
            log.warn("Cannot save history: conversationId is null or empty");
            return;
        }

        // Update RAM cache
        conversations.put(conversationId, new ArrayList<>(history));
        log.debug("💾 Updated RAM cache: {} messages for conversationId: {}",
                history.size(), conversationId);

        // Note: PostgreSQL persistence happens in AgentService.handle()
        // via memoryService.saveMessage() for each individual message
    }

    /**
     * ⭐ UPDATED: Clears history from both RAM cache and PostgreSQL.
     *
     * @param conversationId the conversation identifier
     */
    public void clearHistory(String conversationId) {
        if (conversationId != null && !conversationId.isBlank()) {
            // Clear from RAM cache
            conversations.remove(conversationId);

            // Clear from PostgreSQL
            try {
                int deletedCount = memoryService.deleteConversation(conversationId);
                log.info("🗑️ Cleared history for conversationId: {} ({} messages from DB, RAM cache cleared)",
                        conversationId, deletedCount);
            } catch (Exception e) {
                log.warn("⚠️ Failed to clear from PostgreSQL: {}", e.getMessage());
                log.info("🗑️ Cleared RAM cache for conversationId: {}", conversationId);
            }
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

