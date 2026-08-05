package com.mka.translation.service;

import com.mka.translation.dto.TranslationResponse;

/**
 * Service interface for multi-lingual translation handling with resilience and fallbacks.
 */
public interface TranslationService {

    /**
     * Translates input text from sourceLanguage to targetLanguage.
     *
     * @param text The text to translate.
     * @param sourceLanguage Source language code (e.g., eng_Latn or en).
     * @param targetLanguage Target language code (e.g., hin_Deva or hi).
     * @return TranslationResponse payload (or fallback if unavailable).
     */
    TranslationResponse translate(String text, String sourceLanguage, String targetLanguage);

    /**
     * Checks if the translation service is available and healthy.
     *
     * @return true if healthy.
     */
    boolean isTranslationServiceAvailable();
}
