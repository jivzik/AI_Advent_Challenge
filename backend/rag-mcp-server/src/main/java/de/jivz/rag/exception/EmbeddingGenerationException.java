package de.jivz.rag.exception;

/**
 * Исключение при генерации эмбеддингов.
 */
public class EmbeddingGenerationException extends RuntimeException {

    public EmbeddingGenerationException(String message) {
        super(message);
    }

    public EmbeddingGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}

