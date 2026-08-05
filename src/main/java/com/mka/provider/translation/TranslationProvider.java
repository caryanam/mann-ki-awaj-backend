package com.mka.provider.translation;

import com.mka.dto.translation.TranslationRequest;
import com.mka.dto.translation.TranslationResponse;
import com.mka.enums.translation.SupportedLanguage;

/**
 * Strategy/Provider interface implemented by specific translation engine backends.
 */
public interface TranslationProvider {

    TranslationResponse translate(TranslationRequest request);

    com.mka.enums.translation.TranslationProvider getProviderType();

    boolean isAvailable();

    boolean supportsLanguagePair(SupportedLanguage sourceLanguage, SupportedLanguage targetLanguage);
}
