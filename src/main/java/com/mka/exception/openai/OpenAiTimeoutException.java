package com.mka.exception.openai;

/**
 * Exception thrown when requests to OpenAI time out or network connection fails.
 */
public class OpenAiTimeoutException extends OpenAiApiException {

    public OpenAiTimeoutException(String message) {
        super(message);
    }

    public OpenAiTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
