package com.mka.translation.exception;

/**
 * Exception thrown when the external translation microservice is unreachable or down.
 */
public class TranslationServiceUnavailableException extends RuntimeException {
    public TranslationServiceUnavailableException(String message) {
        super(message);
    }

    public TranslationServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
