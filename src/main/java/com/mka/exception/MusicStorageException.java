package com.mka.exception;

public class MusicStorageException extends RuntimeException {
    public MusicStorageException() {
        super("MUSIC_STORAGE_ERROR");
    }

    public MusicStorageException(Throwable cause) {
        super("MUSIC_STORAGE_ERROR", cause);
    }
}
