# 🎯 Правильные шаблоны GitHub Actions Tools для MCP Server

## ⚠️ Важно!

Твой MCP Server использует `ToolDefinition` и `getDefinition()`, не `getInputSchema()`!

---

## 📝 Шаблон 1: TriggerWorkflowTool.java

Создай файл: `backend/mcp-server/src/main/java/de/jivz/mcp/tools/git/TriggerWorkflowTool.java`

```java
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
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class TriggerWorkflowTool extends GitToolBase implements Tool {

    private static final String NAME = "trigger_workflow";

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

        log.info("🔧 Triggering workflow: {} on ref: {}", workflowFileName, ref);

        try {
            GitHub github = getGitHub();
            GHWorkflow workflow = github.getRepository(getRepository())
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
}
```

---

## 📝 Шаблон 2: ListCommitsTool.java

```java
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
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class ListCommitsTool extends GitToolBase implements Tool {

    private static final String NAME = "list_commits";

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

        log.info("🔧 Listing {} commits", limit);

        try {
            GitHub github = getGitHub();
            GHRepository repo = github.getRepository(getRepository());

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
}
```

---

## 📝 Шаблон 3: GitCommitTool.java

```java
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

        log.info("🔧 Creating commit: {}", message);

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
```

---

## 📝 Шаблон 4: GitAddTool.java

```java
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

        log.info("🔧 Staging files: {}", path);

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
```

---

## ✅ Checklist для добавления

1. ⬜ Создай 4 файла в `backend/mcp-server/src/main/java/de/jivz/mcp/tools/git/`
2. ⬜ Скопируй код из шаблонов выше
3. ⬜ Собери: `mvn clean package -DskipTests`
4. ⬜ Перезапусти MCP Server
5. ⬜ Проверь: `curl http://localhost:8081/mcp/tools`
6. ⬜ Протестируй из CLI: `> deploy team-service`

---

**Готово! Используй эти правильные шаблоны для твоего MCP Server!** 🚀

