package com.mka.client.translation;

import com.mka.dto.translation.TranslationRequest;
import com.mka.dto.translation.TranslationResponse;

/**
 * Client Interface for communicating with the external Python IndicTrans2 translation service.
 */
public interface IndicTrans2Client {

    TranslationResponse translate(TranslationRequest request);

    String translateSingle(String text, String sourceLanguage, String targetLanguage);

    boolean isHealthOk();
}
