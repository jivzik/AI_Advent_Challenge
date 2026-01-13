package de.jivz.agentservice.service;



import de.jivz.agentservice.dto.GithubPrReview;
import de.jivz.agentservice.dto.ReviewDecision;
import de.jivz.agentservice.dto.ReviewResult;
import de.jivz.agentservice.mcp.MCPFactory;
import de.jivz.agentservice.mcp.model.MCPToolResult;
import de.jivz.agentservice.persistence.PRReviewEntity;
import de.jivz.agentservice.persistence.PRReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Service for storing and retrieving code reviews
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewStorageService {

    private final PRReviewRepository repository;
    private final MCPFactory mcpFactory;

    @Value("${code-review.reports-dir:reviews}")
    private String reportsDir;

    /**
     * Check if PR was already reviewed (deduplication)
     */
    public boolean isAlreadyReviewed(Integer prNumber, String headSha, String agentName) {
        boolean exists = repository.existsByPrNumberAndHeadShaAndAgentName(
                prNumber, headSha, agentName
        );

        if (exists) {
            log.info("⏭️  PR #{} (sha: {}) already reviewed by {}",
                    prNumber, headSha, agentName);
        }

        return exists;
    }

    /**
     * Save review to DB and file system
     */
    @Transactional
    public PRReviewEntity saveReview(ReviewResult review, String agentName) {
        log.info("💾 Saving review for PR #{}", review.getPrNumber());

        try {
            // 1. Save markdown file
            String filePath = saveMarkdownReport(review);

            // 2. Save to DB
            PRReviewEntity entity = PRReviewEntity.builder()
                    .prNumber(review.getPrNumber())
                    .repository(review.getRepository())
                    .baseSha(review.getBaseSha())
                    .headSha(review.getHeadSha())
                    .prTitle(review.getPrTitle())
                    .prAuthor(review.getPrAuthor())
                    .baseBranch(review.getBaseBranch())
                    .headBranch(review.getHeadBranch())
                    .agentName(agentName)
                    .agentVersion("1.0.0")
                    .reviewedAt(LocalDateTime.now())
                    .reviewTimeMs(review.getReviewTimeMs())
                    .decision(mapDecision(review.getDecision()))
                    .summary(review.getSummary())
                    .totalIssues(review.getTotalIssues())
                    .reportFilePath(filePath)
                    .status(PRReviewEntity.ReviewStatus.COMPLETED)
                    .build();

            PRReviewEntity saved = repository.save(entity);
            log.info("✅ Review saved with ID: {}", saved.getId());

            // 3. Post to GitHub via MCP
            try {
                Map<String, Object> params = Map.of(
                        "repository", review.getRepository(),
                        "pr_number", review.getPrNumber(),
                        "review_body", buildReviewBody(review),
                        "decision", review.getDecision().name(),
                        "commit_sha", review.getHeadSha()
                );

                MCPToolResult result = mcpFactory.route("git:post_pr_review", params);

                if (result.isSuccess()) {
                    Map<String, Object> data = (Map<String, Object>) result.getResult();
                    String githubUrl = (String) data.get("reviewUrl");

                    saved.setPostedToGithub(true);
                    saved.setGithubReviewUrl(githubUrl);
                    repository.save(saved);

                    log.info("✅ Review posted to GitHub: {}", githubUrl);
                }
            } catch (Exception e) {
                log.warn("⚠️ Failed to post to GitHub: {}", e.getMessage());
            }

            return saved;

        } catch (Exception e) {
            log.error("❌ Failed to save review: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save review", e);
        }
    }
    /**
     * Build review comment body for GitHub
     */
    private String buildReviewBody(ReviewResult review) {
        StringBuilder body = new StringBuilder();

        body.append("# 🤖 AI Code Review\n\n");
        body.append("## 📊 Summary\n\n");
        body.append(review.getSummary()).append("\n\n");

        body.append("**Statistics:**\n");
        body.append("- Total issues: ").append(review.getTotalIssues()).append("\n");
        body.append("- Review time: ").append(review.getReviewTimeMs() / 1000).append("s\n\n");

        body.append("## 🏁 Recommendation\n\n");
        body.append(review.getDecision()).append(" **");
        body.append(review.getDecision().name().replace("_", " ")).append("**\n\n");

        // Detailed review
        if (review.getReviewText() != null) {
            String reviewText = review.getReviewText();
            if (reviewText.length() > 5000) {
                reviewText = reviewText.substring(0, 4997) + "...";
            }
            body.append("## 📋 Review\n\n");
            body.append(reviewText).append("\n\n");
        }

        body.append("---\n");
        body.append("*Generated by AI CodeReviewAgent powered by Claude*\n");

        return body.toString();
    }

    /**
     * Save review as markdown file
     */
    private String saveMarkdownReport(ReviewResult review) throws IOException {
        // Create directory structure: reviews/2026-01/
        LocalDateTime now = LocalDateTime.now();
        String yearMonth = now.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Path dirPath = Paths.get(reportsDir, yearMonth);
        Files.createDirectories(dirPath);

        // Filename: PR-123-abc123.md or PR-123-timestamp.md if sha is null
        String shaOrTimestamp = (review.getHeadSha() != null && review.getHeadSha().length() >= 7)
                ? review.getHeadSha().substring(0, 7)
                : now.format(DateTimeFormatter.ofPattern("HHmmss"));

        String filename = String.format("PR-%d-%s.md",
                review.getPrNumber(),
                shaOrTimestamp
        );

        Path filePath = dirPath.resolve(filename);

        // Generate markdown content
        String markdown = generateMarkdown(review);

        // Write file
        Files.writeString(filePath, markdown);

        log.info("📝 Markdown report saved: {}", filePath);
        return filePath.toString();
    }

    /**
     * Generate markdown content for review
     */
    private String generateMarkdown(ReviewResult review) {
        StringBuilder md = new StringBuilder();

        md.append("# Code Review Report\n\n");
        md.append("## PR Information\n\n");
        md.append(String.format("- **PR #%d**: %s\n",
                review.getPrNumber() != null ? review.getPrNumber() : 0,
                review.getPrTitle() != null ? review.getPrTitle() : "N/A"));
        md.append(String.format("- **Repository**: %s\n",
                review.getRepository() != null ? review.getRepository() : "N/A"));
        md.append(String.format("- **Author**: %s\n",
                review.getPrAuthor() != null ? review.getPrAuthor() : "N/A"));
        md.append(String.format("- **Branch**: %s → %s\n",
                review.getHeadBranch() != null ? review.getHeadBranch() : "N/A",
                review.getBaseBranch() != null ? review.getBaseBranch() : "N/A"));

        if (review.getHeadSha() != null) {
            md.append(String.format("- **Head SHA**: %s\n", review.getHeadSha()));
        }
        if (review.getBaseSha() != null) {
            md.append(String.format("- **Base SHA**: %s\n", review.getBaseSha()));
        }

        md.append(String.format("- **Reviewed at**: %s\n", LocalDateTime.now()));
        md.append(String.format("- **Review time**: %d ms\n\n",
                review.getReviewTimeMs() != null ? review.getReviewTimeMs() : 0));

        md.append("## Decision\n\n");
        md.append(String.format("**%s** - %s\n\n",
                review.getDecision() != null ? review.getDecision() : "N/A",
                review.getSummary() != null ? review.getSummary() : "N/A"
        ));

        md.append("## Issues Found\n\n");
        md.append(String.format("- Total: %d\n\n", review.getTotalIssues()));

        md.append("## Detailed Review\n\n");
        md.append(review.getReviewText() != null ? review.getReviewText() : "No detailed review available.");
        md.append("\n\n---\n\n");
        md.append("*Generated by CodeReviewAgent*\n");

        return md.toString();
    }

    /**
     * Map ReviewResult.Decision to Entity.Decision
     */
    private PRReviewEntity.ReviewDecision mapDecision(ReviewDecision decision) {
        if (decision == null) return null;
        return PRReviewEntity.ReviewDecision.valueOf(decision.name());
    }

    /**
     * Get review by PR and commit
     */
    public PRReviewEntity getReview(Integer prNumber, String headSha, String agentName) {
        return repository.findByPrNumberAndHeadShaAndAgentName(prNumber, headSha, agentName)
                .orElse(null);
    }

    /**
     * Get all reviews for PR
     */
    public List<PRReviewEntity> getReviewsForPR(Integer prNumber) {
        return repository.findByPrNumberOrderByReviewedAtDesc(prNumber);
    }
}