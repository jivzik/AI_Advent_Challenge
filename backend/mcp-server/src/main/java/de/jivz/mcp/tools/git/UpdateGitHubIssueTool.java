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
 * Tool zum Bearbeiten/Aktualisieren von GitHub Issues.
 */
@Component
@Slf4j
public class UpdateGitHubIssueTool implements Tool {

    private static final String NAME = "update_github_issue";

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

        properties.put("issueNumber", PropertyDefinition.builder()
                .type("integer")
                .description("Issue number to update (required)")
                .build()
        );

        properties.put("title", PropertyDefinition.builder()
                .type("string")
                .description("New title for the issue (optional)")
                .build()
        );

        properties.put("body", PropertyDefinition.builder()
                .type("string")
                .description("New body/description for the issue (optional)")
                .build()
        );

        properties.put("state", PropertyDefinition.builder()
                .type("string")
                .description("New state: 'open' or 'closed' (optional)")
                .build()
        );

        properties.put("labels", PropertyDefinition.builder()
                .type("array")
                .description("Array of label names to set (replaces existing labels, optional)")
                .build()
        );

        properties.put("assignees", PropertyDefinition.builder()
                .type("array")
                .description("Array of GitHub usernames to assign (replaces existing assignees, optional)")
                .build()
        );

        properties.put("milestone", PropertyDefinition.builder()
                .type("integer")
                .description("Milestone number to assign (optional, use -1 to remove milestone)")
                .build()
        );

        return ToolDefinition.builder()
                .name(NAME)
                .description("Update an existing GitHub issue. You can update title, body, state, labels, assignees, and milestone.")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .required(List.of("issueNumber"))
                        .build())
                .build();
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        log.info("🔧 Executing {}: updating GitHub issue", NAME);

        // Validate required parameters
        if (!arguments.containsKey("issueNumber")) {
            throw new ToolExecutionException("Issue number is required");
        }
        int issueNumber = ((Number) arguments.get("issueNumber")).intValue();

        // Get repository parameter
        String repository = (String) arguments.getOrDefault("repository", defaultRepository);
        if (repository == null || repository.isBlank()) {
            throw new ToolExecutionException("Repository parameter is required. Provide 'repository' or configure 'github.repository' property.");
        }

        try {
            // Connect to GitHub
            GitHub github = connectToGitHub();
            GHRepository repo = github.getRepository(repository);
            log.info("📦 Connected to repository: {}", repository);

            // Get the issue
            GHIssue issue = repo.getIssue(issueNumber);
            log.info("🔍 Found issue #{}: {}", issueNumber, issue.getTitle());

            // Update title if provided
            if (arguments.containsKey("title")) {
                String newTitle = (String) arguments.get("title");
                if (newTitle != null && !newTitle.isBlank()) {
                    issue.setTitle(newTitle);
                    log.info("✏️  Updated title");
                }
            }

            // Update body if provided
            if (arguments.containsKey("body")) {
                String newBody = (String) arguments.get("body");
                issue.setBody(newBody != null ? newBody : "");
                log.info("✏️  Updated body");
            }

            // Update state if provided
            if (arguments.containsKey("state")) {
                String state = (String) arguments.get("state");
                if (state != null) {
                    if (state.equalsIgnoreCase("closed")) {
                        issue.close();
                        log.info("🔒 Closed issue");
                    } else if (state.equalsIgnoreCase("open")) {
                        issue.reopen();
                        log.info("🔓 Reopened issue");
                    }
                }
            }

            // Update labels if provided
            if (arguments.containsKey("labels")) {
                @SuppressWarnings("unchecked")
                List<String> labels = (List<String>) arguments.get("labels");
                if (labels != null) {
                    issue.setLabels(labels.toArray(new String[0]));
                    log.info("🏷️  Updated labels to: {}", labels);
                }
            }

            // Update assignees if provided
            if (arguments.containsKey("assignees")) {
                @SuppressWarnings("unchecked")
                List<String> assignees = (List<String>) arguments.get("assignees");
                if (assignees != null) {
                    // Remove all current assignees
                    for (GHUser currentAssignee : issue.getAssignees()) {
                        issue.removeAssignees(currentAssignee);
                    }
                    // Add new assignees
                    for (String assignee : assignees) {
                        GHUser user = github.getUser(assignee);
                        issue.assignTo(user);
                    }
                    log.info("👤 Updated assignees to: {}", assignees);
                }
            }

            // Update milestone if provided
            if (arguments.containsKey("milestone")) {
                int milestoneNumber = ((Number) arguments.get("milestone")).intValue();
                if (milestoneNumber == -1) {
                    issue.setMilestone(null);
                    log.info("🎯 Removed milestone");
                } else {
                    GHMilestone milestone = repo.getMilestone(milestoneNumber);
                    issue.setMilestone(milestone);
                    log.info("🎯 Updated milestone to: {}", milestone.getTitle());
                }
            }

            // Return updated issue details
            Map<String, Object> result = convertIssueToMap(issue);
            log.info("✅ Successfully updated issue #{}", issueNumber);
            return result;

        } catch (IOException e) {
            log.error("❌ Error updating GitHub issue", e);
            throw new ToolExecutionException("Failed to update issue: " + e.getMessage());
        }
    }

    /**
     * Connect to GitHub using configured token
     */
    private GitHub connectToGitHub() throws IOException {
        if (githubToken == null || githubToken.isBlank()) {
            throw new ToolExecutionException("GitHub token is required for updating issues. Configure 'github.token' property.");
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

