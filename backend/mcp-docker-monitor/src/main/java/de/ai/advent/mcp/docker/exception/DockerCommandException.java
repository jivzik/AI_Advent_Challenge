package de.ai.advent.mcp.docker.exception;

/**
 * Exception для ошибок выполнения Docker команд
 *
 * Выбрасывается при ошибках выполнения docker команд,
 * когда контейнер не найден или команда вернула ошибку
 */
public class DockerCommandException extends RuntimeException {

    private String stderr;
    private int exitCode;

    public DockerCommandException(String message) {
        super(message);
        this.stderr = "";
        this.exitCode = -1;
    }

    public DockerCommandException(String message, String stderr) {
        super(message);
        this.stderr = stderr != null ? stderr : "";
        this.exitCode = -1;
    }

    public DockerCommandException(String message, String stderr, int exitCode) {
        super(message);
        this.stderr = stderr != null ? stderr : "";
        this.exitCode = exitCode;
    }

    public DockerCommandException(String message, Throwable cause) {
        super(message, cause);
        this.stderr = "";
        this.exitCode = -1;
    }

    public String getStderr() {
        return stderr;
    }

    public int getExitCode() {
        return exitCode;
    }

    public boolean hasStderr() {
        return stderr != null && !stderr.isEmpty();
    }
}

