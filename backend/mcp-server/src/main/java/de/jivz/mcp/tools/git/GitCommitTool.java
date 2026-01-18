package de.jivz.mcp.tools.git;
import de.jivz.mcp.model.InputSchema;
import de.jivz.mcp.model.PropertyDefinition;
import de.jivz.mcp.model.ToolDefinition;
import de.jivz.mcp.tools.Tool;
import de.jivz.mcp.tools.ToolArguments;
import de.jivz.mcp.tools.ToolExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
@Slf4j
public class GitCommitTool extends GitToolBase implements Tool {
    private static final String NAME = "git_commit";
    @Override
    public String getName() {
        return NAME;
    }
    @Override
    public ToolDefinition getDefinition() {
        Map<String, PropertyDefinition> properties = new LinkedHashMap<>();
        properties.put("message", PropertyDefinition.builder()
                .type("string")
                .description("Commit message")
                .build());
        return ToolDefinition.builder()
                .name(NAME)
                .description("Create a git commit with staged changes")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .required(List.of("message"))
                        .build())
                .build();
    }
    @Override
    public Object execute(Map<String, Object> arguments) {
        ToolArguments args = ToolArguments.of(arguments);
        String message = args.getRequiredString("message");
        log.info("Creating commit: {}", message);
        try (Git git = getGitRepository()) {
            RevCommit commit = git.commit()
                .setMessage(message)
                .call();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("sha", commit.getName());
            result.put("message", commit.getShortMessage());
            result.put("author", commit.getAuthorIdent().getName());
            return result;
        } catch (Exception e) {
            log.error("Failed to create commit: {}", e.getMessage(), e);
            throw new ToolExecutionException("Failed to create commit: " + e.getMessage(), e);
        }
    }
}
