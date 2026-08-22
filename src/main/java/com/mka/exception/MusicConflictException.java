package com.mka.exception;

public class MusicConflictException extends RuntimeException {
    public MusicConflictException(String code) {
        super(code);
    }
}
