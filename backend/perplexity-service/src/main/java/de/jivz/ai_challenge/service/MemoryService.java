package de.jivz.ai_challenge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.jivz.ai_challenge.dto.ConversationSummaryDTO;
import de.jivz.ai_challenge.dto.Message;
import de.jivz.ai_challenge.dto.ResponseMetrics;
import de.jivz.ai_challenge.entity.MemoryEntry;
import de.jivz.ai_challenge.repository.MemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing long-term memory storage in PostgreSQL.
 *
 * Key responsibilities:
 * - Save all messages to database (full history preservation)
 * - Retrieve conversation history
 * - Provide statistics and analytics
 * - Export conversations to JSON
 * - Handle database errors gracefully (fallback to RAM)
 *
 * IMPORTANT:
 * - Stores ALL messages in PostgreSQL (complete history)
 * - DialogCompressionService handles compression for LLM context
 * - Database is source of truth, RAM is cache
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryService {

    private final MemoryRepository memoryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Saves a message to the database with full metrics.
     * This is the primary method for persisting conversation data.
     *
     * @param conversationId conversation identifier
     * @param userId user identifier (nullable)
     * @param role message role ("user", "assistant", "system")
     * @param content message content
     * @param model AI model name (nullable, for assistant messages)
     * @param metrics response metrics (nullable)
     * @return saved MemoryEntry
     */
    @Transactional
    public MemoryEntry saveMessage(
            String conversationId,
            String userId,
            String role,
            String content,
            String model,
            ResponseMetrics metrics
    ) {
        try {
            MemoryEntry.MemoryEntryBuilder builder = MemoryEntry.builder()
                    .conversationId(conversationId)
                    .userId(userId)
                    .role(role)
                    .content(content)
                    .timestamp(LocalDateTime.now())
                    .model(model)
                    .isCompressed(false);

            // Add metrics if available
            if (metrics != null) {
                builder.inputTokens(metrics.getInputTokens())
                        .outputTokens(metrics.getOutputTokens())
                        .totalTokens(metrics.getTotalTokens())
                        .responseTimeMs(metrics.getResponseTimeMs());

                // Convert cost to BigDecimal if present
                if (metrics.getCost() != null) {
                    builder.cost(BigDecimal.valueOf(metrics.getCost()));
                }
            }

            MemoryEntry entry = builder.build();
            MemoryEntry saved = memoryRepository.save(entry);

            log.debug("💾 Saved {} message to DB: conversationId={}, id={}, tokens={}",
                    role, conversationId, saved.getId(),
                    metrics != null ? metrics.getTotalTokens() : "N/A");

            return saved;

        } catch (Exception e) {
            log.error("❌ Failed to save message to database: {}", e.getMessage());
            // Don't throw - allow application to continue with RAM-only mode
            return null;
        }
    }

    /**
     * Saves a simple message without metrics.
     * Convenience method for user messages.
     *
     * @param conversationId conversation identifier
     * @param role message role
     * @param content message content
     * @return saved MemoryEntry
     */
    @Transactional
    public MemoryEntry saveMessage(String conversationId, String role, String content) {
        return saveMessage(conversationId, null, role, content, null, null);
    }

    /**
     * Retrieves full conversation history from database.
     * Returns all messages in chronological order.
     *
     * @param conversationId conversation identifier
     * @return list of messages (converted from MemoryEntry)
     */
    @Transactional(readOnly = true)
    public List<Message> getFullHistory(String conversationId) {
        try {
            List<MemoryEntry> entries = memoryRepository
                    .findByConversationIdOrderByTimestampAsc(conversationId);

            log.debug("📖 Loaded {} messages from DB for conversation: {}",
                    entries.size(), conversationId);

            return entries.stream()
                    .map(this::toMessage)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Failed to load history from database: {}", e.getMessage());
            return new ArrayList<>(); // Return empty list on error
        }
    }

    /**
     * Retrieves only original (non-compressed) messages.
     * Useful when you need raw history without summaries.
     *
     * @param conversationId conversation identifier
     * @return list of original messages
     */
    @Transactional(readOnly = true)
    public List<Message> getOriginalHistory(String conversationId) {
        try {
            List<MemoryEntry> entries = memoryRepository
                    .findByConversationIdAndIsCompressedFalseOrderByTimestampAsc(conversationId);

            return entries.stream()
                    .map(this::toMessage)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Failed to load original history: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Gets list of all conversation IDs for a specific user.
     *
     * @param userId user identifier
     * @return list of conversation IDs (newest first)
     */
    @Transactional(readOnly = true)
    public List<String> getConversationIds(String userId) {
        try {
            List<String> conversationIds = memoryRepository
                    .findDistinctConversationIdsByUserId(userId);

            log.debug("📋 Found {} conversations for user: {}", conversationIds.size(), userId);
            return conversationIds;

        } catch (Exception e) {
            log.error("❌ Failed to get conversation IDs: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Deletes all messages for a conversation.
     * Permanent operation - cannot be undone!
     *
     * @param conversationId conversation identifier
     * @return number of deleted messages
     */
    @Transactional
    public int deleteConversation(String conversationId) {
        try {
            int deletedCount = memoryRepository.deleteByConversationId(conversationId);
            log.info("🗑️ Deleted {} messages for conversation: {}", deletedCount, conversationId);
            return deletedCount;

        } catch (Exception e) {
            log.error("❌ Failed to delete conversation: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Gets comprehensive statistics for a conversation.
     *
     * @param conversationId conversation identifier
     * @return map with statistics (messageCount, totalTokens, totalCost, etc.)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getConversationStats(String conversationId) {
        Map<String, Object> stats = new HashMap<>();

        try {
            Object[] result = memoryRepository.getConversationStats(conversationId);

            long totalTokens = result[0] != null ? ((Number) result[0]).longValue() : 0;
            double totalCost = result[1] != null ? ((Number) result[1]).doubleValue() : 0.0;
            long messageCount = result[2] != null ? ((Number) result[2]).longValue() : 0;

            stats.put("conversationId", conversationId);
            stats.put("messageCount", messageCount);
            stats.put("totalTokens", totalTokens);
            stats.put("totalCost", totalCost);
            stats.put("averageTokensPerMessage", messageCount > 0 ? totalTokens / messageCount : 0);

            // Get first and last message timestamps
            List<MemoryEntry> entries = memoryRepository
                    .findByConversationIdOrderByTimestampAsc(conversationId);

            if (!entries.isEmpty()) {
                stats.put("firstMessageAt", entries.getFirst().getTimestamp().toString());
                stats.put("lastMessageAt", entries.getLast().getTimestamp().toString());
                stats.put("duration", calculateDuration(entries.get(0).getTimestamp(),
                        entries.get(entries.size() - 1).getTimestamp()));
            }

            log.debug("📊 Stats for {}: {} messages, {} tokens, ${} cost",
                    conversationId, messageCount, totalTokens, totalCost);

        } catch (Exception e) {
            log.error("❌ Failed to get conversation stats: {}", e.getMessage());
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    /**
     * Exports full conversation history to JSON format.
     * Includes all messages and metadata.
     *
     * @param conversationId conversation identifier
     * @return JSON string representation of the conversation
     */
    @Transactional(readOnly = true)
    public String exportToJson(String conversationId) {
        try {
            List<MemoryEntry> entries = memoryRepository
                    .findByConversationIdOrderByTimestampAsc(conversationId);

            Map<String, Object> export = new HashMap<>();
            export.put("conversationId", conversationId);
            export.put("exportedAt", Instant.now().toString());
            export.put("messageCount", entries.size());
            export.put("messages", entries);

            // Add statistics
            export.put("statistics", getConversationStats(conversationId));

            String json = objectMapper.writeValueAsString(export);
            log.info("📤 Exported conversation {} to JSON ({} bytes)", conversationId, json.length());

            return json;

        } catch (Exception e) {
            log.error("❌ Failed to export conversation: {}", e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * Checks if a conversation exists in the database.
     *
     * @param conversationId conversation identifier
     * @return true if conversation has messages
     */
    @Transactional(readOnly = true)
    public boolean conversationExists(String conversationId) {
        try {
            return memoryRepository.existsByConversationId(conversationId);
        } catch (Exception e) {
            log.error("❌ Failed to check conversation existence: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Gets the most recent N messages for a conversation.
     * Useful for loading recent context without full history.
     *
     * @param conversationId conversation identifier
     * @param limit maximum number of messages to return
     * @return list of recent messages (oldest first)
     */
    @Transactional(readOnly = true)
    public List<Message> getRecentMessages(String conversationId, int limit) {
        try {
            List<MemoryEntry> entries = memoryRepository
                    .findByConversationIdOrderByTimestampDesc(
                            conversationId,
                            PageRequest.of(0, limit)
                    );

            // Reverse to get chronological order (oldest first)
            Collections.reverse(entries);

            return entries.stream()
                    .map(this::toMessage)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Failed to get recent messages: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Gets global statistics across all conversations.
     *
     * @return map with global statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getGlobalStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            stats.put("totalConversations", memoryRepository.countDistinctConversations());
            stats.put("totalMessages", memoryRepository.countAllMessages());
            stats.put("totalTokens", memoryRepository.sumTotalTokens());
            stats.put("totalCost", memoryRepository.sumTotalCost());

            log.debug("🌍 Global stats: {} conversations, {} messages",
                    stats.get("totalConversations"), stats.get("totalMessages"));

        } catch (Exception e) {
            log.error("❌ Failed to get global stats: {}", e.getMessage());
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    /**
     * Batch save multiple messages (more efficient for bulk operations).
     *
     * @param entries list of memory entries to save
     * @return list of saved entries
     */
    @Transactional
    public List<MemoryEntry> saveAll(List<MemoryEntry> entries) {
        try {
            List<MemoryEntry> saved = memoryRepository.saveAll(entries);
            log.info("💾 Batch saved {} messages", saved.size());
            return saved;

        } catch (Exception e) {
            log.error("❌ Failed to batch save messages: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Gets summaries of all conversations in the system.
     * Returns conversation metadata: ID, first message, last message time, message count, compression status.
     * Sorted by most recent first.
     *
     * @return list of conversation summaries sorted by last message time (descending)
     */
    @Transactional(readOnly = true)
    public List<ConversationSummaryDTO> getConversationSummaries() {
        try {
            List<String> conversationIds = memoryRepository.findAllConversationIds();
            List<ConversationSummaryDTO> summaries = new ArrayList<>();

            for (String conversationId : conversationIds) {
                ConversationSummaryDTO summary = buildConversationSummary(conversationId);
                if (summary != null) {
                    summaries.add(summary);
                }
            }

            log.info("📋 Built summaries for {} conversations", summaries.size());
            return summaries;

        } catch (Exception e) {
            log.error("❌ Failed to get conversation summaries: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Gets summaries of all conversations for a specific user.
     * Similar to getConversationSummaries but filtered by userId.
     * Sorted by most recent first.
     *
     * @param userId user identifier
     * @return list of conversation summaries for the user sorted by last message time (descending)
     */
    @Transactional(readOnly = true)
    public List<ConversationSummaryDTO> getConversationSummariesForUser(String userId) {
        try {
            List<String> conversationIds = memoryRepository.findDistinctConversationIdsByUserId(userId);
            List<ConversationSummaryDTO> summaries = new ArrayList<>();

            for (String conversationId : conversationIds) {
                ConversationSummaryDTO summary = buildConversationSummary(conversationId, userId);
                if (summary != null) {
                    summaries.add(summary);
                }
            }

            log.info("📋 Built {} conversation summaries for user: {}", summaries.size(), userId);
            return summaries;

        } catch (Exception e) {
            log.error("❌ Failed to get conversation summaries for user {}: {}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Helper method to build a single conversation summary DTO.
     * Collects all necessary metadata for a conversation.
     *
     * @param conversationId conversation identifier
     * @return conversation summary or null if conversation not found
     */
    private ConversationSummaryDTO buildConversationSummary(String conversationId) {
        return buildConversationSummary(conversationId, null);
    }

    /**
     * Helper method to build a single conversation summary DTO with optional userId.
     *
     * @param conversationId conversation identifier
     * @param userId optional user identifier
     * @return conversation summary or null if conversation not found
     */
    private ConversationSummaryDTO buildConversationSummary(String conversationId, String userId) {
        try {
            // Get first message for preview
            MemoryEntry firstEntry = memoryRepository.findFirstByConversationIdOrderByTimestampAsc(conversationId);
            if (firstEntry == null) {
                return null;
            }

            // Get last message for timestamp
            MemoryEntry lastEntry = memoryRepository.findFirstByConversationIdOrderByTimestampDesc(conversationId);

            // Get message count
            long messageCount = memoryRepository.countByConversationId(conversationId);

            // Get compressed message count
            long compressedCount = memoryRepository.countByConversationIdAndIsCompressedTrue(conversationId);

            // Extract first 50 characters for preview
            String firstMessage = firstEntry.getContent();
            if (firstMessage.length() > 50) {
                firstMessage = firstMessage.substring(0, 50) + "...";
            }

            return ConversationSummaryDTO.builder()
                    .conversationId(conversationId)
                    .firstMessage(firstMessage)
                    .lastMessageTime(lastEntry != null ? lastEntry.getTimestamp() : firstEntry.getTimestamp())
                    .messageCount(messageCount)
                    .compressedMessageCount(compressedCount)
                    .hasCompression(compressedCount > 0)
                    .userId(userId != null ? userId : firstEntry.getUserId())
                    .build();

        } catch (Exception e) {
            log.warn("⚠️ Failed to build summary for conversation {}: {}", conversationId, e.getMessage());
            return null;
        }
    }

    // ========== HELPER METHODS ==========

    /**
     * Converts MemoryEntry to Message DTO.
     */
    private Message toMessage(MemoryEntry entry) {
        return new Message(entry.getRole(), entry.getContent());
    }

    /**
     * Calculates human-readable duration between two timestamps.
     */
    private String calculateDuration(LocalDateTime start, LocalDateTime end) {
        long seconds = end.toEpochSecond(ZoneOffset.UTC) - start.toEpochSecond(ZoneOffset.UTC);

        if (seconds < 60) {
            return seconds + " seconds";
        } else if (seconds < 3600) {
            return (seconds / 60) + " minutes";
        } else if (seconds < 86400) {
            return (seconds / 3600) + " hours";
        } else {
            return (seconds / 86400) + " days";
        }
    }

    // ========== ⭐ NEW SUMMARY METHODS ==========

    /**
     * ⭐ Saves a summary to the database.
     * Called by DialogCompressionService after creating a summary.
     *
     * This prevents re-creating the same summary multiple times,
     * saving tokens and reducing API calls.
     *
     * @param conversationId conversation identifier
     * @param summaryText the summary content
     * @param messagesCount number of messages that were compressed
     * @param timestamp when the summary was created
     * @return saved MemoryEntry
     */
    @Transactional
    public MemoryEntry saveSummary(String conversationId, String summaryText,
                                    int messagesCount, LocalDateTime timestamp) {
        try {
            MemoryEntry summaryEntry = MemoryEntry.builder()
                    .conversationId(conversationId)
                    .role("system")
                    .content(summaryText)
                    .timestamp(timestamp)
                    .isCompressed(true)
                    .compressedMessagesCount(messagesCount)
                    .compressionTimestamp(timestamp)
                    .build();

            MemoryEntry saved = memoryRepository.save(summaryEntry);
            log.info("💾 Summary saved to database for conversation: {} ({} messages compressed)",
                    conversationId, messagesCount);
            return saved;

        } catch (Exception e) {
            log.error("❌ Failed to save summary: {}", e.getMessage());
            return null;
        }
    }

    /**
     * ⭐ Retrieves the last (most recent) summary for a conversation.
     * Returns Optional to handle case when no summary exists.
     *
     * @param conversationId conversation identifier
     * @return Optional containing the summary content, or empty if no summary exists
     */
    @Transactional(readOnly = true)
    public java.util.Optional<String> getLastSummary(String conversationId) {
        try {
            return memoryRepository.findLastSummary(conversationId)
                    .map(MemoryEntry::getContent);

        } catch (Exception e) {
            log.error("❌ Failed to get last summary: {}", e.getMessage());
            return java.util.Optional.empty();
        }
    }

    /**
     * ⭐ Gets the last summary entry with all metadata (not just content).
     * Useful for getting compression timestamp and message count.
     *
     * @param conversationId conversation identifier
     * @return Optional containing the full MemoryEntry
     */
    @Transactional(readOnly = true)
    public java.util.Optional<MemoryEntry> getLastSummaryEntry(String conversationId) {
        try {
            return memoryRepository.findLastSummary(conversationId);

        } catch (Exception e) {
            log.error("❌ Failed to get last summary entry: {}", e.getMessage());
            return java.util.Optional.empty();
        }
    }

    /**
     * ⭐ Gets all messages that came AFTER the last summary.
     * Used to build the effective history for LLM:
     * [SUMMARY] + [recent messages since summary]
     *
     * @param conversationId conversation identifier
     * @return list of messages after the last summary (or all if no summary exists)
     */
    @Transactional(readOnly = true)
    public List<Message> getMessagesAfterSummary(String conversationId) {
        try {
            java.util.Optional<MemoryEntry> lastSummary = memoryRepository.findLastSummary(conversationId);

            if (lastSummary.isEmpty()) {
                // No summary exists - return all messages
                log.debug("No summary found, loading full history for: {}", conversationId);
                return getFullHistory(conversationId);
            }

            // Get messages after the summary timestamp
            List<MemoryEntry> entries = memoryRepository
                    .findByConversationIdAndTimestampAfterAndIsCompressedFalse(
                            conversationId,
                            lastSummary.get().getTimestamp()
                    );

            log.debug("Found {} messages after summary for: {}", entries.size(), conversationId);

            return entries.stream()
                    .map(this::toMessage)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Failed to get messages after summary: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * ⭐ Loads conversation history optimized for LLM use.
     * This is the main method that uses saved summaries to reduce token usage.
     *
     * Strategy:
     * 1. Check if a ready summary exists
     * 2. If yes - return [SYSTEM: summary] + [recent messages after summary]
     * 3. If no - return full history
     *
     * This saves tokens because:
     * - Summary is created once and reused (0 tokens to create)
     * - Only recent messages after summary are sent (fewer tokens)
     * - LLM understands context without reading all old messages
     *
     * @param conversationId conversation identifier
     * @return list of messages optimized for LLM context
     */
    @Transactional(readOnly = true)
    public List<Message> loadHistoryForLLM(String conversationId) {
        try {
            java.util.Optional<String> summaryOpt = getLastSummary(conversationId);

            if (summaryOpt.isEmpty()) {
                // No summary - load full history
                log.debug("📚 No summary found, loading full history for LLM");
                return getFullHistory(conversationId);
            }

            // Build optimized history: summary + recent messages
            List<Message> historyForLLM = new ArrayList<>();

            // Add the saved summary as a system message
            historyForLLM.add(new Message("system", summaryOpt.get()));
            log.info("🗜️ Using saved summary from database (0 tokens spent!)");

            // Add messages that came after the summary
            List<Message> recentMessages = getMessagesAfterSummary(conversationId);
            historyForLLM.addAll(recentMessages);

            log.debug("📊 Built LLM history: 1 summary + {} recent messages", recentMessages.size());
            return historyForLLM;

        } catch (Exception e) {
            log.error("❌ Failed to load history for LLM: {}", e.getMessage());
            // Fallback to full history on error
            return getFullHistory(conversationId);
        }
    }
}

