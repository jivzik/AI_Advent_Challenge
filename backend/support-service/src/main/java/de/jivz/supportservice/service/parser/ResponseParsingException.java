package de.jivz.supportservice.service.parser;

/**
 * Exception für Response-Parsing-Fehler.
 */
public class ResponseParsingException extends Exception {

    public ResponseParsingException(String message) {
        super(message);
    }

    public ResponseParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}

