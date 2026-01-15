package de.jivz.teamassistantservice.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Query Log - история запросов к Team Assistant
 */
@Entity
@Table(name = "query_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_member_id")
    private TeamMember teamMember;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String query;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(name = "query_type", length = 50)
    private String queryType; // SHOW_TASKS, CREATE_TASK, ANALYZE_PRIORITY, ANSWER_QUESTION, RECOMMENDATION

    // Tools used
    @Column(name = "tools_used", columnDefinition = "TEXT[]")
    private String[] toolsUsed;

    // Sources from RAG
    @Column(name = "rag_sources", columnDefinition = "TEXT[]")
    private String[] ragSources;

    // Actions performed
    @Column(name = "actions_performed", columnDefinition = "TEXT[]")
    private String[] actionsPerformed; // task_created, task_updated, etc

    // Metrics
    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "confidence_score", precision = 3, scale = 2)
    private BigDecimal confidenceScore;

    // User feedback
    @Column(name = "user_feedback")
    private Boolean userFeedback; // true = helpful, false = not helpful

    @Column(name = "feedback_comment", columnDefinition = "TEXT")
    private String feedbackComment;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "session_id")
    private String sessionId; // для группировки запросов в одной сессии
}