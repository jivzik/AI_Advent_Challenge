package de.jivz.ai_challenge.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for database-related errors.
 * Provides graceful error handling and fallback mechanisms.
 *
 * Strategy:
 * - Catch database exceptions (PostgreSQL connection issues, etc.)
 * - Log errors for monitoring
 * - Return user-friendly error messages
 * - Allow application to continue with RAM-only mode
 */
@Slf4j
@ControllerAdvice
public class DatabaseExceptionHandler {

    /**
     * Handles all data access exceptions (PostgreSQL errors).
     *
     * Examples:
     * - Connection timeout
     * - Database unavailable
     * - SQL syntax errors
     * - Constraint violations
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(DataAccessException ex) {
        log.error("❌ Database error occurred: {}", ex.getMessage(), ex);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Database temporarily unavailable");
        errorResponse.put("message", "The system is currently operating in fallback mode. Your request can still be processed, but conversation history may not be persisted.");
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        errorResponse.put("fallbackMode", true);

        // Include specific error details in debug mode
        if (log.isDebugEnabled()) {
            errorResponse.put("details", ex.getMessage());
        }

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorResponse);
    }

    /**
     * Handles generic database-related exceptions.
     */
    @ExceptionHandler(org.springframework.jdbc.CannotGetJdbcConnectionException.class)
    public ResponseEntity<Map<String, Object>> handleConnectionException(Exception ex) {
        log.error("❌ Cannot connect to database: {}", ex.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Database connection failed");
        errorResponse.put("message", "Unable to connect to PostgreSQL. The application will continue with in-memory storage only.");
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        errorResponse.put("fallbackMode", true);
        errorResponse.put("action", "Please check database configuration and ensure PostgreSQL is running.");

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorResponse);
    }

    /**
     * Handles transaction-related errors.
     */
    @ExceptionHandler(org.springframework.transaction.TransactionException.class)
    public ResponseEntity<Map<String, Object>> handleTransactionException(Exception ex) {
        log.error("❌ Transaction error: {}", ex.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Transaction failed");
        errorResponse.put("message", "A database transaction could not be completed. Your data may not be saved.");
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    /**
     * Handles JPA/Hibernate specific errors.
     */
    @ExceptionHandler(jakarta.persistence.PersistenceException.class)
    public ResponseEntity<Map<String, Object>> handlePersistenceException(Exception ex) {
        log.error("❌ Persistence error: {}", ex.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Data persistence failed");
        errorResponse.put("message", "Failed to save or retrieve data from the database.");
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    /**
     * Generic exception handler as last resort.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("❌ Unexpected error: {}", ex.getMessage(), ex);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Internal server error");
        errorResponse.put("message", "An unexpected error occurred. Please try again later.");
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
}

