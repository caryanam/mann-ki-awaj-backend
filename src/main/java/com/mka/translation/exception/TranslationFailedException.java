package com.mka.translation.exception;

/**
 * Exception thrown when neural translation inference fails inside the translation engine.
 */
public class TranslationFailedException extends RuntimeException {
    public TranslationFailedException(String message) {
        super(message);
    }

    public TranslationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
