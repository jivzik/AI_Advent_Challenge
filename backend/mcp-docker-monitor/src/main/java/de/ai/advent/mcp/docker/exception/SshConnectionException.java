package de.ai.advent.mcp.docker.exception;

/**
 * Exception для ошибок SSH подключения
 *
 * Выбрасывается при проблемах с SSH соединением,
 * аутентификацией или сетевыми ошибками
 */
public class SshConnectionException extends RuntimeException {

    public SshConnectionException(String message) {
        super(message);
    }

    public SshConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public SshConnectionException(Throwable cause) {
        super(cause);
    }
}

