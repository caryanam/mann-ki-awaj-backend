package com.mka.service.translation;

import com.mka.enums.translation.SupportedLanguage;

/**
 * Service Interface for natural language detection on text inputs.
 */
public interface LanguageDetectionService {

    SupportedLanguage detectLanguage(String text);

    SupportedLanguage detectLanguageWithFallback(String text, SupportedLanguage fallbackLanguage);

    boolean isTextInLanguage(String text, SupportedLanguage language);
}
