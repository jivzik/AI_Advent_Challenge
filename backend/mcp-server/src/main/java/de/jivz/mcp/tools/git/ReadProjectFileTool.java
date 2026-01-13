package de.jivz.mcp.tools.git;

import de.jivz.mcp.model.InputSchema;
import de.jivz.mcp.model.PropertyDefinition;
import de.jivz.mcp.model.ToolDefinition;
import de.jivz.mcp.tools.Tool;
import de.jivz.mcp.tools.ToolArguments;
import de.jivz.mcp.tools.ToolExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Инструмент для чтения содержимого файла из проекта.
 */
@Component
@Slf4j
public class ReadProjectFileTool extends GitToolBase implements Tool {

    private static final String NAME = "read_project_file";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ToolDefinition getDefinition() {
        Map<String, PropertyDefinition> properties = new LinkedHashMap<>();

        properties.put("filePath", PropertyDefinition.builder()
                .type("string")
                .description("Относительный путь к файлу от корня проекта")
                .build());

        return ToolDefinition.builder()
                .name(NAME)
                .description("Прочитать содержимое файла из проекта")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .required(List.of("filePath"))
                        .build())
                .build();
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        ToolArguments args = ToolArguments.of(arguments);
        String filePath = args.getString("filePath", "");

        log.info("🔧 Выполнение {}: чтение файла '{}'", NAME, filePath);

        try {
            // Валидация пути
            Path validatedPath = validateFilePath(filePath);

            // Проверка расширения файла
            String filename = validatedPath.getFileName().toString();
            if (!isAllowedExtension(filename)) {
                log.warn("Попытка чтения файла с недопустимым расширением: {}", filename);
                throw new ToolExecutionException(
                        "Тип файла не поддерживается. Разрешенные расширения: " + ALLOWED_EXTENSIONS
                );
            }

            // Проверка размера файла
            validateFileSize(validatedPath);

            // Чтение содержимого файла
            String content = Files.readString(validatedPath);
            long size = Files.size(validatedPath);

            log.info("✅ Файл прочитан: {} ({} байт)", filePath, size);
            log.debug("Аудит: Чтение файла {} пользователем system", validatedPath);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", content);
            result.put("path", filePath);
            result.put("size", size);

            return result;

        } catch (IOException e) {
            log.error("❌ Ошибка при чтении файла: {}", filePath, e);
            throw new ToolExecutionException("Ошибка при чтении файла: " + e.getMessage());
        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Неожиданная ошибка при чтении файла: {}", filePath, e);
            throw new ToolExecutionException("Неожиданная ошибка: " + e.getMessage());
        }
    }
}

