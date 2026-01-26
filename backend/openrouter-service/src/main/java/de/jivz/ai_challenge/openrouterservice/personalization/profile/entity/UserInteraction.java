package de.jivz.ai_challenge.openrouterservice.personalization.profile.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity representing a user interaction with the chat service.
 * Stores queries, responses, feedback, and metrics for learning and analytics.
 */
@Entity
@Table(
    name = "user_interactions",
    indexes = {
        @Index(name = "idx_user_interaction_user_id_created", columnList = "user_id, created_at"),
        @Index(name = "idx_user_interaction_query_type", columnList = "query_type")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User identifier who made this interaction
     */
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    /**
     * The user's query/question
     */
    @Column(name = "query", nullable = false, columnDefinition = "TEXT")
    private String query;

    /**
     * Type of query: "code", "explanation", "debug", "general"
     */
    @Column(name = "query_type", length = 50)
    private String queryType;

    /**
     * The agent's response
     */
    @Column(name = "response", columnDefinition = "TEXT")
    private String response;

    /**
     * User feedback: 1 (👍), -1 (👎), 0 (no feedback)
     */
    @Column(name = "feedback")
    @Builder.Default
    private Integer feedback = 0;

    /**
     * Additional context as JSONB
     * Example: {"tags": ["spring-boot", "jpa"], "sessionId": "abc123"}
     */
    @Type(JsonBinaryType.class)
    @Column(name = "context", columnDefinition = "jsonb")
    private Map<String, Object> context;

    /**
     * Processing time in milliseconds
     */
    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;

    /**
     * Number of tokens used (if available)
     */
    @Column(name = "tokens_used")
    private Integer tokensUsed;

    /**
     * Model used for this interaction (e.g., "claude-3.5-sonnet", "gpt-4")
     */
    @Column(name = "model", length = 100)
    private String model;

    /**
     * Timestamp when the interaction was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (queryType == null || queryType.isEmpty()) {
            queryType = determineQueryType(query);
        }
    }

    /**
     * Automatically determines the query type based on keywords
     * @param query The user's query
     * @return The determined query type
     */
    private String determineQueryType(String query) {
        if (query == null || query.isEmpty()) {
            return "general";
        }

        String lowerQuery = query.toLowerCase();

        // Check for code-related keywords
        if (lowerQuery.contains("код") || lowerQuery.contains("класс") ||
            lowerQuery.contains("метод") || lowerQuery.contains("функция") ||
            lowerQuery.contains("code") || lowerQuery.contains("class") ||
            lowerQuery.contains("method") || lowerQuery.contains("function")) {
            return "code";
        }

        // Check for explanation keywords
        if (lowerQuery.contains("объясни") || lowerQuery.contains("что такое") ||
            lowerQuery.contains("как работает") || lowerQuery.contains("explain") ||
            lowerQuery.contains("what is") || lowerQuery.contains("how does")) {
            return "explanation";
        }

        // Check for debug keywords
        if (lowerQuery.contains("ошибка") || lowerQuery.contains("баг") ||
            lowerQuery.contains("не работает") || lowerQuery.contains("fix") ||
            lowerQuery.contains("error") || lowerQuery.contains("bug") ||
            lowerQuery.contains("not working") || lowerQuery.contains("debug")) {
            return "debug";
        }

        return "general";
    }
}
