package de.jivz.ai_challenge.openrouterservice.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity for storing conversation messages in PostgreSQL.
 * Represents the long-term memory of AI agent conversations.
 *
 * Key features:
 * - Stores ALL messages (full history) in database
 * - Supports metrics (tokens, cost, response time)
 * - Indexed for fast queries by conversation_id, user_id, timestamp
 * - Compression flag for identifying summary messages
 */
@Entity
@Table(name = "memory_entries", indexes = {
    @Index(name = "idx_conversation_id", columnList = "conversation_id"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_conversation_timestamp", columnList = "conversation_id, timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Conversation identifier - groups related messages together.
     * Required field, indexed for fast lookups.
     */
    @Column(name = "conversation_id", nullable = false, length = 255)
    private String conversationId;

    /**
     * User identifier - tracks which user owns the conversation.
     * Nullable (for anonymous users), indexed for multi-user queries.
     */
    @Column(name = "user_id", length = 255)
    private String userId;

    /**
     * Message role: "user", "assistant", or "system".
     * Required field.
     */
    @Column(name = "role", nullable = false, length = 50)
    private String role;

    /**
     * Message content - the actual text of the message.
     * TEXT type for unlimited length.
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Timestamp when the message was created.
     * Indexed for chronological queries and sorting.
     */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /**
     * AI model used for generating this message.
     * Nullable (only for assistant messages), useful for analytics.
     */
    @Column(name = "model", length = 255)
    private String model;

    /**
     * Number of input tokens used (for API cost tracking).
     * Nullable - only relevant for LLM requests.
     */
    @Column(name = "input_tokens")
    private Integer inputTokens;

    /**
     * Number of output tokens generated (for API cost tracking).
     * Nullable - only relevant for LLM responses.
     */
    @Column(name = "output_tokens")
    private Integer outputTokens;

    /**
     * Total tokens used (input + output).
     * Nullable - calculated field for convenience.
     */
    @Column(name = "total_tokens")
    private Integer totalTokens;

    /**
     * Cost of this API call in USD.
     * DECIMAL(10,6) for precise currency calculations.
     * Nullable - only for paid API calls.
     */
    @Column(name = "cost", precision = 10, scale = 6)
    private BigDecimal cost;

    /**
     * Flag indicating if this is a compressed/summary message.
     * False = original message
     * True = this is a summary created by DialogCompressionService
     *
     * Note: Currently compression happens separately, this flag is for future use
     * when we might store summaries as separate entries.
     */
    @Column(name = "is_compressed", nullable = false)
    @Builder.Default
    private Boolean isCompressed = false;

    /**
     * Response time in milliseconds.
     * Nullable - tracks API performance.
     */
    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    /**
     * ⭐ Number of messages that were compressed into this summary.
     * Only used when role="system" and isCompressed=true.
     * Nullable - only for summary entries.
     */
    @Column(name = "compressed_messages_count")
    private Integer compressedMessagesCount;

    /**
     * ⭐ Timestamp when this summary was created.
     * Only used when role="system" and isCompressed=true.
     * Nullable - only for summary entries.
     */
    @Column(name = "compression_timestamp")
    private LocalDateTime compressionTimestamp;

    /**
     * Helper method to check if this entry has metrics data.
     */
    public boolean hasMetrics() {
        return inputTokens != null || outputTokens != null || cost != null;
    }

    /**
     * Helper method to check if this is a user message.
     */
    public boolean isUserMessage() {
        return "user".equalsIgnoreCase(role);
    }

    /**
     * Helper method to check if this is an assistant message.
     */
    public boolean isAssistantMessage() {
        return "assistant".equalsIgnoreCase(role);
    }

    /**
     * Helper method to check if this is a system message.
     */
    public boolean isSystemMessage() {
        return "system".equalsIgnoreCase(role);
    }
}

