package de.jivz.mcp.tools.git;

import de.jivz.mcp.model.InputSchema;
import de.jivz.mcp.model.PropertyDefinition;
import de.jivz.mcp.model.ToolDefinition;
import de.jivz.mcp.tools.Tool;
import de.jivz.mcp.tools.ToolExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * Tool zum Posten eines Pull Request Reviews auf GitHub.
 * Unterstützt verschiedene Review-Entscheidungen (APPROVE, REQUEST_CHANGES, COMMENT)
 * und optionale inline Kommentare zu spezifischen Zeilen.
 */
@Component
@Slf4j
public class PostPRReviewTool implements Tool {

    private static final String NAME = "post_pr_review";

    @Value("${personal.github.token}")
    private String githubToken;

    @Value("${personal.github.repository}")
    private String defaultRepository;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ToolDefinition getDefinition() {
        Map<String, PropertyDefinition> properties = new LinkedHashMap<>();

        properties.put("repository", PropertyDefinition.builder()
                .type("string")
                .description("GitHub repository in format 'owner/repo' (e.g., 'octocat/Hello-World'). Optional if default repository is configured.")
                .build()
        );

        properties.put("pr_number", PropertyDefinition.builder()
                .type("integer")
                .description("Pull Request number to review (required). Also accepts 'prNumber' in camelCase.")
                .build()
        );

        properties.put("review_body", PropertyDefinition.builder()
                .type("string")
                .description("Main review comment body (required). Also accepts 'reviewBody' in camelCase.")
                .build()
        );

        properties.put("decision", PropertyDefinition.builder()
                .type("string")
                .description("Review decision: 'APPROVE', 'REQUEST_CHANGES', or 'COMMENT' (default: 'COMMENT')")
                .build()
        );

        properties.put("commit_sha", PropertyDefinition.builder()
                .type("string")
                .description("Specific commit SHA to review. If not provided, reviews the latest commit. Also accepts 'commitSha' in camelCase.")
                .build()
        );

        properties.put("comments", PropertyDefinition.builder()
                .type("array")
                .description("Optional array of inline comments. Each comment should have: path (string), position (integer), body (string)")
                .build()
        );

        List<String> required = List.of("pr_number", "review_body");

        return ToolDefinition.builder()
                .name(NAME)
                .description("Post a review on a GitHub Pull Request with optional inline comments and decision (APPROVE, REQUEST_CHANGES, or COMMENT).")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .required(required)
                        .build())
                .build();
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        log.info("🔧 Executing {}: posting PR review", NAME);

        // Validate GitHub token
        if (githubToken == null || githubToken.isBlank()) {
            throw new ToolExecutionException("GitHub token is required for posting reviews. Configure 'github.token' property.");
        }

        // Get repository parameter
        String repository = (String) arguments.getOrDefault("repository", defaultRepository);
        if (repository == null || repository.isBlank()) {
            throw new ToolExecutionException("Repository parameter is required. Provide 'repository' or configure 'github.repository' property.");
        }

        // Get PR number (required) - support both snake_case and camelCase
        Object prNumberObj = arguments.get("pr_number");
        if (prNumberObj == null) {
            prNumberObj = arguments.get("prNumber");
        }
        if (prNumberObj == null) {
            throw new ToolExecutionException("Parameter 'pr_number' (or 'prNumber') is required");
        }
        int prNumber = ((Number) prNumberObj).intValue();

        // Get review body (required) - support both snake_case and camelCase
        String reviewBody = (String) arguments.get("review_body");
        if (reviewBody == null) {
            reviewBody = (String) arguments.get("reviewBody");
        }
        if (reviewBody == null || reviewBody.isBlank()) {
            throw new ToolExecutionException("Parameter 'review_body' (or 'reviewBody') is required");
        }

        // Get decision (optional, default to COMMENT)
        String decisionStr = (String) arguments.getOrDefault("decision", "COMMENT");
        GHPullRequestReviewEvent decision = parseDecision(decisionStr);

        // Get commit SHA (optional) - support both snake_case and camelCase
        String commitSha = (String) arguments.get("commit_sha");
        if (commitSha == null) {
            commitSha = (String) arguments.get("commitSha");
        }

        // Get inline comments (optional)
        List<Map<String, Object>> comments = (List<Map<String, Object>>) arguments.get("comments");

        try {
            // Connect to GitHub
            GitHub github = connectToGitHub();

            // Get repository
            GHRepository repo = github.getRepository(repository);
            log.info("📦 Connected to repository: {}", repository);

            // Get pull request
            GHPullRequest pr = repo.getPullRequest(prNumber);
            log.info("📋 Retrieved PR #{}: {}", prNumber, pr.getTitle());

            // Use provided commit SHA or latest
            String reviewCommitSha = commitSha;
            if (reviewCommitSha == null || reviewCommitSha.isBlank()) {
                reviewCommitSha = pr.getHead().getSha();
                log.info("📌 Using latest commit SHA: {}", reviewCommitSha);
            } else {
                log.info("📌 Using provided commit SHA: {}", reviewCommitSha);
            }

            // Create review builder
            GHPullRequestReviewBuilder reviewBuilder = pr.createReview()
                    .body(reviewBody)
                    .commitId(reviewCommitSha)
                    .event(decision);

            // Add inline comments if provided
            if (comments != null && !comments.isEmpty()) {
                log.info("💬 Adding {} inline comment(s)", comments.size());
                for (Map<String, Object> comment : comments) {
                    String path = (String) comment.get("path");
                    Object positionObj = comment.get("position");
                    String body = (String) comment.get("body");

                    if (path == null || positionObj == null || body == null) {
                        log.warn("⚠️ Skipping invalid inline comment: missing required fields (path, position, body)");
                        continue;
                    }

                    int position = ((Number) positionObj).intValue();
                    reviewBuilder.comment(body, path, position);
                    log.debug("  ✅ Added comment on {} at position {}", path, position);
                }
            }

            // Submit review
            log.info("🚀 Submitting review...");
            GHPullRequestReview review = reviewBuilder.create();

            // Build result
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("reviewId", review.getId());
            result.put("reviewUrl", review.getHtmlUrl().toString());
            result.put("state", review.getState() != null ? review.getState().name() : "UNKNOWN");
            result.put("author", review.getUser().getLogin());
            result.put("body", review.getBody());
            result.put("commitSha", reviewCommitSha);
            result.put("submittedAt", review.getSubmittedAt() != null ? review.getSubmittedAt().toString() : null);

            log.info("✅ Successfully posted review for PR #{}: {}", prNumber, review.getHtmlUrl());
            log.info("   Decision: {}", decision.name());

            return result;

        } catch (GHFileNotFoundException e) {
            log.error("❌ Pull Request #{} not found in repository {}", prNumber, repository);
            throw new ToolExecutionException("Pull Request #" + prNumber + " not found in repository " + repository);
        } catch (IOException e) {
            log.error("❌ Error posting PR review to GitHub", e);
            throw new ToolExecutionException("Failed to post PR review: " + e.getMessage());
        }
    }

    /**
     * Parse decision string to GHPullRequestReviewEvent
     */
    private GHPullRequestReviewEvent parseDecision(String decision) {
        if (decision == null) {
            return GHPullRequestReviewEvent.COMMENT;
        }

        return switch (decision.toUpperCase()) {
            case "APPROVE", "APPROVED" -> GHPullRequestReviewEvent.APPROVE;
            case "REQUEST_CHANGES", "CHANGES_REQUESTED" -> GHPullRequestReviewEvent.REQUEST_CHANGES;
            case "COMMENT", "COMMENTED" -> GHPullRequestReviewEvent.COMMENT;
            default -> {
                log.warn("⚠️ Unknown decision '{}', using COMMENT", decision);
                yield GHPullRequestReviewEvent.COMMENT;
            }
        };
    }

    /**
     * Connect to GitHub using configured token
     */
    private GitHub connectToGitHub() throws IOException {
        log.debug("🔐 Connecting to GitHub with token authentication");
        return new GitHubBuilder()
                .withOAuthToken(githubToken)
                .build();
    }
}

