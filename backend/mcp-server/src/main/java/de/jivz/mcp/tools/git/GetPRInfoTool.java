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
import java.util.stream.Collectors;

/**
 * Tool zum Abrufen detaillierter Informationen zu einem spezifischen Pull Request.
 * Liefert Details wie Commits, geänderte Dateien, Reviews, Kommentare, Status, etc.
 */
@Component
@Slf4j
public class GetPRInfoTool implements Tool {

    private static final String NAME = "get_pr_info";

    @Value("${github.token}")
    private String githubToken;

    @Value("${github.repository}")
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
                .description("Pull Request number to retrieve information for (required). Also accepts 'prNumber' in camelCase.")
                .build()
        );

        properties.put("include_commits", PropertyDefinition.builder()
                .type("boolean")
                .description("Include list of commits in the PR (default: true). Also accepts 'includeCommits' in camelCase.")
                .build()
        );

        properties.put("include_files", PropertyDefinition.builder()
                .type("boolean")
                .description("Include list of changed files (default: true). Also accepts 'includeFiles' in camelCase.")
                .build()
        );

        properties.put("include_reviews", PropertyDefinition.builder()
                .type("boolean")
                .description("Include review comments and status (default: true). Also accepts 'includeReviews' in camelCase.")
                .build()
        );

        properties.put("include_comments", PropertyDefinition.builder()
                .type("boolean")
                .description("Include issue comments (default: false). Also accepts 'includeComments' in camelCase.")
                .build()
        );

        List<String> required = List.of("pr_number");

        return ToolDefinition.builder()
                .name(NAME)
                .description("Get detailed information about a specific Pull Request including commits, changed files, reviews, and comments.")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .required(required)
                        .build())
                .build();
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        log.info("🔧 Executing {}: retrieving PR information", NAME);

        // Get repository parameter
        String repository = (String) arguments.getOrDefault("repository", defaultRepository);
        if (repository == null || repository.isBlank()) {
            throw new ToolExecutionException("Repository parameter is required. Provide 'repository' or configure 'github.repository' property.");
        }

        // Get PR number (required) - support both snake_case and camelCase for compatibility
        Object prNumberObj = arguments.get("pr_number");
        if (prNumberObj == null) {
            prNumberObj = arguments.get("prNumber"); // fallback to camelCase
        }
        if (prNumberObj == null) {
            throw new ToolExecutionException("Parameter 'pr_number' (or 'prNumber') is required");
        }
        int prNumber = ((Number) prNumberObj).intValue();

        // Get optional flags - support both snake_case and camelCase
        boolean includeCommits = getBooleanParam(arguments, "include_commits", "includeCommits", true);
        boolean includeFiles = getBooleanParam(arguments, "include_files", "includeFiles", true);
        boolean includeReviews = getBooleanParam(arguments, "include_reviews", "includeReviews", true);
        boolean includeComments = getBooleanParam(arguments, "include_comments", "includeComments", false);

        try {
            // Connect to GitHub
            GitHub github = connectToGitHub();

            // Get repository
            GHRepository repo = github.getRepository(repository);
            log.info("📦 Connected to repository: {}", repository);

            // Get pull request
            GHPullRequest pr = repo.getPullRequest(prNumber);
            log.info("📋 Retrieved PR #{}: {}", prNumber, pr.getTitle());

            // Build result
            Map<String, Object> result = new LinkedHashMap<>();

            // Basic PR information
            result.put("number", pr.getNumber());
            result.put("title", pr.getTitle());
            result.put("description", pr.getBody() != null ? pr.getBody() : "");
            result.put("author", pr.getUser().getLogin());
            result.put("state", pr.getState().name().toLowerCase());
            result.put("draft", pr.isDraft());
            result.put("merged", pr.isMerged());
            result.put("mergeable", pr.getMergeable());
            result.put("mergeableState", pr.getMergeableState());

            // Branch information
            Map<String, String> branches = new LinkedHashMap<>();
            branches.put("base", pr.getBase().getRef());
            branches.put("baseSha", pr.getBase().getSha());
            branches.put("head", pr.getHead().getRef());
            branches.put("headSha", pr.getHead().getSha());
            result.put("branches", branches);

            // Dates
            result.put("createdAt", pr.getCreatedAt().toString());
            result.put("updatedAt", pr.getUpdatedAt().toString());
            if (pr.isMerged()) {
                result.put("mergedAt", pr.getMergedAt() != null ? pr.getMergedAt().toString() : null);
            }
            if (pr.getClosedAt() != null) {
                result.put("closedAt", pr.getClosedAt().toString());
            }

            // URLs
            result.put("url", pr.getHtmlUrl().toString());
            result.put("apiUrl", pr.getUrl().toString());

            // Stats
            result.put("additions", pr.getAdditions());
            result.put("deletions", pr.getDeletions());
            result.put("changedFiles", pr.getChangedFiles());
            result.put("commits", pr.getCommits());

            // Labels
            List<String> labels = pr.getLabels().stream()
                    .map(GHLabel::getName)
                    .collect(Collectors.toList());
            result.put("labels", labels);

            // Assignees
            List<String> assignees = pr.getAssignees().stream()
                    .map(GHUser::getLogin)
                    .collect(Collectors.toList());
            result.put("assignees", assignees);

            // Requested reviewers
            List<String> requestedReviewers = pr.getRequestedReviewers().stream()
                    .map(GHUser::getLogin)
                    .collect(Collectors.toList());
            result.put("requestedReviewers", requestedReviewers);

            // Optional: Include commits
            if (includeCommits) {
                log.debug("📝 Including commits in response");
                List<Map<String, Object>> commits = new ArrayList<>();
                for (GHPullRequestCommitDetail commit : pr.listCommits()) {
                    Map<String, Object> commitInfo = new LinkedHashMap<>();
                    commitInfo.put("sha", commit.getSha());
                    commitInfo.put("message", commit.getCommit().getMessage());
                    commitInfo.put("author", commit.getCommit().getAuthor().getName());
                    commitInfo.put("date", commit.getCommit().getAuthor().getDate().toString());
                    commits.add(commitInfo);
                }
                result.put("commitsList", commits);
                log.info("  ✅ Added {} commits", commits.size());
            }

            // Optional: Include changed files
            if (includeFiles) {
                log.debug("📁 Including changed files in response");
                List<Map<String, Object>> files = new ArrayList<>();
                for (GHPullRequestFileDetail file : pr.listFiles()) {
                    Map<String, Object> fileInfo = new LinkedHashMap<>();
                    fileInfo.put("filename", file.getFilename());
                    fileInfo.put("status", file.getStatus());
                    fileInfo.put("additions", file.getAdditions());
                    fileInfo.put("deletions", file.getDeletions());
                    fileInfo.put("changes", file.getChanges());
                    fileInfo.put("patch", file.getPatch());
                    files.add(fileInfo);
                }
                result.put("files", files);
                log.info("  ✅ Added {} changed files", files.size());
            }

            // Optional: Include reviews
            if (includeReviews) {
                log.debug("🔍 Including reviews in response");
                List<Map<String, Object>> reviews = new ArrayList<>();
                for (GHPullRequestReview review : pr.listReviews()) {
                    Map<String, Object> reviewInfo = new LinkedHashMap<>();
                    reviewInfo.put("id", review.getId());
                    reviewInfo.put("author", review.getUser().getLogin());
                    reviewInfo.put("state", review.getState() != null ? review.getState().name() : "UNKNOWN");
                    reviewInfo.put("body", review.getBody() != null ? review.getBody() : "");
                    reviewInfo.put("submittedAt", review.getSubmittedAt() != null ? review.getSubmittedAt().toString() : null);
                    reviews.add(reviewInfo);
                }
                result.put("reviews", reviews);
                log.info("  ✅ Added {} reviews", reviews.size());
            }

            // Optional: Include comments
            if (includeComments) {
                log.debug("💬 Including comments in response");
                List<Map<String, Object>> comments = new ArrayList<>();
                for (GHIssueComment comment : pr.getComments()) {
                    Map<String, Object> commentInfo = new LinkedHashMap<>();
                    commentInfo.put("id", comment.getId());
                    commentInfo.put("author", comment.getUser().getLogin());
                    commentInfo.put("body", comment.getBody());
                    commentInfo.put("createdAt", comment.getCreatedAt().toString());
                    commentInfo.put("updatedAt", comment.getUpdatedAt().toString());
                    comments.add(commentInfo);
                }
                result.put("comments", comments);
                log.info("  ✅ Added {} comments", comments.size());
            }

            log.info("✅ Successfully retrieved detailed information for PR #{}", prNumber);
            return result;

        } catch (GHFileNotFoundException e) {
            log.error("❌ Pull Request #{} not found in repository {}", prNumber, repository);
            throw new ToolExecutionException("Pull Request #" + prNumber + " not found in repository " + repository);
        } catch (IOException e) {
            log.error("❌ Error retrieving PR information from GitHub", e);
            throw new ToolExecutionException("Failed to retrieve PR information: " + e.getMessage());
        }
    }

    /**
     * Connect to GitHub using configured token or anonymous access
     */
    private GitHub connectToGitHub() throws IOException {
        if (githubToken != null && !githubToken.isBlank()) {
            log.debug("🔐 Connecting to GitHub with token authentication");
            return new GitHubBuilder()
                    .withOAuthToken(githubToken)
                    .build();
        } else {
            log.warn("⚠️  No GitHub token configured. Using anonymous access (rate limited).");
            log.warn("⚠️  Configure 'github.token' property for better rate limits.");
            return GitHub.connectAnonymously();
        }
    }

    /**
     * Get boolean parameter supporting both snake_case and camelCase naming conventions
     */
    private boolean getBooleanParam(Map<String, Object> arguments, String snakeCaseName, String camelCaseName, boolean defaultValue) {
        Object value = arguments.get(snakeCaseName);
        if (value == null) {
            value = arguments.get(camelCaseName);
        }
        if (value == null) {
            return defaultValue;
        }
        return (boolean) value;
    }
}

