package com.mka.service;

import org.springframework.core.io.Resource;

import java.io.InputStream;

public interface MusicStorageService {

    StoredMusicResource getAudio(String filename);

    StoredMusicResource getCover(String filename);
    StoredMusicResource getPrivateAudio(String storageKey);
    StoredMusicResource getPrivateCover(String storageKey);

    StagedMusicFile stageAudio(InputStream source, String extension);
    StagedMusicFile stageCover(InputStream source, String extension);
    void promoteDraftAudio(StagedMusicFile staged);
    void promoteDraftCover(StagedMusicFile staged);
    void discardStaged(StagedMusicFile staged);
    void deleteDraftAudio(String storageKey);
    void deleteDraftCover(String storageKey);
    boolean draftAudioExists(String storageKey);
    boolean draftCoverExists(String storageKey);
    boolean publicAudioExists(String storageKey);
    boolean publicCoverExists(String storageKey);
    void publishAudio(String storageKey);
    void publishCover(String storageKey);
    void unpublishAudio(String storageKey);
    void unpublishCover(String storageKey);

    QuarantinedMusicFile quarantinePrivateAudio(String storageKey);
    QuarantinedMusicFile quarantinePrivateCover(String storageKey);
    QuarantinedMusicFile quarantinePublicAudio(String storageKey);
    QuarantinedMusicFile quarantinePublicCover(String storageKey);
    void restoreQuarantined(QuarantinedMusicFile quarantined);
    void purgeQuarantined(QuarantinedMusicFile quarantined);

    record StoredMusicResource(Resource resource, String contentType, long contentLength) {
    }

    record StagedMusicFile(String token, String storageKey) {
    }

    enum MusicFileArea { PRIVATE_AUDIO, PRIVATE_COVER, PUBLIC_AUDIO, PUBLIC_COVER }

    record QuarantinedMusicFile(String token, String storageKey, MusicFileArea area) {
    }
}
