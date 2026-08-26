package com.mka.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@Configuration
public class FileUploadConfig {

    private final MusicUploadProperties musicUploadProperties;

    public FileUploadConfig(MusicUploadProperties musicUploadProperties) {
        this.musicUploadProperties = musicUploadProperties;
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        long largestFile = Math.max(musicUploadProperties.getMaxAudioSize().toBytes(),
                musicUploadProperties.getMaxCoverSize().toBytes());
        long requestSize = musicUploadProperties.getMaxAudioSize().toBytes()
                + musicUploadProperties.getMaxCoverSize().toBytes() + DataSize.ofMegabytes(1).toBytes();
        factory.setMaxFileSize(DataSize.ofBytes(largestFile));
        factory.setMaxRequestSize(DataSize.ofBytes(requestSize));
        return factory.createMultipartConfig();
    }
}
