package de.jivz.mcp.tools.git;
import de.jivz.mcp.model.InputSchema;
import de.jivz.mcp.model.PropertyDefinition;
import de.jivz.mcp.model.ToolDefinition;
import de.jivz.mcp.tools.Tool;
import de.jivz.mcp.tools.ToolArguments;
import de.jivz.mcp.tools.ToolExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHWorkflow;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
@Component
@Slf4j
public class TriggerWorkflowTool extends GitToolBase implements Tool {
    private static final String NAME = "trigger_workflow";

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
        properties.put("workflow", PropertyDefinition.builder()
                .type("string")
                .description("Workflow file name (e.g., deploy.yml)")
                .build());
        properties.put("ref", PropertyDefinition.builder()
                .type("string")
                .description("Git ref (branch or tag)")
                .build());
        return ToolDefinition.builder()
                .name(NAME)
                .description("Trigger a GitHub Actions workflow")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .required(List.of("workflow", "ref"))
                        .build())
                .build();
    }
    @Override
    public Object execute(Map<String, Object> arguments) {
        ToolArguments args = ToolArguments.of(arguments);
        String workflowFileName = args.getRequiredString("workflow");
        String ref = args.getString("ref", "main");
        String repository = (String) arguments.getOrDefault("repository", defaultRepository);

        log.info("🔧 Triggering workflow: {} on ref: {}", workflowFileName, ref);
        try {
            GitHub github = connectToGitHub();
            GHWorkflow workflow = github.getRepository(repository)
                .getWorkflow(workflowFileName);
            if (workflow == null) {
                throw new ToolExecutionException("Workflow not found: " + workflowFileName);
            }
            workflow.dispatch(ref);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("workflow", workflowFileName);
            result.put("ref", ref);
            result.put("message", "Workflow triggered successfully");
            return result;
        } catch (Exception e) {
            log.error("Failed to trigger workflow: {}", e.getMessage(), e);
            throw new ToolExecutionException("Failed to trigger workflow: " + e.getMessage(), e);
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
}
