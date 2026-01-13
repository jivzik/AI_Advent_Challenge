package de.jivz.agentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Результат code review от агента
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResult {

    // PR Information
    private Integer prNumber;
    private String repository;
    private String baseSha;
    private String headSha;
    private String prTitle;
    private String prAuthor;
    private String baseBranch;
    private String headBranch;

    // Review Decision
    private ReviewDecision decision;  // APPROVE / REQUEST_CHANGES / COMMENT

    // Summary
    private String summary;  // Brief overview (2-3 sentences)

    // Issue Counts (structured from DECISION BLOCK)
    private int totalIssues;
    private int criticalIssues;  // 🔴 Must fix (security, bugs, breaking changes)
    private int majorIssues;     // ⚠️ Should fix (performance, maintainability)
    private int minorIssues;     // 💡 Nice to have (style, suggestions)

    // Full Review Text
    private String reviewText;  // Complete review from LLM

    // Metadata
    private Long reviewTimeMs;
    private LocalDateTime reviewedAt;

    /**
     * Проверяет есть ли блокирующие issues
     */
    public boolean hasBlockingIssues() {
        return criticalIssues > 0 || majorIssues > 3;
    }

    /**
     * Проверяет готов ли PR к merge
     */
    public boolean isReadyToMerge() {
        return decision == ReviewDecision.APPROVE && !hasBlockingIssues();
    }

    /**
     * Получает severity level PR (для приоритизации)
     */
    public String getSeverityLevel() {
        if (criticalIssues > 0) {
            return "CRITICAL";
        } else if (majorIssues > 0) {
            return "MAJOR";
        } else if (minorIssues > 0) {
            return "MINOR";
        } else {
            return "CLEAN";
        }
    }

    /**
     * Генерирует краткую статистику для логов
     */
    public String getIssueStats() {
        return String.format("Total: %d (🔴 %d | ⚠️ %d | 💡 %d)",
                totalIssues, criticalIssues, majorIssues, minorIssues);
    }
}