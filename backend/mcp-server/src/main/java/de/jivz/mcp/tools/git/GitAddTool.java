package de.jivz.mcp.tools.git;
import de.jivz.mcp.model.InputSchema;
import de.jivz.mcp.model.PropertyDefinition;
import de.jivz.mcp.model.ToolDefinition;
import de.jivz.mcp.tools.Tool;
import de.jivz.mcp.tools.ToolArguments;
import de.jivz.mcp.tools.ToolExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
@Slf4j
public class GitAddTool extends GitToolBase implements Tool {
    private static final String NAME = "git_add";
    @Override
    public String getName() {
        return NAME;
    }
    @Override
    public ToolDefinition getDefinition() {
        Map<String, PropertyDefinition> properties = new LinkedHashMap<>();
        properties.put("path", PropertyDefinition.builder()
                .type("string")
                .description("Path to add (use '.' for all files)")
                .build());
        return ToolDefinition.builder()
                .name(NAME)
                .description("Stage files for git commit (git add)")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .build())
                .build();
    }
    @Override
    public Object execute(Map<String, Object> arguments) {
        ToolArguments args = ToolArguments.of(arguments);
        String path = args.getString("path", ".");
        log.info("Staging files: {}", path);
        try (Git git = getGitRepository()) {
            if (".".equals(path)) {
                git.add().addFilepattern(".").call();
            } else {
                git.add().addFilepattern(path).call();
            }
            Status status = git.status().call();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("path", path);
            result.put("added", status.getAdded().size());
            result.put("changed", status.getChanged().size());
            result.put("message", "Files staged successfully");
            return result;
        } catch (Exception e) {
            log.error("Failed to stage files: {}", e.getMessage(), e);
            throw new ToolExecutionException("Failed to stage files: " + e.getMessage(), e);
        }
    }
}
