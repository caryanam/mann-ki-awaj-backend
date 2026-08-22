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
        if (cleanText.contains("%E0%A4%") || cleanText.contains("%E0%A5%") || cleanText.contains("%")) {
            try {
                cleanText = java.net.URLDecoder.decode(cleanText, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {}
        }
        String srcLang = sourceLanguage.trim();
        String tgtLang = targetLanguage.trim();

        // 1. Same language check: Return original text immediately if non-auto and identical
        SupportedLanguage srcEnum = SupportedLanguage.fromCode(srcLang);
        SupportedLanguage tgtEnum = SupportedLanguage.fromCode(tgtLang);
        if (srcEnum != null && tgtEnum != null && (srcEnum == tgtEnum || srcLang.equalsIgnoreCase(tgtLang))) {
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
                if (response != null && response.getTranslatedText() != null && !response.getTranslatedText().isBlank()) {
                    log.info("OpenAI Translation completed successfully: [{}] -> [{}] in {}ms", srcLang, tgtLang, duration);
                    return response;
                }
            } catch (Exception ex) {
                long duration = System.currentTimeMillis() - startTime;
                log.warn("OpenAI Translation provider failed after {}ms: {}. Trying server-side fallback.", duration, ex.getMessage());
            }
        } else {
            log.info("OpenAI Translation provider is not configured or unavailable. Executing server-side translation fallback.");
        }

        // 3. Server-side Google GTX Fallback
        String sanitizedText = sanitizeEncodedSymbols(cleanText);
        String gtxTranslated = translateViaGoogleGtx(sanitizedText, tgtLang);
        if (gtxTranslated != null && !gtxTranslated.isBlank() && !gtxTranslated.equals(sanitizedText)) {
            return TranslationResponse.builder()
                    .originalText(sanitizedText)
                    .translatedText(gtxTranslated)
                    .sourceLanguage(srcLang)
                    .targetLanguage(tgtLang)
                    .engine("GoogleGTX")
                    .cached(false)
                    .build();
        }

        // 4. Graceful Fallback: Return original text
        return TranslationResponse.fallback(sanitizedText, srcLang, tgtLang);
    }

    private String sanitizeEncodedSymbols(String str) {
        if (str == null || str.isBlank()) return str;
        String clean = str;
        if (clean.contains("%")) {
            try {
                clean = java.net.URLDecoder.decode(clean, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {}
        }
        return clean
                .replace("%2सी", ",")
                .replace("%3एफ", "?")
                .replace("%2स", ",")
                .replace("%3ए", "?")
                .replaceAll("(?i)%2C", ",")
                .replaceAll("(?i)%3F", "?")
                .replaceAll("(?i)%21", "!")
                .replace("%20", " ")
                .replaceAll("(?i)%3([Ff]|एफ)?", "?")
                .replaceAll("(?i)%2([Cc]|सी)?", ",");
    }

    private String translateViaGoogleGtx(String text, String targetLang) {
        try {
            String cleanInput = sanitizeEncodedSymbols(text);
            String encodedText = java.net.URLEncoder.encode(cleanInput, java.nio.charset.StandardCharsets.UTF_8);
            String tgt = targetLang != null ? targetLang.toLowerCase() : "en";
            String urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=" + tgt + "&dt=t&q=" + encodedText;
            java.net.URI uri = java.net.URI.create(urlStr);
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            java.util.List<?> response = restTemplate.getForObject(uri, java.util.List.class);
            if (response != null && !response.isEmpty() && response.get(0) instanceof java.util.List<?> parts) {
                StringBuilder sb = new StringBuilder();
                for (Object item : parts) {
                    if (item instanceof java.util.List<?> part && !part.isEmpty() && part.get(0) != null) {
                        sb.append(part.get(0).toString());
                    }
                }
                String result = sb.toString().trim();
                if (!result.isEmpty()) {
                    return sanitizeEncodedSymbols(result);
                }
            }
        } catch (Exception e) {
            log.warn("Server-side Google GTX translation failed: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public java.util.Map<String, String> translateBatch(java.util.List<String> texts, String sourceLanguage, String targetLanguage) {
        if (texts == null || texts.isEmpty()) return java.util.Collections.emptyMap();
        java.util.Map<String, String> resultMap = new java.util.HashMap<>();

        for (String text : texts) {
            if (text == null || text.isBlank()) continue;
            try {
                TranslationResponse resp = translate(text, sourceLanguage, targetLanguage);
                resultMap.put(text, resp != null && resp.getTranslatedText() != null ? resp.getTranslatedText() : text);
            } catch (Exception e) {
                resultMap.put(text, text);
            }
        }
        return resultMap;
    }

    @Override
    public boolean isTranslationServiceAvailable() {
        return openAiProvider.isAvailable();
    }
}
