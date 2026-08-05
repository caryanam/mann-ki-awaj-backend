package com.mka.service.translation;

import com.mka.dto.translation.TranslationRequest;
import com.mka.dto.translation.TranslationResponse;
import com.mka.enums.translation.EntityType;
import com.mka.enums.translation.SupportedLanguage;

import java.util.Optional;

/**
 * High-level Service Interface for text and entity translation management.
 */
public interface TranslationService {

    TranslationResponse translate(TranslationRequest request);

    TranslationResponse translate(String text, SupportedLanguage sourceLanguage, SupportedLanguage targetLanguage);

    TranslationResponse translateEntity(EntityType entityType, Long entityId, String text,
                                        SupportedLanguage sourceLanguage, SupportedLanguage targetLanguage);

    Optional<TranslationResponse> getCachedTranslation(EntityType entityType, Long entityId, SupportedLanguage targetLanguage);

    void clearCache(EntityType entityType, Long entityId);

    boolean isLanguageSupported(SupportedLanguage language);
}
