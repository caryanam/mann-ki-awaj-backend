package com.mka.exception.openai;

/**
 * Generic runtime exception for OpenAI API failures.
 */
public class OpenAiApiException extends RuntimeException {

    public OpenAiApiException(String message) {
        super(message);
    }

    public OpenAiApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
