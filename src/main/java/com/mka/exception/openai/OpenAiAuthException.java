package com.mka.exception.openai;

/**
 * Exception thrown when OpenAI authentication fails (401 Unauthorized or missing API key).
 */
public class OpenAiAuthException extends OpenAiApiException {

    public OpenAiAuthException(String message) {
        super(message);
    }

    public OpenAiAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
