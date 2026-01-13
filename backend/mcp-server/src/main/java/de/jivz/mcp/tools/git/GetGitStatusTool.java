package de.jivz.mcp.tools.git;

import de.jivz.mcp.model.InputSchema;
import de.jivz.mcp.model.ToolDefinition;
import de.jivz.mcp.tools.Tool;
import de.jivz.mcp.tools.ToolExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Инструмент для получения статуса Git-репозитория.
 */
@Component
@Slf4j
public class GetGitStatusTool extends GitToolBase implements Tool {

    private static final String NAME = "get_git_status";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name(NAME)
                .description("Получить статус репозитория (измененные, добавленные, неотслеживаемые файлы)")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(new LinkedHashMap<>())
                        .build())
                .build();
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        log.info("🔧 Выполнение {}: получение статуса репозитория", NAME);

        try (Git git = getGitRepository()) {
            Status status = git.status().call();

            Map<String, Object> result = new LinkedHashMap<>();

            // Измененные файлы
            List<String> modified = new ArrayList<>();
            modified.addAll(status.getModified());
            modified.addAll(status.getChanged());
            result.put("modified", modified.stream().sorted().collect(Collectors.toList()));

            // Добавленные файлы
            result.put("added", status.getAdded().stream().sorted().collect(Collectors.toList()));

            // Неотслеживаемые файлы
            result.put("untracked", status.getUntracked().stream().sorted().collect(Collectors.toList()));

            // Удаленные файлы
            List<String> deleted = new ArrayList<>();
            deleted.addAll(status.getMissing());
            deleted.addAll(status.getRemoved());
            result.put("deleted", deleted.stream().sorted().collect(Collectors.toList()));

            log.info("✅ Статус получен: modified={}, added={}, untracked={}, deleted={}",
                    modified.size(),
                    status.getAdded().size(),
                    status.getUntracked().size(),
                    deleted.size());

            return result;

        } catch (Exception e) {
            log.error("❌ Ошибка при получении статуса репозитория", e);
            throw new ToolExecutionException("Ошибка при получении статуса репозитория: " + e.getMessage());
        }
    }
}

