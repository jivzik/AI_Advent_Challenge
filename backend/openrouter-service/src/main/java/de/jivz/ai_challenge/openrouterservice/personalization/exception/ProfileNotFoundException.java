package de.jivz.ai_challenge.openrouterservice.personalization.exception;

/**
 * Exception thrown when a user profile is not found
 */
public class ProfileNotFoundException extends RuntimeException {

    public ProfileNotFoundException(String message) {
        super(message);
    }

    public ProfileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
