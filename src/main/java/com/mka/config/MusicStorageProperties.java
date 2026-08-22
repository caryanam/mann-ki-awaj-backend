package com.mka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "music.storage")
public class MusicStorageProperties {

    private String root = "music-storage";
    private String audioDir = "audio";
    private String coverDir = "covers";

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public String getAudioDir() {
        return audioDir;
    }

    public void setAudioDir(String audioDir) {
        this.audioDir = audioDir;
    }

    public String getCoverDir() {
        return coverDir;
    }

    public void setCoverDir(String coverDir) {
        this.coverDir = coverDir;
    }
}
