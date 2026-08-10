package com.mka.exception.openai;

/**
 * Exception thrown when OpenAI rate limits or quota limits are exceeded (429 Too Many Requests).
 */
public class OpenAiRateLimitException extends OpenAiApiException {

    public OpenAiRateLimitException(String message) {
        super(message);
    }

    public OpenAiRateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
