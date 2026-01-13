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

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Инструмент для получения истории коммитов Git.
 */
@Component
@Slf4j
public class GetGitLogTool extends GitToolBase implements Tool {

    private static final String NAME = "get_git_log";
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ToolDefinition getDefinition() {
        Map<String, PropertyDefinition> properties = new LinkedHashMap<>();

        properties.put("limit", PropertyDefinition.builder()
                .type("integer")
                .description("Количество коммитов (по умолчанию 10, максимум 50)")
                .build());

        return ToolDefinition.builder()
                .name(NAME)
                .description("Получить последние коммиты из истории Git")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .build())
                .build();
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        ToolArguments args = ToolArguments.of(arguments);
        int limit = args.getInt("limit", DEFAULT_LIMIT);

        // Валидация лимита
        if (limit <= 0) {
            limit = DEFAULT_LIMIT;
        }
        if (limit > MAX_LIMIT) {
            limit = MAX_LIMIT;
        }

        log.info("🔧 Выполнение {}: получение {} коммитов", NAME, limit);

        try (Git git = getGitRepository()) {
            Iterable<RevCommit> commits = git.log().setMaxCount(limit).call();

            List<Map<String, String>> commitList = new ArrayList<>();

            for (RevCommit commit : commits) {
                Map<String, String> commitInfo = new LinkedHashMap<>();
                commitInfo.put("hash", commit.getName());
                commitInfo.put("author", commit.getAuthorIdent().getName());
                commitInfo.put("date", DATE_FORMAT.format(new Date(commit.getCommitTime() * 1000L)));
                commitInfo.put("message", commit.getShortMessage());

                commitList.add(commitInfo);
            }

            log.info("✅ Получено {} коммитов", commitList.size());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("commits", commitList);

            return result;

        } catch (Exception e) {
            log.error("❌ Ошибка при получении истории коммитов", e);
            throw new ToolExecutionException("Ошибка при получении истории коммитов: " + e.getMessage());
        }
    }
}

