package com.mka.translation.provider;

import com.mka.client.openai.OpenAIClient;
import com.mka.config.openai.OpenAIProperties;
import com.mka.enums.translation.TranslationProvider;
import com.mka.translation.dto.TranslationRequest;
import com.mka.translation.dto.TranslationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OpenAITranslationProviderTest {

    private OpenAIClient openAIClient;
    private OpenAIProperties openAIProperties;
    private OpenAITranslationProvider provider;

    @BeforeEach
    void setUp() {
        openAIClient = mock(OpenAIClient.class);
        openAIProperties = new OpenAIProperties();
        openAIProperties.setTranslationModel("gpt-4o-mini");

        provider = new OpenAITranslationProvider(openAIClient, openAIProperties);
    }

    @Test
    void testGetProviderType() {
        assertEquals(TranslationProvider.OPENAI, provider.getProviderType());
    }

    @Test
    void testIsAvailable_delegatesToClient() {
        when(openAIClient.isConfigured()).thenReturn(true);
        assertTrue(provider.isAvailable());

        when(openAIClient.isConfigured()).thenReturn(false);
        assertFalse(provider.isAvailable());
    }

    @Test
    void testTranslate_callsOpenAIClientWithHumanReadableLanguageNames() {
        when(openAIClient.translateText(eq("My name is Niraj"), eq("English"), eq("Marathi"), eq("gpt-4o-mini")))
                .thenReturn("माझं नाव नीरज आहे");

        TranslationRequest request = TranslationRequest.builder()
                .text("My name is Niraj")
                .sourceLanguage("EN")
                .targetLanguage("MR")
                .build();

        TranslationResponse response = provider.translate(request);

        assertNotNull(response);
        assertEquals("My name is Niraj", response.getOriginalText());
        assertEquals("माझं नाव नीरज आहे", response.getTranslatedText());
        assertEquals("EN", response.getSourceLanguage());
        assertEquals("MR", response.getTargetLanguage());
        assertEquals("OpenAI", response.getEngine());
        assertFalse(response.isCached());

        verify(openAIClient, times(1)).translateText("My name is Niraj", "English", "Marathi", "gpt-4o-mini");
    }

    @Test
    void testTranslate_resolvesFloresCodesToHumanReadableNames() {
        when(openAIClient.translateText(eq("Hello"), eq("English"), eq("Hindi"), eq("gpt-4o-mini")))
                .thenReturn("नमस्ते");

        TranslationRequest request = TranslationRequest.builder()
                .text("Hello")
                .sourceLanguage("eng_Latn")
                .targetLanguage("hin_Deva")
                .build();

        TranslationResponse response = provider.translate(request);

        assertNotNull(response);
        assertEquals("नमस्ते", response.getTranslatedText());
        assertEquals("OpenAI", response.getEngine());

        verify(openAIClient, times(1)).translateText("Hello", "English", "Hindi", "gpt-4o-mini");
    }

    @Test
    void testTranslate_preservesEmojis() {
        String input = "Happy birthday! 🎉🥳";
        String expectedOutput = "वाढदिवसाच्या हार्दिक शुभेच्छा! 🎉🥳";

        when(openAIClient.translateText(eq(input), eq("English"), eq("Marathi"), eq("gpt-4o-mini")))
                .thenReturn(expectedOutput);

        TranslationRequest request = TranslationRequest.builder()
                .text(input)
                .sourceLanguage("EN")
                .targetLanguage("MR")
                .build();

        TranslationResponse response = provider.translate(request);
        assertEquals(expectedOutput, response.getTranslatedText());
        assertTrue(response.getTranslatedText().contains("🎉🥳"));
    }

    @Test
    void testTranslate_preservesUsernamesAndMentions() {
        String input = "@quietchapter said hello";
        String expectedOutput = "@quietchapter म्हणाला नमस्कार";

        when(openAIClient.translateText(eq(input), eq("English"), eq("Marathi"), eq("gpt-4o-mini")))
                .thenReturn(expectedOutput);

        TranslationRequest request = TranslationRequest.builder()
                .text(input)
                .sourceLanguage("EN")
                .targetLanguage("MR")
                .build();

        TranslationResponse response = provider.translate(request);
        assertEquals(expectedOutput, response.getTranslatedText());
        assertTrue(response.getTranslatedText().contains("@quietchapter"));
    }

    @Test
    void testTranslate_autoSourceLanguage_usesAutoDetectedLanguageName() {
        when(openAIClient.translateText(eq("माझे विचार"), eq("auto-detected language"), eq("English"), eq("gpt-4o-mini")))
                .thenReturn("My thoughts");

        TranslationRequest request = TranslationRequest.builder()
                .text("माझे विचार")
                .sourceLanguage("auto")
                .targetLanguage("EN")
                .build();

        TranslationResponse response = provider.translate(request);

        assertNotNull(response);
        assertEquals("My thoughts", response.getTranslatedText());

        verify(openAIClient, times(1)).translateText("माझे विचार", "auto-detected language", "English", "gpt-4o-mini");
    }
}
