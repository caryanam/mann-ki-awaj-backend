package com.mka.dto.translation;

import com.mka.enums.translation.EntityType;
import com.mka.enums.translation.SupportedLanguage;
import com.mka.enums.translation.TranslationProvider;
import com.mka.enums.translation.TranslationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for translation response payload.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationResponse {

    private String originalText;
    private String translatedText;
    private SupportedLanguage sourceLanguage;
    private SupportedLanguage targetLanguage;
    private EntityType entityType;
    private Long entityId;
    private TranslationProvider provider;
    private TranslationStatus status;
    private boolean cached;
    private String errorMessage;
    private LocalDateTime translatedAt;

    public static TranslationResponse success(String originalText, String translatedText,
                                              SupportedLanguage sourceLanguage, SupportedLanguage targetLanguage,
                                              TranslationProvider provider, boolean cached) {
        return TranslationResponse.builder()
                .originalText(originalText)
                .translatedText(translatedText)
                .sourceLanguage(sourceLanguage)
                .targetLanguage(targetLanguage)
                .provider(provider)
                .status(TranslationStatus.COMPLETED)
                .cached(cached)
                .translatedAt(LocalDateTime.now())
                .build();
    }

    public static TranslationResponse failure(String originalText, String errorMessage,
                                              SupportedLanguage sourceLanguage, SupportedLanguage targetLanguage,
                                              TranslationProvider provider) {
        return TranslationResponse.builder()
                .originalText(originalText)
                .translatedText(originalText)
                .sourceLanguage(sourceLanguage)
                .targetLanguage(targetLanguage)
                .provider(provider)
                .status(TranslationStatus.FAILED)
                .cached(false)
                .errorMessage(errorMessage)
                .translatedAt(LocalDateTime.now())
                .build();
    }
}
