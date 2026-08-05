package com.mka.exception.translation;

/**
 * Custom runtime exception thrown by translation operations.
 */
public class TranslationException extends RuntimeException {

    private String errorCode;

    public TranslationException(String message) {
        super(message);
    }

    public TranslationException(String message, Throwable cause) {
        super(message, cause);
    }

    public TranslationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public TranslationException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
