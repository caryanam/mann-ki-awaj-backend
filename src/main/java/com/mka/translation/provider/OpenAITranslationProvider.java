package com.mka.translation.provider;

import com.mka.client.openai.OpenAIClient;
import com.mka.config.openai.OpenAIProperties;
import com.mka.enums.translation.SupportedLanguage;
import com.mka.enums.translation.TranslationProvider;
import com.mka.translation.dto.TranslationRequest;
import com.mka.translation.dto.TranslationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * OpenAI implementation of TranslationProviderStrategy.
 * Translates content via OpenAI Chat Completions API (using model configured in openai.translation.model).
 */
@Component
public class OpenAITranslationProvider implements TranslationProviderStrategy {

    private static final Logger log = LoggerFactory.getLogger(OpenAITranslationProvider.class);

    private final OpenAIClient openAIClient;
    private final OpenAIProperties openAIProperties;

    public OpenAITranslationProvider(OpenAIClient openAIClient, OpenAIProperties openAIProperties) {
        this.openAIClient = openAIClient;
        this.openAIProperties = openAIProperties;
    }

    @Override
    public TranslationResponse translate(TranslationRequest request) {
        String cleanText = request.getText().trim();
        SupportedLanguage srcLangEnum = SupportedLanguage.fromCode(request.getSourceLanguage());
        SupportedLanguage tgtLangEnum = SupportedLanguage.fromCode(request.getTargetLanguage());

        String srcLangName = srcLangEnum != null ? srcLangEnum.getDisplayName() : "auto-detected language";
        String tgtLangName = tgtLangEnum != null ? tgtLangEnum.getDisplayName() : request.getTargetLanguage();

        log.info("Executing OpenAI Translation [{}] -> [{}] for text length: {}",
                srcLangName, tgtLangName, cleanText.length());

        String translatedText = openAIClient.translateText(
                cleanText,
                srcLangName,
                tgtLangName,
                openAIProperties.getTranslationModel()
        );

        return TranslationResponse.builder()
                .originalText(cleanText)
                .translatedText(translatedText)
                .sourceLanguage(request.getSourceLanguage())
                .targetLanguage(request.getTargetLanguage())
                .engine("OpenAI")
                .cached(false)
                .build();
    }

    @Override
    public boolean isAvailable() {
        return openAIClient.isConfigured();
    }

    @Override
    public TranslationProvider getProviderType() {
        return TranslationProvider.OPENAI;
    }
}
