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
 * Tool zum Abrufen der Liste von GitHub Issues.
 */
@Component
@Slf4j
public class ListGitHubIssuesTool implements Tool {

    private static final String NAME = "list_github_issues";

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

        properties.put("state", PropertyDefinition.builder()
                .type("string")
                .description("State of issues to retrieve: 'open', 'closed', or 'all' (default: 'open')")
                .build()
        );

        properties.put("labels", PropertyDefinition.builder()
                .type("array")
                .description("Filter by label names (optional)")
                .build()
        );

        properties.put("assignee", PropertyDefinition.builder()
                .type("string")
                .description("Filter by assignee username (optional)")
                .build()
        );

        properties.put("creator", PropertyDefinition.builder()
                .type("string")
                .description("Filter by creator username (optional)")
                .build()
        );

        properties.put("limit", PropertyDefinition.builder()
                .type("integer")
                .description("Maximum number of issues to return (default: 30, max: 100)")
                .build()
        );

        return ToolDefinition.builder()
                .name(NAME)
                .description("Get list of issues from a GitHub repository. Returns issue number, title, state, author, labels, and assignees. Supports filtering by state, labels, assignee, and creator.")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .build())
                .build();
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        log.info("🔧 Executing {}: retrieving GitHub issues", NAME);

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

        // Get optional filter parameters
        @SuppressWarnings("unchecked")
        List<String> labels = (List<String>) arguments.getOrDefault("labels", new ArrayList<>());
        String assignee = (String) arguments.get("assignee");
        String creator = (String) arguments.get("creator");

        // Get limit parameter
        int limit = arguments.containsKey("limit")
                ? ((Number) arguments.get("limit")).intValue()
                : 30;
        limit = Math.min(limit, 100); // Cap at 100

        try {
            // Connect to GitHub
            GitHub github = connectToGitHub();
            GHRepository repo = github.getRepository(repository);
            log.info("📦 Connected to repository: {}", repository);

            // Build query
            GHIssueQueryBuilder.Sort sort = GHIssueQueryBuilder.Sort.UPDATED;
            List<GHIssue> issues = repo.queryIssues()
                    .state(ghState)
                    .sort(sort)
                    .list()
                    .toList();

            log.info("📋 Found {} issue(s) in state '{}'", issues.size(), state);

            // Apply filters
            if (!labels.isEmpty()) {
                Set<String> labelSet = new HashSet<>(labels);
                issues = issues.stream()
                        .filter(issue -> {
                            Set<String> issueLabels = issue.getLabels().stream()
                                    .map(GHLabel::getName)
                                    .collect(Collectors.toSet());
                            return issueLabels.containsAll(labelSet);
                        })
                        .collect(Collectors.toList());
                log.info("🏷️  Filtered by labels: {} -> {} issue(s)", labels, issues.size());
            }

            if (assignee != null && !assignee.isBlank()) {
                issues = issues.stream()
                        .filter(issue -> issue.getAssignees().stream()
                                .anyMatch(user -> user.getLogin().equalsIgnoreCase(assignee)))
                        .collect(Collectors.toList());
                log.info("👤 Filtered by assignee: {} -> {} issue(s)", assignee, issues.size());
            }

            if (creator != null && !creator.isBlank()) {
                issues = issues.stream()
                        .filter(issue -> {
                            try {
                                return issue.getUser().getLogin().equalsIgnoreCase(creator);
                            } catch (IOException e) {
                                log.warn("Failed to get creator for issue #{}", issue.getNumber());
                                return false;
                            }
                        })
                        .collect(Collectors.toList());
                log.info("✍️  Filtered by creator: {} -> {} issue(s)", creator, issues.size());
            }

            // Limit results
            if (issues.size() > limit) {
                issues = issues.subList(0, limit);
            }

            // Convert to result format
            List<Map<String, Object>> result = issues.stream()
                    .map(this::convertIssueToMap)
                    .collect(Collectors.toList());

            log.info("✅ Successfully retrieved {} issue(s)", result.size());
            return result;

        } catch (IOException e) {
            log.error("❌ Error retrieving issues from GitHub", e);
            throw new ToolExecutionException("Failed to retrieve issues: " + e.getMessage());
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
     * Convert GHIssue to Map format
     */
    private Map<String, Object> convertIssueToMap(GHIssue issue) {
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("number", issue.getNumber());
            result.put("title", issue.getTitle());
            result.put("body", issue.getBody() != null ? issue.getBody() : "");
            result.put("state", issue.getState().name().toLowerCase());
            result.put("author", issue.getUser().getLogin());
            result.put("url", issue.getHtmlUrl().toString());
            result.put("createdAt", issue.getCreatedAt().toString());
            result.put("updatedAt", issue.getUpdatedAt().toString());

            // Comments count
            result.put("commentsCount", issue.getCommentsCount());

            // Labels
            List<String> labelNames = issue.getLabels().stream()
                    .map(GHLabel::getName)
                    .toList();
            result.put("labels", labelNames);

            // Assignees
            List<String> assigneeLogins = issue.getAssignees().stream()
                    .map(GHUser::getLogin)
                    .toList();
            result.put("assignees", assigneeLogins);

            // Milestone
            GHMilestone milestone = issue.getMilestone();
            result.put("milestone", milestone != null ? milestone.getTitle() : null);

            return result;
        } catch (IOException e) {
            throw new ToolExecutionException("Failed to convert issue to map: " + e.getMessage());
        }
    }
}

