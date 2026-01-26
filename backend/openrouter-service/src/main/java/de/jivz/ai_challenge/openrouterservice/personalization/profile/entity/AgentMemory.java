package de.jivz.ai_challenge.openrouterservice.personalization.profile.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing agent's memory about user patterns and preferences.
 * Used for learning and adapting to user behavior over time.
 */
@Entity
@Table(
    name = "agent_memory",
    indexes = {
        @Index(name = "idx_agent_memory_user_id_type", columnList = "user_id, memory_type"),
        @Index(name = "idx_agent_memory_user_id_key", columnList = "user_id, key", unique = true),
        @Index(name = "idx_agent_memory_confidence", columnList = "confidence")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User identifier this memory belongs to
     */
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    /**
     * Type of memory: "preference", "pattern", "context", "learned"
     */
    @Column(name = "memory_type", nullable = false, length = 50)
    private String memoryType;

    /**
     * Key for this memory entry (e.g., "frequent_query_type", "preferred_error_handling")
     */
    @Column(name = "key", nullable = false, length = 255)
    private String key;

    /**
     * Value of this memory entry (stored as text, can be JSON for complex values)
     */
    @Column(name = "value", nullable = false, columnDefinition = "TEXT")
    private String value;

    /**
     * Confidence level (0.0 to 1.0) indicating how confident the agent is about this memory
     */
    @Column(name = "confidence", nullable = false)
    @Builder.Default
    private Double confidence = 0.5;

    /**
     * Number of times this memory has been used/reinforced
     */
    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private Integer usageCount = 0;

    /**
     * Timestamp when this memory was last used
     */
    @Column(name = "last_used")
    private LocalDateTime lastUsed;

    /**
     * Timestamp when this memory was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this memory was last updated
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Increment usage count and update last used timestamp
     */
    public void incrementUsage() {
        usageCount++;
        lastUsed = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Update confidence level (bounded between 0.0 and 1.0)
     * @param newConfidence The new confidence level
     */
    public void updateConfidence(Double newConfidence) {
        this.confidence = Math.max(0.0, Math.min(1.0, newConfidence));
        this.updatedAt = LocalDateTime.now();
    }
}
