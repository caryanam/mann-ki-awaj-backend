package com.mka.translation.service;

import com.mka.enums.translation.TranslationProvider;
import com.mka.translation.dto.TranslationRequest;
import com.mka.translation.dto.TranslationResponse;
import com.mka.translation.exception.TranslationRequestException;
import com.mka.translation.provider.OpenAITranslationProvider;
import com.mka.translation.service.impl.TranslationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TranslationServiceImplTest {

    private OpenAITranslationProvider openAiProvider;
    private TranslationServiceImpl translationService;

    @BeforeEach
    void setUp() {
        openAiProvider = mock(OpenAITranslationProvider.class);
        translationService = new TranslationServiceImpl(openAiProvider);
    }

    @Test
    void testTranslate_blankText_throwsException() {
        assertThrows(TranslationRequestException.class, () -> translationService.translate("", "EN", "MR"));
    }

    @Test
    void testTranslate_blankSourceLanguage_throwsException() {
        assertThrows(TranslationRequestException.class, () -> translationService.translate("Hello", "", "MR"));
    }

    @Test
    void testTranslate_blankTargetLanguage_throwsException() {
        assertThrows(TranslationRequestException.class, () -> translationService.translate("Hello", "EN", ""));
    }

    @Test
    void testTranslate_sameLanguage_returnsOriginalTextWithoutCallingOpenAI() {
        TranslationResponse response = translationService.translate("Hello world", "EN", "EN");
        assertNotNull(response);
        assertEquals("Hello world", response.getOriginalText());
        assertEquals("Hello world", response.getTranslatedText());
        assertEquals("none", response.getEngine());
        verifyNoInteractions(openAiProvider);
    }

    @Test
    void testTranslate_openAiSuccess_returnsOpenAiResponse() {
        when(openAiProvider.isAvailable()).thenReturn(true);
        when(openAiProvider.translate(any(TranslationRequest.class))).thenReturn(
                TranslationResponse.builder()
                        .originalText("Hello")
                        .translatedText("नमस्कार")
                        .sourceLanguage("EN")
                        .targetLanguage("MR")
                        .engine("OpenAI")
                        .cached(false)
                        .build()
        );

        TranslationResponse response = translationService.translate("Hello", "EN", "MR");
        assertNotNull(response);
        assertEquals("नमस्कार", response.getTranslatedText());
        assertEquals("OpenAI", response.getEngine());
        verify(openAiProvider, times(1)).translate(any(TranslationRequest.class));
    }

    @Test
    void testTranslate_openAiFailure_returnsGracefulOriginalTextFallback() {
        when(openAiProvider.isAvailable()).thenReturn(true);
        when(openAiProvider.translate(any(TranslationRequest.class))).thenThrow(new RuntimeException("OpenAI timeout / rate limit"));

        TranslationResponse response = translationService.translate("Hello", "EN", "MR");
        assertNotNull(response);
        assertEquals("Hello", response.getOriginalText());
        assertEquals("Hello", response.getTranslatedText());
        assertEquals("fallback", response.getEngine());
        assertFalse(response.isCached());
    }

    @Test
    void testTranslate_openAiUnavailable_returnsGracefulOriginalTextFallback() {
        when(openAiProvider.isAvailable()).thenReturn(false);

        TranslationResponse response = translationService.translate("Hello", "EN", "MR");
        assertNotNull(response);
        assertEquals("Hello", response.getOriginalText());
        assertEquals("Hello", response.getTranslatedText());
        assertEquals("fallback", response.getEngine());
        verify(openAiProvider, never()).translate(any(TranslationRequest.class));
    }
}
