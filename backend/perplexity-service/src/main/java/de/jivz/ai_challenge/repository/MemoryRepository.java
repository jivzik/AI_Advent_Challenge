package de.jivz.ai_challenge.repository;

import de.jivz.ai_challenge.entity.MemoryEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for accessing MemoryEntry entities in PostgreSQL.
 * Provides methods for querying conversation history with various filters.
 *
 * Supports:
 * - Conversation history retrieval (full and filtered)
 * - User-specific queries
 * - Time-based queries
 * - Aggregations and statistics
 * - Batch operations
 */
@Repository
public interface MemoryRepository extends JpaRepository<MemoryEntry, Long> {

    /**
     * Finds all messages for a conversation, ordered chronologically.
     * This is the primary method for loading conversation history.
     *
     * @param conversationId the conversation identifier
     * @return list of messages ordered by timestamp (oldest first)
     */
    List<MemoryEntry> findByConversationIdOrderByTimestampAsc(String conversationId);

    /**
     * Finds all messages for a conversation with pagination.
     * Useful for large conversations to avoid loading everything at once.
     *
     * @param conversationId the conversation identifier
     * @param pageable pagination parameters
     * @return page of messages
     */
    Page<MemoryEntry> findByConversationIdOrderByTimestampAsc(String conversationId, Pageable pageable);

    /**
     * Finds only original (non-compressed) messages for a conversation.
     * Used when you need the raw history without summaries.
     *
     * @param conversationId the conversation identifier
     * @return list of original messages only
     */
    List<MemoryEntry> findByConversationIdAndIsCompressedFalseOrderByTimestampAsc(String conversationId);

    /**
     * Finds all conversations for a specific user, ordered by most recent first.
     * Returns the latest message from each conversation.
     *
     * @param userId the user identifier
     * @return list of user's messages ordered by timestamp (newest first)
     */
    List<MemoryEntry> findByUserIdOrderByTimestampDesc(String userId);

    /**
     * Finds all distinct conversation IDs for a user.
     * Useful for listing all user's conversations.
     *
     * @param userId the user identifier
     * @return list of unique conversation IDs
     */
    @Query("SELECT m.conversationId FROM MemoryEntry m WHERE m.userId = :userId GROUP BY m.conversationId ORDER BY MAX(m.timestamp) DESC")
    List<String> findDistinctConversationIdsByUserId(@Param("userId") String userId);

    /**
     * Finds messages within a specific time range.
     * Useful for analytics and data export.
     *
     * @param start start timestamp (inclusive)
     * @param end end timestamp (inclusive)
     * @return list of messages in the time range
     */
    List<MemoryEntry> findByTimestampBetweenOrderByTimestampAsc(Instant start, Instant end);

    /**
     * Counts total messages in a conversation.
     *
     * @param conversationId the conversation identifier
     * @return number of messages
     */
    long countByConversationId(String conversationId);

    /**
     * Counts only non-compressed messages in a conversation.
     *
     * @param conversationId the conversation identifier
     * @return number of original messages
     */
    long countByConversationIdAndIsCompressedFalse(String conversationId);

    /**
     * Checks if any messages exist for a conversation.
     * More efficient than loading all messages.
     *
     * @param conversationId the conversation identifier
     * @return true if conversation has messages
     */
    boolean existsByConversationId(String conversationId);

    /**
     * Deletes all messages for a conversation.
     * Used when user wants to clear conversation history.
     *
     * @param conversationId the conversation identifier
     * @return number of deleted entries
     */
    @Modifying
    @Query("DELETE FROM MemoryEntry m WHERE m.conversationId = :conversationId")
    int deleteByConversationId(@Param("conversationId") String conversationId);

    /**
     * Gets the most recent message for a conversation.
     * Useful for checking last activity.
     *
     * @param conversationId the conversation identifier
     * @return the most recent message or null
     */
    MemoryEntry findFirstByConversationIdOrderByTimestampDesc(String conversationId);

    /**
     * Finds the most recent N messages for a conversation.
     * Useful for loading recent context.
     *
     * @param conversationId the conversation identifier
     * @param pageable pagination with limit
     * @return list of recent messages
     */
    List<MemoryEntry> findByConversationIdOrderByTimestampDesc(String conversationId, Pageable pageable);

    /**
     * Counts total conversations (distinct conversation IDs).
     *
     * @return number of unique conversations
     */
    @Query("SELECT COUNT(DISTINCT m.conversationId) FROM MemoryEntry m")
    long countDistinctConversations();

    /**
     * Counts total messages across all conversations.
     *
     * @return total number of messages
     */
    @Query("SELECT COUNT(m) FROM MemoryEntry m")
    long countAllMessages();

    /**
     * Gets total token usage across all conversations.
     *
     * @return sum of all total_tokens
     */
    @Query("SELECT COALESCE(SUM(m.totalTokens), 0) FROM MemoryEntry m WHERE m.totalTokens IS NOT NULL")
    long sumTotalTokens();

    /**
     * Gets total cost across all conversations.
     *
     * @return sum of all costs
     */
    @Query("SELECT COALESCE(SUM(m.cost), 0) FROM MemoryEntry m WHERE m.cost IS NOT NULL")
    Double sumTotalCost();

    /**
     * Gets conversation statistics (aggregated data).
     * Returns: total tokens, total cost, message count.
     *
     * @param conversationId the conversation identifier
     * @return array with [totalTokens, totalCost, messageCount]
     */
    @Query("SELECT COALESCE(SUM(m.totalTokens), 0), COALESCE(SUM(m.cost), 0), COUNT(m) " +
           "FROM MemoryEntry m WHERE m.conversationId = :conversationId")
    Object[] getConversationStats(@Param("conversationId") String conversationId);

    /**
     * ⭐ Finds the last (most recent) summary for a conversation.
     * Returns the most recent compressed/summary message.
     *
     * @param conversationId the conversation identifier
     * @return the most recent summary or empty if no summary exists
     */
    @Query("SELECT m FROM MemoryEntry m WHERE m.conversationId = :conversationId " +
           "AND m.isCompressed = true AND m.role = 'system' ORDER BY m.timestamp DESC")
    java.util.Optional<MemoryEntry> findLastSummary(@Param("conversationId") String conversationId);

    /**
     * ⭐ Finds messages after a specific timestamp (excluding compressed/summary messages).
     * Used to get messages that came after the last summary.
     *
     * @param conversationId the conversation identifier
     * @param after the timestamp to start from (exclusive)
     * @return list of non-compressed messages after the given time
     */
    List<MemoryEntry> findByConversationIdAndTimestampAfterAndIsCompressedFalse(
            String conversationId, LocalDateTime after);

    /**
     * ⭐ Finds all distinct conversation IDs across all users.
     * Useful for listing all conversations in the system.
     *
     * @return list of all unique conversation IDs ordered by most recent first
     */
    @Query("SELECT m.conversationId FROM MemoryEntry m GROUP BY m.conversationId ORDER BY MAX(m.timestamp) DESC")
    List<String> findAllConversationIds();

    /**
     * ⭐ Counts compressed messages for a conversation.
     * Used to determine if conversation has summaries.
     *
     * @param conversationId the conversation identifier
     * @return count of compressed messages
     */
    long countByConversationIdAndIsCompressedTrue(String conversationId);

    /**
     * ⭐ Gets the first message (oldest) for a conversation.
     * Used to get the first message for preview/title.
     *
     * @param conversationId the conversation identifier
     * @return the first message or null
     */
    MemoryEntry findFirstByConversationIdOrderByTimestampAsc(String conversationId);
}

