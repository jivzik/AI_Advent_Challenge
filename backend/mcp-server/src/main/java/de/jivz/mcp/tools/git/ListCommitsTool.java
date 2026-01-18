package de.jivz.mcp.tools.git;
import de.jivz.mcp.model.InputSchema;
import de.jivz.mcp.model.PropertyDefinition;
import de.jivz.mcp.model.ToolDefinition;
import de.jivz.mcp.tools.Tool;
import de.jivz.mcp.tools.ToolArguments;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.*;
@Component
@Slf4j
public class ListCommitsTool implements Tool {
    private static final String NAME = "list_commits";
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
        properties.put("limit", PropertyDefinition.builder()
                .type("integer")
                .description("Number of commits to return (default: 30)")
                .build());
        return ToolDefinition.builder()
                .name(NAME)
                .description("List recent commits from the repository")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .build())
                .build();
    }
    @Override
    public Object execute(Map<String, Object> arguments) {
        ToolArguments args = ToolArguments.of(arguments);
        int limit = args.getInt("limit", 30);
        log.info("Listing {} commits", limit);
        try {
            GitHub github = connectToGitHub();
            GHRepository repo = github.getRepository(defaultRepository);
            List<GHCommit> commits = repo.listCommits().toList();
            List<Map<String, Object>> commitList = new ArrayList<>();
            int count = 0;
            for (GHCommit commit : commits) {
                if (count >= limit) break;
                Map<String, Object> commitData = new LinkedHashMap<>();
                commitData.put("sha", commit.getSHA1());
                commitData.put("html_url", commit.getHtmlUrl().toString());
                Map<String, Object> commitDetail = new LinkedHashMap<>();
                commitDetail.put("message", commit.getCommitShortInfo().getMessage());
                Map<String, String> author = new LinkedHashMap<>();
                if (commit.getCommitShortInfo().getAuthor() != null) {
                    author.put("name", commit.getCommitShortInfo().getAuthor().getName());
                    author.put("email", commit.getCommitShortInfo().getAuthor().getEmail());
                    author.put("date", commit.getCommitDate().toString());
                }
                commitDetail.put("author", author);
                commitData.put("commit", commitDetail);
                commitList.add(commitData);
                count++;
            }
            return commitList;
        } catch (Exception e) {
            log.error("Failed to list commits: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    private GitHub connectToGitHub() throws IOException {
        return GitHub.connectUsingOAuth(githubToken);
    }
}
