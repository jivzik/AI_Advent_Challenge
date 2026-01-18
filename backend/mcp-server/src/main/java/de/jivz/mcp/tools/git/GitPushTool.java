package de.jivz.mcp.tools.git;
import de.jivz.mcp.model.InputSchema;
import de.jivz.mcp.model.PropertyDefinition;
import de.jivz.mcp.model.ToolDefinition;
import de.jivz.mcp.tools.Tool;
import de.jivz.mcp.tools.ToolArguments;
import de.jivz.mcp.tools.ToolExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
@Slf4j
public class GitPushTool extends GitToolBase implements Tool {
    private static final String NAME = "git_push";


    @Override
    public String getName() {
        return NAME;
    }


    @Override
    public ToolDefinition getDefinition() {
        Map<String, PropertyDefinition> properties = new LinkedHashMap<>();
        properties.put("remote", PropertyDefinition.builder()
                .type("string")
                .description("Remote name (default: origin)")
                .build());
        properties.put("branch", PropertyDefinition.builder()
                .type("string")
                .description("Branch name to push (default: current branch)")
                .build());
        properties.put("force", PropertyDefinition.builder()
                .type("boolean")
                .description("Force push (default: false)")
                .build());
        return ToolDefinition.builder()
                .name(NAME)
                .description("Push commits to remote repository (git push)")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .build())
                .build();
    }
    @Override
    public Object execute(Map<String, Object> arguments) {
        ToolArguments args = ToolArguments.of(arguments);
        String remote = args.getString("remote", "origin");
        String branch = args.getString("branch", null);
        boolean force = args.getBoolean("force", false);
        log.info("Pushing to remote: {}, branch: {}, force: {}", remote, branch, force);
        try (Git git = getGitRepository()) {
            // Create credentials provider with GitHub token for authentication
            UsernamePasswordCredentialsProvider credentialsProvider =
                new UsernamePasswordCredentialsProvider(githubToken, "");

            PushCommand pushCommand = git.push()
                    .setRemote(remote)
                    .setForce(force)
                    .setCredentialsProvider(credentialsProvider);

            if (branch != null && !branch.isEmpty()) {
                pushCommand.add(branch);
            }
            Iterable<PushResult> results = pushCommand.call();
            List<String> messages = new ArrayList<>();
            for (PushResult result : results) {
                result.getRemoteUpdates().forEach(update -> {
                    String msg = String.format("%s: %s -> %s (%s)",
                            update.getRemoteName(),
                            update.getSrcRef(),
                            update.getRemoteName(),
                            update.getStatus());
                    messages.add(msg);
                    log.info("Push result: {}", msg);
                });
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("remote", remote);
            result.put("branch", branch != null ? branch : "current");
            result.put("force", force);
            result.put("messages", messages);
            result.put("message", "Successfully pushed to " + remote);
            return result;
        } catch (Exception e) {
            log.error("Failed to push: {}", e.getMessage(), e);
            throw new ToolExecutionException("Failed to push: " + e.getMessage(), e);
        }
    }
}
