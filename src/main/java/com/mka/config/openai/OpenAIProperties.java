package com.mka.config.openai;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for OpenAI API integration.
 * Reads properties with prefix `openai` from application.properties or .env configuration.
 */
@ConfigurationProperties(prefix = "openai")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpenAIProperties {

    /**
     * OpenAI API Key loaded from environment variable OPENAI_API_KEY or .env configuration.
     */
    private String apiKey;

    /**
     * Base URL for OpenAI REST API endpoints.
     */
    private String apiUrl = "https://api.openai.com/v1";

    /**
     * Default model for translation.
     */
    private String translationModel = "gpt-4o-mini";

    /**
     * Default model for speech transcription.
     */
    private String transcriptionModel = "gpt-4o-mini-transcribe";

    /**
     * Default model for content moderation.
     */
    private String moderationModel = "omni-moderation-latest";

    /**
     * HTTP connection establishment timeout.
     */
    private Duration connectTimeout = Duration.ofSeconds(5);

    /**
     * HTTP socket read timeout.
     */
    private Duration readTimeout = Duration.ofSeconds(30);
}
