package com.mka.client.openai;

import com.mka.config.openai.OpenAIProperties;
import com.mka.dto.openai.OpenAIHealthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class OpenAIClientTest {

    private OpenAIProperties properties;
    private OpenAIClientImpl client;

    @BeforeEach
    void setUp() {
        properties = new OpenAIProperties();
        properties.setApiKey("");
        properties.setApiUrl("https://api.openai.com/v1");
        properties.setTranslationModel("gpt-4o-mini");
        properties.setTranscriptionModel("gpt-4o-mini-transcribe");
        properties.setModerationModel("omni-moderation-latest");
        properties.setConnectTimeout(Duration.ofSeconds(5));
        properties.setReadTimeout(Duration.ofSeconds(30));

        RestClient restClient = RestClient.builder()
                .baseUrl(properties.getApiUrl())
                .build();

        client = new OpenAIClientImpl(restClient, properties);
    }

    @Test
    void testIsConfigured_returnsFalseWhenEmpty() {
        assertFalse(client.isConfigured());
    }

    @Test
    void testIsConfigured_returnsTrueWhenKeyPresent() {
        properties.setApiKey("sk-dummy-valid-key");
        assertTrue(client.isConfigured());
    }

    @Test
    void testCheckConnection_returnsMisconfiguredWhenKeyMissing() {
        OpenAIHealthResponse response = client.checkConnection();
        assertNotNull(response);
        assertEquals("MISCONFIGURED", response.getStatus());
        assertFalse(response.isConnected());
        assertEquals("gpt-4o-mini", response.getTranslationModel());
        assertEquals("gpt-4o-mini-transcribe", response.getTranscriptionModel());
        assertEquals("omni-moderation-latest", response.getModerationModel());
    }
}
