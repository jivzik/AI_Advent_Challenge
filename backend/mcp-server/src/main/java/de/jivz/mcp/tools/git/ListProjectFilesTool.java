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
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Инструмент для получения списка файлов в директории проекта.
 */
@Component
@Slf4j
public class ListProjectFilesTool extends GitToolBase implements Tool {

    private static final String NAME = "list_project_files";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ToolDefinition getDefinition() {
        Map<String, PropertyDefinition> properties = new LinkedHashMap<>();

        properties.put("directory", PropertyDefinition.builder()
                .type("string")
                .description("Путь к директории (по умолчанию '.')")
                .build());

        properties.put("recursive", PropertyDefinition.builder()
                .type("boolean")
                .description("Рекурсивно обходить поддиректории (по умолчанию false)")
                .build());

        properties.put("extensions", PropertyDefinition.builder()
                .type("array")
                .description("Фильтр по расширениям файлов, например [\"java\", \"md\"]")
                .build());

        return ToolDefinition.builder()
                .name(NAME)
                .description("Получить список файлов в директории проекта")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .build())
                .build();
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        ToolArguments args = ToolArguments.of(arguments);
        String directory = args.getString("directory", ".");
        boolean recursive = args.getBoolean("recursive", false);
        List<String> extensions = args.getList("extensions", new ArrayList<>());

        log.info("🔧 Выполнение {}: directory='{}', recursive={}, extensions={}",
                NAME, directory, recursive, extensions);

        try {
            // Валидация и получение пути к директории
            Path dirPath = getValidatedDirectory(directory);

            // Получение списка файлов
            List<String> files;
            if (recursive) {
                files = listFilesRecursive(dirPath, extensions);
            } else {
                files = listFilesNonRecursive(dirPath, extensions);
            }

            log.info("✅ Найдено {} файлов в директории '{}'", files.size(), directory);

            return files;

        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Ошибка при получении списка файлов в директории: {}", directory, e);
            throw new ToolExecutionException("Ошибка при получении списка файлов: " + e.getMessage());
        }
    }

    /**
     * Получить и валидировать путь к директории.
     */
    private Path getValidatedDirectory(String directory) {
        Path projectPath = Paths.get(projectRoot).toAbsolutePath().normalize();
        Path dirPath = projectPath.resolve(directory).normalize();

        // Проверка path traversal
        if (directory.contains("..")) {
            log.warn("Попытка path traversal: {}", directory);
            throw new ToolExecutionException("Путь содержит запрещенные символы (..)");
        }

        // Проверка, что директория внутри проекта
        if (!dirPath.startsWith(projectPath)) {
            log.warn("Попытка доступа за пределами проекта: {}", dirPath);
            throw new ToolExecutionException("Директория должна находиться внутри проекта");
        }

        // Проверка существования директории
        if (!Files.exists(dirPath)) {
            throw new ToolExecutionException("Директория не найдена: " + dirPath);
        }

        if (!Files.isDirectory(dirPath)) {
            throw new ToolExecutionException("Путь не является директорией: " + dirPath);
        }

        return dirPath;
    }

    /**
     * Получить список файлов без рекурсии.
     */
    private List<String> listFilesNonRecursive(Path dirPath, List<String> extensions) throws IOException {
        Path projectPath = Paths.get(projectRoot).toAbsolutePath().normalize();

        try (Stream<Path> stream = Files.list(dirPath)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> matchesExtensions(p, extensions))
                    .map(p -> projectPath.relativize(p).toString())
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    /**
     * Получить список файлов рекурсивно.
     */
    private List<String> listFilesRecursive(Path dirPath, List<String> extensions) throws IOException {
        Path projectPath = Paths.get(projectRoot).toAbsolutePath().normalize();

        try (Stream<Path> stream = Files.walk(dirPath)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> !isExcludedDirectory(p))
                    .filter(p -> matchesExtensions(p, extensions))
                    .map(p -> projectPath.relativize(p).toString())
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    /**
     * Проверка соответствия расширению файла.
     */
    private boolean matchesExtensions(Path path, List<String> extensions) {
        if (extensions == null || extensions.isEmpty()) {
            return true;
        }

        String filename = path.getFileName().toString().toLowerCase();
        return extensions.stream()
                .anyMatch(ext -> {
                    String normalizedExt = ext.startsWith(".") ? ext : "." + ext;
                    return filename.endsWith(normalizedExt.toLowerCase());
                });
    }
}

