package de.jivz.mcp.tools.git;

import de.jivz.mcp.model.InputSchema;
import de.jivz.mcp.model.ToolDefinition;
import de.jivz.mcp.tools.Tool;
import de.jivz.mcp.tools.ToolExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Инструмент для получения текущей Git-ветки.
 */
@Component
@Slf4j
public class GetCurrentBranchTool extends GitToolBase implements Tool {

    private static final String NAME = "get_current_branch";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name(NAME)
                .description("Получить название текущей Git ветки")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(new LinkedHashMap<>())
                        .build())
                .build();
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        log.info("🔧 Выполнение {}: получение текущей ветки", NAME);

        try (Git git = getGitRepository()) {
            String branch = git.getRepository().getBranch();

            if (branch == null) {
                throw new ToolExecutionException("Не удалось определить текущую ветку");
            }

            log.info("✅ Текущая ветка: {}", branch);

            Map<String, String> result = new LinkedHashMap<>();
            result.put("branch", branch);

            return result;

        } catch (Exception e) {
            log.error("❌ Ошибка при получении текущей ветки", e);
            throw new ToolExecutionException("Ошибка при получении текущей ветки: " + e.getMessage());
        }
    }
}

