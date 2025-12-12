package de.jivz.ai_challenge.controller;

import de.jivz.ai_challenge.dto.ConversationSummaryDTO;
import de.jivz.ai_challenge.dto.Message;
import de.jivz.ai_challenge.service.MemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller for long-term memory management.
 *
 * Provides endpoints for:
 * - Retrieving conversation history
 * - Managing conversations (list, delete)
 * - Viewing statistics and analytics
 * - Exporting conversations
 *
 * All endpoints support CORS for frontend integration.
 */
@Slf4j
@RestController
@RequestMapping("/api/memory")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MemoryController {

    private final MemoryService memoryService;

    /**
     * GET /api/memory/conversations/{userId}
     *
     * Retrieves list of all conversations for a specific user.
     * Returns conversation IDs sorted by most recent first.
     *
     * @param userId user identifier
     * @return list of conversation IDs
     */
    @GetMapping("/conversations/{userId}")
    public ResponseEntity<Map<String, Object>> getUserConversations(
            @PathVariable String userId
    ) {
        log.info("📋 GET /api/memory/conversations/{}", userId);

        try {
            List<String> conversationIds = memoryService.getConversationIds(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("conversationCount", conversationIds.size());
            response.put("conversationIds", conversationIds);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Failed to get conversations for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/memory/conversation/{conversationId}
     *
     * Retrieves full conversation history.
     * Returns all messages in chronological order.
     *
     * @param conversationId conversation identifier
     * @return full conversation history with messages
     */
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<Map<String, Object>> getConversationHistory(
            @PathVariable String conversationId
    ) {
        log.info("📖 GET /api/memory/conversation/{}", conversationId);

        try {
            List<Message> messages = memoryService.getFullHistory(conversationId);

            Map<String, Object> response = new HashMap<>();
            response.put("conversationId", conversationId);
            response.put("messageCount", messages.size());
            response.put("messages", messages);

            if (messages.isEmpty()) {
                response.put("warning", "No messages found for this conversation");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Failed to get conversation history: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/memory/conversation/{conversationId}/recent
     *
     * Retrieves recent N messages from a conversation.
     * Useful for loading partial history.
     *
     * @param conversationId conversation identifier
     * @param limit maximum number of messages (default: 10)
     * @return recent messages
     */
    @GetMapping("/conversation/{conversationId}/recent")
    public ResponseEntity<Map<String, Object>> getRecentMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("📖 GET /api/memory/conversation/{}/recent?limit={}", conversationId, limit);

        try {
            List<Message> messages = memoryService.getRecentMessages(conversationId, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("conversationId", conversationId);
            response.put("messageCount", messages.size());
            response.put("limit", limit);
            response.put("messages", messages);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Failed to get recent messages: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/memory/conversation/{conversationId}/stats
     *
     * Retrieves statistics for a conversation.
     * Includes message count, token usage, cost, duration, etc.
     *
     * @param conversationId conversation identifier
     * @return conversation statistics
     */
    @GetMapping("/conversation/{conversationId}/stats")
    public ResponseEntity<Map<String, Object>> getConversationStats(
            @PathVariable String conversationId
    ) {
        log.info("📊 GET /api/memory/conversation/{}/stats", conversationId);

        try {
            Map<String, Object> stats = memoryService.getConversationStats(conversationId);

            if (stats.containsKey("error")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(stats);
            }

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("❌ Failed to get conversation stats: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/memory/conversation/{conversationId}/export
     *
     * Exports full conversation to JSON format.
     * Includes all messages, metadata, and statistics.
     *
     * @param conversationId conversation identifier
     * @return JSON export of the conversation
     */
    @GetMapping(value = "/conversation/{conversationId}/export",
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> exportConversation(
            @PathVariable String conversationId
    ) {
        log.info("📤 GET /api/memory/conversation/{}/export", conversationId);

        try {
            String json = memoryService.exportToJson(conversationId);

            if (json.contains("\"error\"")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(json);
            }

            return ResponseEntity.ok()
                    .header("Content-Disposition",
                            "attachment; filename=\"conversation_" + conversationId + ".json\"")
                    .body(json);

        } catch (Exception e) {
            log.error("❌ Failed to export conversation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * DELETE /api/memory/conversation/{conversationId}
     *
     * Deletes a conversation and all its messages.
     * This operation is permanent and cannot be undone!
     *
     * @param conversationId conversation identifier
     * @return deletion confirmation
     */
    @DeleteMapping("/conversation/{conversationId}")
    public ResponseEntity<Map<String, Object>> deleteConversation(
            @PathVariable String conversationId
    ) {
        log.info("🗑️ DELETE /api/memory/conversation/{}", conversationId);

        try {
            int deletedCount = memoryService.deleteConversation(conversationId);

            Map<String, Object> response = new HashMap<>();
            response.put("conversationId", conversationId);
            response.put("deletedMessages", deletedCount);
            response.put("status", "deleted");

            if (deletedCount == 0) {
                response.put("warning", "No messages found to delete");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Failed to delete conversation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/memory/conversation/{conversationId}/exists
     *
     * Checks if a conversation exists in the database.
     *
     * @param conversationId conversation identifier
     * @return existence check result
     */
    @GetMapping("/conversation/{conversationId}/exists")
    public ResponseEntity<Map<String, Object>> checkConversationExists(
            @PathVariable String conversationId
    ) {
        log.debug("🔍 GET /api/memory/conversation/{}/exists", conversationId);

        try {
            boolean exists = memoryService.conversationExists(conversationId);

            Map<String, Object> response = new HashMap<>();
            response.put("conversationId", conversationId);
            response.put("exists", exists);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Failed to check conversation existence: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/memory/stats
     *
     * Retrieves global statistics across all conversations.
     * Includes total conversations, messages, tokens, cost, etc.
     *
     * @return global memory statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getGlobalStats() {
        log.info("🌍 GET /api/memory/stats");

        try {
            Map<String, Object> stats = memoryService.getGlobalStats();

            if (stats.containsKey("error")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(stats);
            }

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("❌ Failed to get global stats: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/memory/conversations
     *
     * Retrieves list of all conversations with metadata (summaries).
     * Returns brief information for each conversation sorted by most recent first.
     *
     * @return list of conversation summaries
     */
    @GetMapping("/conversations")
    public ResponseEntity<Map<String, Object>> getAllConversationSummaries() {
        log.info("📋 GET /api/memory/conversations");

        try {
            List<ConversationSummaryDTO> summaries = memoryService.getConversationSummaries();

            Map<String, Object> response = new HashMap<>();
            response.put("totalConversations", summaries.size());
            response.put("conversations", summaries);
            response.put("timestamp", new Date().toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Failed to get conversation summaries: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/memory/user/{userId}/conversations
     *
     * Retrieves list of conversations for a specific user with metadata.
     * Returns brief information for each conversation sorted by most recent first.
     *
     * @param userId user identifier
     * @return list of conversation summaries for the user
     */
    @GetMapping("/user/{userId}/conversations")
    public ResponseEntity<Map<String, Object>> getUserConversationSummaries(
            @PathVariable String userId
    ) {
        log.info("📋 GET /api/memory/user/{}/conversations", userId);

        try {
            List<ConversationSummaryDTO> summaries = memoryService.getConversationSummariesForUser(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("totalConversations", summaries.size());
            response.put("conversations", summaries);
            response.put("timestamp", new Date().toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Failed to get conversation summaries for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/memory/health
     *
     * Health check endpoint for memory service.
     * Tests database connectivity.
     *
     * @return health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.debug("❤️ GET /api/memory/health");

        try {
            // Try to get global stats to test DB connection
            Map<String, Object> stats = memoryService.getGlobalStats();

            Map<String, Object> health = new HashMap<>();
            health.put("status", "healthy");
            health.put("database", "connected");
            health.put("timestamp", new Date().toString());
            health.put("totalConversations", stats.get("totalConversations"));
            health.put("totalMessages", stats.get("totalMessages"));

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            log.error("❌ Health check failed: {}", e.getMessage());

            Map<String, Object> health = new HashMap<>();
            health.put("status", "unhealthy");
            health.put("database", "disconnected");
            health.put("error", e.getMessage());
            health.put("timestamp", new Date().toString());

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }
}

