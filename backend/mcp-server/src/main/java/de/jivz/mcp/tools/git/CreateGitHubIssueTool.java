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
 * Tool zum Erstellen von GitHub Issues.
 */
@Component
@Slf4j
public class CreateGitHubIssueTool implements Tool {

    private static final String NAME = "create_github_issue";

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

        properties.put("title", PropertyDefinition.builder()
                .type("string")
                .description("Title of the issue (required)")
                .build()
        );

        properties.put("body", PropertyDefinition.builder()
                .type("string")
                .description("Body/description of the issue (optional)")
                .build()
        );

        properties.put("labels", PropertyDefinition.builder()
                .type("array")
                .description("Array of label names to apply (optional)")
                .build()
        );

        properties.put("assignees", PropertyDefinition.builder()
                .type("array")
                .description("Array of GitHub usernames to assign (optional)")
                .build()
        );

        properties.put("milestone", PropertyDefinition.builder()
                .type("integer")
                .description("Milestone number to assign (optional)")
                .build()
        );

        return ToolDefinition.builder()
                .name(NAME)
                .description("Create a new issue in a GitHub repository. Returns the created issue details including number and URL.")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .required(List.of("title"))
                        .build())
                .build();
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        log.info("🔧 Executing {}: creating GitHub issue", NAME);

        // Validate required parameters
        String title = (String) arguments.get("title");
        if (title == null || title.isBlank()) {
            throw new ToolExecutionException("Title is required");
        }

        // Get repository parameter
        String repository = (String) arguments.getOrDefault("repository", defaultRepository);
        if (repository == null || repository.isBlank()) {
            throw new ToolExecutionException("Repository parameter is required. Provide 'repository' or configure 'github.repository' property.");
        }

        // Get optional parameters
        String body = (String) arguments.getOrDefault("body", "");
        @SuppressWarnings("unchecked")
        List<String> labels = (List<String>) arguments.getOrDefault("labels", new ArrayList<>());
        @SuppressWarnings("unchecked")
        List<String> assignees = (List<String>) arguments.getOrDefault("assignees", new ArrayList<>());
        Integer milestoneNumber = arguments.containsKey("milestone")
                ? ((Number) arguments.get("milestone")).intValue()
                : null;

        try {
            // Connect to GitHub
            GitHub github = connectToGitHub();
            GHRepository repo = github.getRepository(repository);
            log.info("📦 Connected to repository: {}", repository);

            // Create issue builder
            GHIssueBuilder issueBuilder = repo.createIssue(title);

            if (body != null && !body.isBlank()) {
                issueBuilder.body(body);
            }

            // Create the issue
            GHIssue issue = issueBuilder.create();
            log.info("✅ Issue created with number: #{}", issue.getNumber());

            // Apply labels if provided
            if (!labels.isEmpty()) {
                try {
                    issue.setLabels(labels.toArray(new String[0]));
                    log.info("🏷️  Applied {} label(s)", labels.size());
                } catch (IOException e) {
                    log.warn("⚠️  Failed to apply labels: {}", e.getMessage());
                }
            }

            // Assign users if provided
            if (!assignees.isEmpty()) {
                try {
                    for (String assignee : assignees) {
                        GHUser user = github.getUser(assignee);
                        issue.assignTo(user);
                    }
                    log.info("👤 Assigned {} user(s)", assignees.size());
                } catch (IOException e) {
                    log.warn("⚠️  Failed to assign users: {}", e.getMessage());
                }
            }

            // Set milestone if provided
            if (milestoneNumber != null) {
                try {
                    GHMilestone milestone = repo.getMilestone(milestoneNumber);
                    issue.setMilestone(milestone);
                    log.info("🎯 Set milestone: {}", milestone.getTitle());
                } catch (IOException e) {
                    log.warn("⚠️  Failed to set milestone: {}", e.getMessage());
                }
            }

            // Return issue details
            Map<String, Object> result = convertIssueToMap(issue);
            log.info("✅ Successfully created issue #{}", issue.getNumber());
            return result;

        } catch (IOException e) {
            log.error("❌ Error creating GitHub issue", e);
            throw new ToolExecutionException("Failed to create issue: " + e.getMessage());
        }
    }

    /**
     * Connect to GitHub using configured token
     */
    private GitHub connectToGitHub() throws IOException {
        if (githubToken == null || githubToken.isBlank()) {
            throw new ToolExecutionException("GitHub token is required for creating issues. Configure 'github.token' property.");
        }

        log.debug("🔐 Connecting to GitHub with token authentication");
        return new GitHubBuilder()
                .withOAuthToken(githubToken)
                .build();
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

