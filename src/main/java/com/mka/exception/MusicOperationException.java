package com.mka.exception;

public class MusicOperationException extends RuntimeException {
    public MusicOperationException(String publicCode, Throwable cause) {
        super(publicCode, cause);
    }
}
