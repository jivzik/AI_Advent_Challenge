package de.jivz.ai_challenge.openrouterservice.personalization.exception;
/**
 * Exception thrown when profile data is invalid
 */
public class InvalidProfileDataException extends RuntimeException {
    public InvalidProfileDataException(String message) {
        super(message);
    }
    public InvalidProfileDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
