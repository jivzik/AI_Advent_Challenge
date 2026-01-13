package de.jivz.agentservice.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity for PR review records
 * Tracks review status and prevents duplicate reviews
 */
@Entity
@Table(
        name = "pr_reviews",
        uniqueConstraints = @UniqueConstraint(
                name = "unique_pr_review",
                columnNames = {"pr_number", "head_sha", "agent_name"}
        ),
        indexes = {
                @Index(name = "idx_pr_reviews_pr_number", columnList = "pr_number"),
                @Index(name = "idx_pr_reviews_repository", columnList = "repository"),
                @Index(name = "idx_pr_reviews_head_sha", columnList = "head_sha"),
                @Index(name = "idx_pr_reviews_reviewed_at", columnList = "reviewed_at"),
                @Index(name = "idx_pr_reviews_status", columnList = "status"),
                @Index(name = "idx_pr_reviews_dedup", columnList = "pr_number, head_sha, agent_name")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PRReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // PR identification
    @Column(name = "pr_number", nullable = false)
    private Integer prNumber;

    @Column(name = "repository", nullable = false, length = 500)
    private String repository;

    // Git commits (for deduplication)
    @Column(name = "base_sha", nullable = false, length = 40)
    private String baseSha;

    @Column(name = "head_sha", nullable = false, length = 40)
    private String headSha;

    // PR metadata
    @Column(name = "pr_title", length = 1000)
    private String prTitle;

    @Column(name = "pr_author")
    private String prAuthor;

    @Column(name = "base_branch")
    private String baseBranch;

    @Column(name = "head_branch")
    private String headBranch;

    // Agent info
    @Column(name = "agent_name", length = 100)
    @Builder.Default
    private String agentName = "CodeReviewAgent";

    @Column(name = "agent_version", length = 50)
    @Builder.Default
    private String agentVersion = "1.0.0";

    // Review timing
    @Column(name = "reviewed_at", nullable = false)
    @Builder.Default
    private LocalDateTime reviewedAt = LocalDateTime.now();

    @Column(name = "review_time_ms")
    private Long reviewTimeMs;

    // Review results
    @Column(name = "decision", length = 50)
    @Enumerated(EnumType.STRING)
    private ReviewDecision decision;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "total_issues")
    @Builder.Default
    private Integer totalIssues = 0;

    @Column(name = "critical_issues")
    @Builder.Default
    private Integer criticalIssues = 0;

    @Column(name = "high_issues")
    @Builder.Default
    private Integer highIssues = 0;

    @Column(name = "medium_issues")
    @Builder.Default
    private Integer mediumIssues = 0;

    @Column(name = "low_issues")
    @Builder.Default
    private Integer lowIssues = 0;

    // File reference
    @Column(name = "report_file_path", length = 500)
    private String reportFilePath;

    // GitHub integration
    @Column(name = "posted_to_github")
    @Builder.Default
    private Boolean postedToGithub = false;

    @Column(name = "github_review_url", length = 1000)
    private String githubReviewUrl;

    // Status
    @Column(name = "status", length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.COMPLETED;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Timestamps
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Enums
    public enum ReviewDecision {
        APPROVE,
        REQUEST_CHANGES,
        COMMENT
    }

    public enum ReviewStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}