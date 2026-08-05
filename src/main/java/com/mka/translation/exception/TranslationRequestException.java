package com.mka.translation.exception;

/**
 * Exception thrown when a translation request contains invalid or malformed parameters.
 */
public class TranslationRequestException extends RuntimeException {
    public TranslationRequestException(String message) {
        super(message);
    }

    public TranslationRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
