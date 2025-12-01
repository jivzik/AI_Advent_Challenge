package de.jivz.ai_challenge.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all controllers.
 * Centralizes error handling and response formatting.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation errors from @Valid annotation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation error: {}", errors);

        ErrorResponse errorResponse = new ErrorResponse(
                "Validation failed",
                errors.toString(),
                Instant.now()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles IllegalArgumentException (business validation errors).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Invalid request: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                "Invalid request",
                ex.getMessage(),
                Instant.now()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles external service errors (e.g., Perplexity API failures).
     */
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalServiceException(ExternalServiceException ex) {
        log.error("External service error: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
                "External service unavailable",
                ex.getMessage(),
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
    }

    /**
     * Handles all other runtime exceptions.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
                "Internal server error",
                "An unexpected error occurred. Please try again later.",
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Standardized error response structure.
     * All fields are non-null by design.
     */
    @Getter
    @AllArgsConstructor
    public static class ErrorResponse {
        private final String error;
        private final String message;
        private final Instant timestamp;
    }
}

