package com.mka.translation.provider;

import com.mka.enums.translation.TranslationProvider;
import com.mka.translation.dto.TranslationRequest;
import com.mka.translation.dto.TranslationResponse;

/**
 * Strategy interface for translation provider implementations (e.g. OpenAI).
 */
public interface TranslationProviderStrategy {

    /**
     * Translates content according to the request specifications.
     *
     * @param request TranslationRequest payload
     * @return TranslationResponse payload
     */
    TranslationResponse translate(TranslationRequest request);

    /**
     * Checks if this provider is configured and reachable.
     *
     * @return true if available.
     */
    boolean isAvailable();

    /**
     * Returns the enum type of this provider.
     *
     * @return TranslationProvider enum
     */
    TranslationProvider getProviderType();
}
