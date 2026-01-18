package de.jivz.mcp.tools.git;

import de.jivz.mcp.tools.ToolExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Базовый класс для Git-инструментов.
 * Содержит общую логику для работы с Git-репозиторием, GitHub API и файловой системой.
 */
@Slf4j
public abstract class GitToolBase {

    protected static final long MAX_FILE_SIZE = 1_048_576L; // 1MB

    protected static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".java", ".kt", ".ts", ".vue", ".js", ".md", ".txt",
            ".json", ".yml", ".yaml", ".properties", ".xml"
    );

    protected static final List<String> EXCLUDED_DIRS = Arrays.asList(
            ".git", "node_modules", "target", "dist", "build", ".idea", ".vscode"
    );

    @Value("${git.project.root:#{systemProperties['user.dir']}}")
    protected String projectRoot;

    @Value("${personal.github.token}")
    protected String githubToken;

    @Value("${personal.github.repository}")
    protected String defaultRepository;

    /**
     * Подключение к GitHub API с использованием токена.
     * Используется всеми GitHub-инструментами для единообразной аутентификации.
     */
    protected GitHub connectToGitHub() throws IOException {
        if (githubToken != null && !githubToken.isBlank()) {
            log.debug("🔐 Connecting to GitHub with token authentication");
            return new GitHubBuilder()
                    .withOAuthToken(githubToken)
                    .build();
        } else {
            log.warn("⚠️ Connecting to GitHub without authentication (rate limits apply)");
            return GitHub.connectAnonymously();
        }
    }

    /**
     * Получить имя репозитория (default или переданное).
     */
    protected String getRepository(String customRepository) {
        if (customRepository != null && !customRepository.isBlank()) {
            return customRepository;
        }
        if (defaultRepository == null || defaultRepository.isBlank()) {
            throw new ToolExecutionException("Repository not specified and no default repository configured");
        }
        return defaultRepository;
    }

    /**
     * Получить Git-репозиторий.
     */
    protected Git getGitRepository() {
        try {
            File projectDir = new File(projectRoot);
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = builder
                    .setGitDir(new File(projectDir, ".git"))
                    .readEnvironment()
                    .findGitDir()
                    .build();

            if (repository.getDirectory() == null) {
                throw new ToolExecutionException("Git-репозиторий не найден в директории: " + projectRoot);
            }

            return new Git(repository);
        } catch (IOException e) {
            log.error("Ошибка при инициализации Git-репозитория", e);
            throw new ToolExecutionException("Не удалось открыть Git-репозиторий: " + e.getMessage());
        }
    }

    /**
     * Валидация пути к файлу.
     *
     * @param relativePath относительный путь к файлу
     * @return абсолютный путь к файлу
     * @throws ToolExecutionException если путь невалидный
     */
    protected Path validateFilePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new ToolExecutionException("Путь к файлу не может быть пустым");
        }

        // Проверка на path traversal
        if (relativePath.contains("..")) {
            log.warn("Попытка path traversal: {}", relativePath);
            throw new ToolExecutionException("Путь содержит запрещенные символы (..)");
        }

        // Проверка на абсолютный путь
        Path path = Paths.get(relativePath);
        if (path.isAbsolute()) {
            log.warn("Попытка использования абсолютного пути: {}", relativePath);
            throw new ToolExecutionException("Абсолютные пути запрещены");
        }

        // Формирование полного пути
        Path projectPath = Paths.get(projectRoot).toAbsolutePath().normalize();
        Path fullPath = projectPath.resolve(relativePath).normalize();

        // Проверка, что файл внутри проекта
        if (!fullPath.startsWith(projectPath)) {
            log.warn("Попытка доступа за пределами проекта: {}", fullPath);
            throw new ToolExecutionException("Файл должен находиться внутри проекта");
        }

        // Проверка существования файла
        if (!Files.exists(fullPath)) {
            throw new ToolExecutionException("Файл не найден: " + fullPath);
        }

        // Проверка прав на чтение
        if (!Files.isReadable(fullPath)) {
            log.warn("Нет прав на чтение файла: {}", fullPath);
            throw new ToolExecutionException("Нет прав на чтение файла: " + fullPath);
        }

        log.debug("Путь к файлу валиден: {}", fullPath);
        return fullPath;
    }

    /**
     * Проверка расширения файла.
     */
    protected boolean isAllowedExtension(String filename) {
        return ALLOWED_EXTENSIONS.stream()
                .anyMatch(ext -> filename.toLowerCase().endsWith(ext));
    }

    /**
     * Проверка, что директория не исключена.
     */
    protected boolean isExcludedDirectory(Path path) {
        String pathStr = path.toString();
        return EXCLUDED_DIRS.stream()
                .anyMatch(excluded -> pathStr.contains(File.separator + excluded + File.separator)
                        || pathStr.endsWith(File.separator + excluded)
                        || pathStr.contains(excluded + File.separator));
    }

    /**
     * Проверка размера файла.
     */
    protected void validateFileSize(Path path) throws IOException {
        long size = Files.size(path);
        if (size > MAX_FILE_SIZE) {
            throw new ToolExecutionException(
                    String.format("Файл слишком большой: %d байт (максимум: %d байт)", size, MAX_FILE_SIZE)
            );
        }
    }
}
