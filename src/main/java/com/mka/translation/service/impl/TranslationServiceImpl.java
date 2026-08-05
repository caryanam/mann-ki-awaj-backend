package com.mka.translation.service.impl;

import com.mka.translation.client.IndicTrans2Client;
import com.mka.translation.dto.TranslationRequest;
import com.mka.translation.dto.TranslationResponse;
import com.mka.translation.exception.TranslationRequestException;
import com.mka.translation.service.TranslationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Production-ready implementation of TranslationService.
 * Validates request parameters, measures latency, calls IndicTrans2Client,
 * and provides safe fallbacks on microservice failures without crashing the application.
 */
@Service
public class TranslationServiceImpl implements TranslationService {

    private static final Logger log = LoggerFactory.getLogger(TranslationServiceImpl.class);
    private final IndicTrans2Client indicTrans2Client;

    public TranslationServiceImpl(IndicTrans2Client indicTrans2Client) {
        this.indicTrans2Client = indicTrans2Client;
    }

    @Override
    public TranslationResponse translate(String text, String sourceLanguage, String targetLanguage) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("Translation request rejected: text is blank");
            throw new TranslationRequestException("Text to translate cannot be empty or blank.");
        }
        if (sourceLanguage == null || sourceLanguage.trim().isEmpty()) {
            log.warn("Translation request rejected: sourceLanguage is blank");
            throw new TranslationRequestException("Source language code cannot be empty.");
        }
        if (targetLanguage == null || targetLanguage.trim().isEmpty()) {
            log.warn("Translation request rejected: targetLanguage is blank");
            throw new TranslationRequestException("Target language code cannot be empty.");
        }

        String cleanText = text.trim();
        String srcLang = sourceLanguage.trim();
        String tgtLang = targetLanguage.trim();

        TranslationRequest request = TranslationRequest.builder()
                .text(cleanText)
                .sourceLanguage(srcLang)
                .targetLanguage(tgtLang)
                .build();

        long startTime = System.currentTimeMillis();
        log.info("Translation request started: [{}] -> [{}] (Length: {} chars)", srcLang, tgtLang, cleanText.length());

        try {
            TranslationResponse response = indicTrans2Client.translate(request);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Translation completed successfully: [{}] -> [{}] in {}ms (engine: {}, cached: {})",
                    srcLang, tgtLang, duration, response.getEngine(), response.isCached());
            return response;
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Translation service call failed after {}ms: {}. Returning graceful fallback.", duration, ex.getMessage());
            
            return TranslationResponse.fallback(cleanText, srcLang, tgtLang);
        }
    }

    @Override
    public boolean isTranslationServiceAvailable() {
        return indicTrans2Client.isTranslationServiceAvailable();
    }
}
