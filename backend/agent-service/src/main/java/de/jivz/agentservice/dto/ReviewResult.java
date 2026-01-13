package de.jivz.agentservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResult {
    private Integer prNumber;
    private String repository;

    // Git info (для storage)
    private String baseSha;
    private String headSha;
    private String baseBranch;
    private String headBranch;

    // PR metadata
    private String prTitle;
    private String prAuthor;

    // Review result
    private ReviewDecision decision;
    private String summary;
    private int totalIssues;

    private String reviewText;  // ← ДОБАВЬ! Полный текст от LLM

    private Long reviewTimeMs;  // ← ДОБАВЬ! Timing

    @Builder.Default
    private LocalDateTime reviewedAt = LocalDateTime.now();

    public enum ReviewDecision {
        APPROVE,
        REQUEST_CHANGES,
        COMMENT
    }
}