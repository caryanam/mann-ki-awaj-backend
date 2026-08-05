package com.mka.translation.client;

import com.mka.translation.dto.TranslationRequest;
import com.mka.translation.dto.TranslationResponse;

/**
 * REST Client Interface for communicating with the external Python IndicTrans2 translation service.
 */
public interface IndicTrans2Client {

    /**
     * Sends a translation request payload to POST /api/v1/translate.
     *
     * @param request The translation request containing text, source, and target languages.
     * @return TranslationResponse payload.
     */
    TranslationResponse translate(TranslationRequest request);

    /**
     * Performs a health check against the translation microservice.
     *
     * @return true if the health endpoint returns HTTP 200 OK.
     */
    boolean isTranslationServiceAvailable();
}
