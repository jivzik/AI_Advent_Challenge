package de.jivz.rag.exception;

/**
 * Исключение при неверном формате файла.
 */
public class InvalidFileFormatException extends RuntimeException {

    public InvalidFileFormatException(String message) {
        super(message);
    }

    public InvalidFileFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}

