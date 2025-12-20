package de.ai.advent.mcp.docker.controller;

import de.ai.advent.mcp.docker.exception.DockerCommandException;
import de.ai.advent.mcp.docker.exception.SshConnectionException;
import de.ai.advent.mcp.docker.model.ToolCallResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Глобальная обработка исключений для всего приложения
 *
 * Перехватывает исключения и преобразует их в стандартный ToolCallResponse
 * Логирует ошибки на разных уровнях
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Обработка SSH ошибок подключения
     */
    @ExceptionHandler(SshConnectionException.class)
    public ResponseEntity<ToolCallResponse> handleSshError(SshConnectionException ex, WebRequest request) {
        String message = "SSH connection failed: " + ex.getMessage();

        log.error("SSH Connection Error - {}", message, ex);

        ToolCallResponse response = new ToolCallResponse(
                false,
                message,
                "SSH"
        );

        return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * Обработка ошибок Docker команд
     */
    @ExceptionHandler(DockerCommandException.class)
    public ResponseEntity<ToolCallResponse> handleDockerError(DockerCommandException ex, WebRequest request) {
        String errorMessage = ex.getMessage();
        if (ex.hasStderr()) {
            errorMessage = errorMessage + " | Stderr: " + ex.getStderr();
        }

        log.error("Docker Command Error - Exit Code: {}, Error: {}", ex.getExitCode(), errorMessage, ex);

        ToolCallResponse response = new ToolCallResponse(
                false,
                errorMessage,
                "DOCKER"
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Обработка ошибок валидации параметров
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ToolCallResponse> handleValidationError(IllegalArgumentException ex, WebRequest request) {
        String errorMessage = ex.getMessage();

        log.warn("Validation Error - {}", errorMessage);

        ToolCallResponse response = new ToolCallResponse(
                false,
                errorMessage,
                "VALIDATION"
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Обработка ошибок не найденных ресурсов
     */
    @ExceptionHandler(value = RuntimeException.class)
    public ResponseEntity<ToolCallResponse> handleRuntimeException(RuntimeException ex, WebRequest request) {
        String message = ex.getMessage();

        // Проверить тип ошибки по сообщению
        if (message != null && message.contains("Container not found")) {
            log.warn("Container not found: {}", message);
            return new ResponseEntity<>(
                    new ToolCallResponse(false, message, "DOCKER"),
                    HttpStatus.NOT_FOUND
            );
        }

        if (message != null && message.contains("SSH connection failed")) {
            log.error("SSH connection error: {}", message);
            return new ResponseEntity<>(
                    new ToolCallResponse(false, message, "SSH"),
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }

        // Generic Runtime Exception
        log.error("Unexpected runtime error: {}", message, ex);

        ToolCallResponse response = new ToolCallResponse(
                false,
                message != null ? message : ex.getClass().getSimpleName(),
                "RUNTIME"
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Обработка всех остальных ошибок
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ToolCallResponse> handleGenericError(Exception ex, WebRequest request) {
        String errorDetails = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();

        log.error("Unexpected error of type {}: {}", ex.getClass().getSimpleName(), errorDetails, ex);

        ToolCallResponse response = new ToolCallResponse(
                false,
                errorDetails,
                "INTERNAL"
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Обработка NullPointerException
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ToolCallResponse> handleNullPointerException(NullPointerException ex, WebRequest request) {
        log.error("NullPointerException: {}", ex.getMessage(), ex);

        ToolCallResponse response = new ToolCallResponse(
                false,
                "A required value is null",
                "NULL_POINTER"
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Обработка IllegalStateException
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ToolCallResponse> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        log.warn("IllegalStateException: {}", ex.getMessage());

        ToolCallResponse response = new ToolCallResponse(
                false,
                ex.getMessage(),
                "ILLEGAL_STATE"
        );

        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }
}

