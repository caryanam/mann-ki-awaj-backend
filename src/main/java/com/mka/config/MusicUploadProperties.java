package com.mka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "music.upload")
public class MusicUploadProperties {
    private DataSize maxAudioSize = DataSize.ofMegabytes(40);
    private DataSize maxCoverSize = DataSize.ofMegabytes(5);
    private int userMaxPending = 5;

    public DataSize getMaxAudioSize() { return maxAudioSize; }
    public void setMaxAudioSize(DataSize maxAudioSize) { this.maxAudioSize = maxAudioSize; }
    public DataSize getMaxCoverSize() { return maxCoverSize; }
    public void setMaxCoverSize(DataSize maxCoverSize) { this.maxCoverSize = maxCoverSize; }
    public int getUserMaxPending() { return userMaxPending; }
    public void setUserMaxPending(int userMaxPending) {
        if (userMaxPending < 1) throw new IllegalArgumentException("music.upload.user-max-pending must be positive");
        this.userMaxPending = userMaxPending;
    }
}
