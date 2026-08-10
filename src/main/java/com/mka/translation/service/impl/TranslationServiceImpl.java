package com.mka.translation.service.impl;

import com.mka.enums.translation.SupportedLanguage;
import com.mka.translation.dto.TranslationRequest;
import com.mka.translation.dto.TranslationResponse;
import com.mka.translation.exception.TranslationRequestException;
import com.mka.translation.provider.OpenAITranslationProvider;
import com.mka.translation.service.TranslationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Production-ready implementation of TranslationService using OpenAI exclusively.
 * Execution flow:
 * 1. Request validation (throws TranslationRequestException if text/lang blank)
 * 2. Same source & target language check -> returns original text (engine = "none")
 * 3. OpenAI Translation Provider -> returns translated text (engine = "OpenAI")
 * 4. OpenAI Failure -> graceful original text fallback (engine = "fallback")
 */
@Service
public class TranslationServiceImpl implements TranslationService {

    private static final Logger log = LoggerFactory.getLogger(TranslationServiceImpl.class);

    private final OpenAITranslationProvider openAiProvider;

    public TranslationServiceImpl(OpenAITranslationProvider openAiProvider) {
        this.openAiProvider = openAiProvider;
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

        // 1. Same language check: Return original text immediately
        SupportedLanguage srcEnum = SupportedLanguage.fromCode(srcLang);
        SupportedLanguage tgtEnum = SupportedLanguage.fromCode(tgtLang);
        if (srcEnum == tgtEnum || srcLang.equalsIgnoreCase(tgtLang)) {
            log.info("Source and target language identical [{}]. Returning original text.", srcLang);
            return TranslationResponse.builder()
                    .originalText(cleanText)
                    .translatedText(cleanText)
                    .sourceLanguage(srcLang)
                    .targetLanguage(tgtLang)
                    .engine("none")
                    .cached(false)
                    .build();
        }

        TranslationRequest request = TranslationRequest.builder()
                .text(cleanText)
                .sourceLanguage(srcLang)
                .targetLanguage(tgtLang)
                .build();

        long startTime = System.currentTimeMillis();
        log.info("Translation request started: [{}] -> [{}] (Length: {} chars)", srcLang, tgtLang, cleanText.length());

        // 2. Execute OpenAI Primary Provider
        if (openAiProvider.isAvailable()) {
            try {
                TranslationResponse response = openAiProvider.translate(request);
                long duration = System.currentTimeMillis() - startTime;
                log.info("OpenAI Translation completed successfully: [{}] -> [{}] in {}ms", srcLang, tgtLang, duration);
                return response;
            } catch (Exception ex) {
                long duration = System.currentTimeMillis() - startTime;
                log.warn("OpenAI Translation provider failed after {}ms: {}. Returning graceful original text fallback.", duration, ex.getMessage());
            }
        } else {
            log.warn("OpenAI Translation provider is not configured or unavailable. Returning graceful original text fallback.");
        }

        // 3. Graceful Fallback: Return original text
        return TranslationResponse.fallback(cleanText, srcLang, tgtLang);
    }

    @Override
    public boolean isTranslationServiceAvailable() {
        return openAiProvider.isAvailable();
    }
}
