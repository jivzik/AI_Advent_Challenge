package de.jivz.ai_challenge.exception;

/**
 * Custom exception for external service errors (e.g., Perplexity API).
 */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

