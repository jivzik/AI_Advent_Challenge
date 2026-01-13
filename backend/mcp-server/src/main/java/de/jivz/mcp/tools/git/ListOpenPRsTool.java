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
 * Tool zum Abrufen der Liste offener Pull Requests aus einem GitHub-Repository.
 */
@Component
@Slf4j
public class ListOpenPRsTool implements Tool {

    private static final String NAME = "list_open_prs";

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

        properties.put("state", PropertyDefinition.builder()
                .type("string")
                .description("State of PRs to retrieve: 'open', 'closed', or 'all' (default: 'open')")
                .build()
        );

        properties.put("limit", PropertyDefinition.builder()
                .type("integer")
                .description("Maximum number of PRs to return (default: 30, max: 100)")
                .build()
        );

        return ToolDefinition.builder()
                .name(NAME)
                .description("Get list of open Pull Requests from a GitHub repository. Returns PR number, title, author, branches, and commit SHAs.")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .build())
                .build();
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        log.info("🔧 Executing {}: retrieving open PRs", NAME);

        // Get repository parameter
        String repository = (String) arguments.getOrDefault("repository", defaultRepository);
        if (repository == null || repository.isBlank()) {
            throw new ToolExecutionException("Repository parameter is required. Provide 'repository' or configure 'github.repository' property.");
        }

        // Get state parameter
        String state = (String) arguments.getOrDefault("state", "open");
        GHIssueState ghState = switch (state.toLowerCase()) {
            case "open" -> GHIssueState.OPEN;
            case "closed" -> GHIssueState.CLOSED;
            case "all" -> GHIssueState.ALL;
            default -> GHIssueState.OPEN;
        };

        // Get limit parameter
        int limit = arguments.containsKey("limit")
                ? ((Number) arguments.get("limit")).intValue()
                : 30;
        limit = Math.min(limit, 100); // Cap at 100

        try {
            // Connect to GitHub
            GitHub github = connectToGitHub();

            // Get repository
            GHRepository repo = github.getRepository(repository);
            log.info("📦 Connected to repository: {}", repository);

            // Get pull requests
            List<GHPullRequest> pullRequests = repo.getPullRequests(ghState);

            // Limit results
            if (pullRequests.size() > limit) {
                pullRequests = pullRequests.subList(0, limit);
            }

            log.info("📋 Found {} pull request(s) in state '{}'", pullRequests.size(), state);

            // Convert to result format
            List<Map<String, Object>> result = pullRequests.stream()
                    .map(this::convertPRToMap)
                    .collect(Collectors.toList());

            log.info("✅ Successfully retrieved {} PR(s)", result.size());
            return result;

        } catch (IOException e) {
            log.error("❌ Error retrieving pull requests from GitHub", e);
            throw new ToolExecutionException("Failed to retrieve pull requests: " + e.getMessage());
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
     * Convert GHPullRequest to Map format expected by the agent service
     */
    private Map<String, Object> convertPRToMap(GHPullRequest pr) {
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("number", pr.getNumber());
            result.put("title", pr.getTitle());
            result.put("description", pr.getBody() != null ? pr.getBody() : "");
            result.put("author", pr.getUser().getLogin());
            result.put("baseBranch", pr.getBase().getRef());
            result.put("headBranch", pr.getHead().getRef());
            result.put("baseSha", pr.getBase().getSha());
            result.put("headSha", pr.getHead().getSha());
            result.put("repository", pr.getRepository().getFullName());
            result.put("state", pr.getState().name().toLowerCase());
            result.put("createdAt", pr.getCreatedAt().toString());
            result.put("updatedAt", pr.getUpdatedAt().toString());
            result.put("url", pr.getHtmlUrl().toString());
            result.put("draft", pr.isDraft());
            result.put("merged", pr.isMerged());

            return result;
        } catch (IOException e) {
            log.warn("⚠️  Error converting PR #{} to map: {}", pr.getNumber(), e.getMessage());
            return Map.of(
                    "number", pr.getNumber(),
                    "error", "Failed to retrieve full PR details"
            );
        }
    }
}

