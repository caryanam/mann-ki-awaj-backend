package com.mka.translation.service;

import com.mka.translation.dto.TranslationResponse;
import java.util.List;
import java.util.Map;

/**
 * Service interface for multi-lingual translation handling with resilience and fallbacks.
 */
public interface TranslationService {

    /**
     * Translates input text from sourceLanguage to targetLanguage.
     */
    TranslationResponse translate(String text, String sourceLanguage, String targetLanguage);

    /**
     * Batch translates a list of texts in a single execution.
     */
    Map<String, String> translateBatch(List<String> texts, String sourceLanguage, String targetLanguage);

    /**
     * Checks if the translation service is available and healthy.
     */
    boolean isTranslationServiceAvailable();
}
