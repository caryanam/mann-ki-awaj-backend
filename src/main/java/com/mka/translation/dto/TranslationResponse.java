package com.mka.translation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object representing translation service responses.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationResponse {

    private String originalText;
    private String translatedText;
    private String sourceLanguage;
    private String targetLanguage;
    private String engine;
    private boolean cached;

    public static TranslationResponse fallback(String originalText, String sourceLanguage, String targetLanguage) {
        return TranslationResponse.builder()
                .originalText(originalText)
                .translatedText(originalText)
                .sourceLanguage(sourceLanguage)
                .targetLanguage(targetLanguage)
                .engine("fallback")
                .cached(false)
                .build();
    }
}
