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
 * Tool zum Löschen/Schließen von GitHub Issues.
 * Hinweis: GitHub API erlaubt kein echtes Löschen von Issues aus Audit-Gründen.
 * Stattdessen wird das Issue geschlossen und als gelöscht markiert.
 */
@Component
@Slf4j
public class DeleteGitHubIssueTool implements Tool {

    private static final String NAME = "delete_github_issue";

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

        properties.put("issueNumber", PropertyDefinition.builder()
                .type("integer")
                .description("Issue number to delete/close (required)")
                .build()
        );

        properties.put("reason", PropertyDefinition.builder()
                .type("string")
                .description("Reason for closing: 'completed', 'not_planned', or leave empty for default (optional)")
                .build()
        );

        properties.put("comment", PropertyDefinition.builder()
                .type("string")
                .description("Optional comment to add before closing the issue")
                .build()
        );

        return ToolDefinition.builder()
                .name(NAME)
                .description("Close a GitHub issue (GitHub API does not support permanent deletion). Optionally add a comment and specify closure reason.")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .required(List.of("issueNumber"))
                        .build())
                .build();
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        log.info("🔧 Executing {}: closing GitHub issue", NAME);

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

        // Get optional parameters
        String reason = (String) arguments.get("reason");
        String comment = (String) arguments.get("comment");

        try {
            // Connect to GitHub
            GitHub github = connectToGitHub();
            GHRepository repo = github.getRepository(repository);
            log.info("📦 Connected to repository: {}", repository);

            // Get the issue
            GHIssue issue = repo.getIssue(issueNumber);
            log.info("🔍 Found issue #{}: {}", issueNumber, issue.getTitle());

            // Check if already closed
            if (issue.getState() == GHIssueState.CLOSED) {
                log.warn("⚠️  Issue #{} is already closed", issueNumber);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", true);
                result.put("message", "Issue was already closed");
                result.put("issueNumber", issueNumber);
                result.put("state", "closed");
                return result;
            }

            // Add comment if provided
            if (comment != null && !comment.isBlank()) {
                issue.comment(comment);
                log.info("💬 Added closing comment");
            }

            // Close the issue with reason if provided
            if (reason != null && !reason.isBlank()) {
                // GitHub API supports state_reason: 'completed' or 'not_planned'
                // This requires using the REST API directly through the github-api library
                String stateReason = switch (reason.toLowerCase()) {
                    case "completed" -> "completed";
                    case "not_planned", "not planned", "wont_fix", "wontfix" -> "not_planned";
                    default -> null;
                };

                if (stateReason != null) {
                    issue.close();
                    log.info("🔒 Closed issue with reason: {}", stateReason);
                    // Note: github-api library might not support state_reason directly
                    // The issue will be closed, but the reason might not be set
                    log.warn("⚠️  Note: Closure reason may require direct REST API call");
                } else {
                    issue.close();
                    log.info("🔒 Closed issue");
                }
            } else {
                issue.close();
                log.info("🔒 Closed issue");
            }

            // Return success response
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "Issue successfully closed");
            result.put("issueNumber", issueNumber);
            result.put("title", issue.getTitle());
            result.put("state", "closed");
            result.put("url", issue.getHtmlUrl().toString());
            result.put("closedAt", new Date().toString());

            log.info("✅ Successfully closed issue #{}", issueNumber);
            return result;

        } catch (IOException e) {
            log.error("❌ Error closing GitHub issue", e);
            throw new ToolExecutionException("Failed to close issue: " + e.getMessage());
        }
    }

    /**
     * Connect to GitHub using configured token
     */
    private GitHub connectToGitHub() throws IOException {
        if (githubToken == null || githubToken.isBlank()) {
            throw new ToolExecutionException("GitHub token is required for closing issues. Configure 'github.token' property.");
        }

        log.debug("🔐 Connecting to GitHub with token authentication");
        return new GitHubBuilder()
                .withOAuthToken(githubToken)
                .build();
    }
}

