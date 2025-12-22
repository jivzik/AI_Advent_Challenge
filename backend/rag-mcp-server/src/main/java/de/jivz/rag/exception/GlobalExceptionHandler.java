package de.jivz.rag.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Глобальный обработчик исключений.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(DocumentProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleDocumentProcessing(DocumentProcessingException ex) {
        log.error("❌ Document processing error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(errorResponse("DOCUMENT_PROCESSING_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(EmbeddingGenerationException.class)
    public ResponseEntity<Map<String, Object>> handleEmbeddingGeneration(EmbeddingGenerationException ex) {
        log.error("❌ Embedding generation error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorResponse("EMBEDDING_GENERATION_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<Map<String, Object>> handleDatabase(DatabaseException ex) {
        log.error("❌ Database error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse("DATABASE_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(InvalidFileFormatException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidFileFormat(InvalidFileFormatException ex) {
        log.error("❌ Invalid file format: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorResponse("INVALID_FILE_FORMAT", ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("⚠️ File too large: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(errorResponse("FILE_TOO_LARGE", "Maximum file size exceeded (50MB)"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("⚠️ Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorResponse("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("❌ Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    private Map<String, Object> errorResponse(String code, String message) {
        return Map.of(
                "success", false,
                "error", Map.of(
                        "code", code,
                        "message", message,
                        "timestamp", LocalDateTime.now().toString()
                )
        );
    }
}

