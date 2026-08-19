package com.mka.client.openai;

import com.mka.config.openai.OpenAIProperties;
import com.mka.dto.openai.OpenAIHealthResponse;
import com.mka.dto.response.VoiceToTextResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OpenAIClientTest {

    private OpenAIProperties properties;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private OpenAIClientImpl client;

    @BeforeEach
    void setUp() {
        properties = new OpenAIProperties();
        properties.setApiKey("sk-test-key-12345");
        properties.setApiUrl("https://api.openai.com/v1");
        properties.setTranslationModel("gpt-4o-mini");
        properties.setTranscriptionModel("whisper-1");
        properties.setModerationModel("omni-moderation-latest");
        properties.setConnectTimeout(Duration.ofSeconds(5));
        properties.setReadTimeout(Duration.ofSeconds(30));

        client = new OpenAIClientImpl(restClient, properties);
    }

    @Test
    void testIsConfigured_returnsFalseWhenEmpty() {
        properties.setApiKey("");
        assertFalse(client.isConfigured());
    }

    @Test
    void testIsConfigured_returnsTrueWhenKeyPresent() {
        assertTrue(client.isConfigured());
    }

    @Test
    void testCheckConnection_returnsMisconfiguredWhenKeyMissing() {
        properties.setApiKey("");
        OpenAIHealthResponse response = client.checkConnection();
        assertNotNull(response);
        assertEquals("MISCONFIGURED", response.getStatus());
        assertFalse(response.isConnected());
    }

    @Test
    void testTranscribeAudio_English_SendsExplicitLanguageCode() {
        setupMockRestClientResponse("Hello world", "en");

        byte[] audioBytes = new byte[]{1, 2, 3, 4};
        VoiceToTextResponse response = client.transcribeAudio(audioBytes, "voice.webm", "whisper-1", "EN");

        assertNotNull(response);
        assertEquals("Hello world", response.getText());
        assertEquals("EN", response.getDetectedLanguage());

        ArgumentCaptor<MultiValueMap<String, Object>> bodyCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodySpec).body(bodyCaptor.capture());
        MultiValueMap<String, Object> body = bodyCaptor.getValue();

        assertEquals("en", body.getFirst("language"));
        assertEquals("whisper-1", body.getFirst("model"));
    }

    @Test
    void testTranscribeAudio_Hindi_SendsExplicitLanguageCodeAndPrompt() {
        setupMockRestClientResponse("\u0928\u092E\u0938\u094D\u0924\u0947", "hi");

        byte[] audioBytes = new byte[]{1, 2, 3, 4};
        VoiceToTextResponse response = client.transcribeAudio(audioBytes, "voice.webm", "whisper-1", "HI");

        assertNotNull(response);
        assertEquals("\u0928\u092E\u0938\u094D\u0924\u0947", response.getText());
        assertEquals("HI", response.getDetectedLanguage());

        ArgumentCaptor<MultiValueMap<String, Object>> bodyCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodySpec).body(bodyCaptor.capture());
        MultiValueMap<String, Object> body = bodyCaptor.getValue();

        assertEquals("hi", body.getFirst("language"));
        assertEquals("\u0928\u092E\u0938\u094D\u0915\u093E\u0930, \u092E\u0928 \u0915\u0940 \u0906\u0935\u093E\u091C \u092E\u0947\u0902 \u0906\u092A\u0915\u093E \u0938\u094D\u0935\u093E\u0917\u0924 \u0939\u0948\u0964", body.getFirst("prompt"));
    }

    @Test
    void testTranscribeAudio_Marathi_SendsExplicitLanguageCodeAndPrompt() {
        setupMockRestClientResponse("\u0928\u092E\u0938\u094D\u0915\u093E\u0930", "mr");

        byte[] audioBytes = new byte[]{1, 2, 3, 4};
        VoiceToTextResponse response = client.transcribeAudio(audioBytes, "voice.webm", "whisper-1", "MR");

        assertNotNull(response);
        assertEquals("\u0928\u092E\u0938\u094D\u0915\u093E\u0930", response.getText());
        assertEquals("MR", response.getDetectedLanguage());

        ArgumentCaptor<MultiValueMap<String, Object>> bodyCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodySpec).body(bodyCaptor.capture());
        MultiValueMap<String, Object> body = bodyCaptor.getValue();

        assertEquals("mr", body.getFirst("language"));
        assertEquals("\u0928\u092E\u0938\u094D\u0915\u093E\u0930, \u092E\u0928 \u0915\u0940 \u0906\u0935\u093E\u091C \u092E\u0947\u0902 \u0906\u092A\u0915\u093E \u0938\u094D\u0935\u093E\u0917\u0924 \u0939\u0948\u0964", body.getFirst("prompt"));
    }

    @Test
    void testTranscribeAudio_Bengali_OmitsLanguageParamAndUsesBengaliPrompt() {
        setupMockRestClientResponse("\u0986\u09AA\u09A8\u09BF \u0995\u09C7\u09AE\u09A8 \u0986\u099B\u09C7\u09A8?", "bn");

        byte[] audioBytes = new byte[]{1, 2, 3, 4};
        VoiceToTextResponse response = client.transcribeAudio(audioBytes, "voice.webm", "whisper-1", "BN");

        assertNotNull(response);
        assertEquals("\u0986\u09AA\u09A8\u09BF \u0995\u09C7\u09AE\u09A8 \u0986\u099B\u09C7\u09A8?", response.getText());
        assertEquals("BN", response.getDetectedLanguage());

        ArgumentCaptor<MultiValueMap<String, Object>> bodyCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodySpec).body(bodyCaptor.capture());
        MultiValueMap<String, Object> body = bodyCaptor.getValue();

        assertNull(body.getFirst("language"), "Language parameter MUST be omitted for BN to prevent 400 Unsupported Language error");
        assertEquals("\u09A8\u09AE\u09B8\u09CD\u0995\u09BE\u09B0, \u09AE\u09A8 \u0995\u09BF \u0986\u0993\u09AF\u09BC\u09BE\u099C\u09C7 \u0986\u09AA\u09A8\u09BE\u0995\u09C7 \u09B8\u09CD\u09AC\u09BE\u0997\u09A4\u09AE\u0964", body.getFirst("prompt"));
    }

    @Test
    void testTranscribeAudio_Punjabi_OmitsLanguageParamAndUsesPunjabiPrompt() {
        setupMockRestClientResponse("\u0A24\u0A41\u0A38\u0A40\u0A02 \u0A15\u0A3F\u0A35\u0A47\u0A02 \u0A39\u0A4B?", "pa");

        byte[] audioBytes = new byte[]{1, 2, 3, 4};
        VoiceToTextResponse response = client.transcribeAudio(audioBytes, "voice.webm", "whisper-1", "PA");

        assertNotNull(response);
        assertEquals("\u0A24\u0A41\u0A38\u0A40\u0A02 \u0A15\u0A3F\u0A35\u0A47\u0A02 \u0A39\u0A4B?", response.getText());
        assertEquals("PA", response.getDetectedLanguage());

        ArgumentCaptor<MultiValueMap<String, Object>> bodyCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodySpec).body(bodyCaptor.capture());
        MultiValueMap<String, Object> body = bodyCaptor.getValue();

        assertNull(body.getFirst("language"), "Language parameter MUST be omitted for PA to prevent 400 Unsupported Language error");
        assertEquals("\u0A28\u0A2E\u0A38\u0A15\u0A3E\u0A30, \u0A2E\u0A28 \u0A15\u0A40 \u0A06\u0A35\u0A3E\u0A1C\u0A3C \u0A35\u0A3F\u0A71\u0A1A \u0A24\u0A41\u0A39\u0A3E\u0A21\u0A3E \u0A38\u0A35\u0A3E\u0A17\u0A24 \u0A39\u0A48\u0964", body.getFirst("prompt"));
    }

    @Test
    void testTranscribeAudio_Bhojpuri_PreservesRequestedLanguageAndSeparatesActualDetection() {
        setupMockRestClientResponse("\u0930\u090A\u0906 \u0915\u0908\u0938\u0928 \u092C\u093E\u0928\u0940?", "hi");

        byte[] audioBytes = new byte[]{1, 2, 3, 4};
        VoiceToTextResponse response = client.transcribeAudio(audioBytes, "voice.webm", "whisper-1", "BHO");

        assertNotNull(response);
        assertEquals("\u0930\u090A\u0906 \u0915\u0908\u0938\u0928 \u092C\u093E\u0928\u0940?", response.getText());
        assertEquals("HI", response.getDetectedLanguage());
        assertEquals("BHO", response.getRequestedLanguage());

        ArgumentCaptor<MultiValueMap<String, Object>> bodyCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodySpec).body(bodyCaptor.capture());
        MultiValueMap<String, Object> body = bodyCaptor.getValue();

        assertNull(body.getFirst("language"), "Language parameter MUST be omitted for BHO to prevent 400 Unsupported Language error");
        assertEquals("\u0928\u092E\u0938\u094D\u0915\u093E\u0930, \u0930\u093E\u0909\u0930 \u092E\u0928 \u0915\u0940 \u0906\u0935\u093E\u091C \u092E\u0947\u0902 \u0938\u094D\u0935\u093E\u0917\u0924 \u092C\u093E\u0964", body.getFirst("prompt"));
    }

    @Test
    void testTranscribeAudio_Telugu_OmitsLanguageParamAndUsesTeluguPrompt() {
        setupMockRestClientResponse("\u0C28\u0C2E\u0C38\u0C4D\u0C15\u0C3E\u0C30\u0C02, \u0C2E\u0C40\u0C30\u0C41 \u0C0E\u0C32\u0C3E \u0C09\u0C28\u0C4D\u0C28\u0C3E\u0C30\u0C41?", "te");

        byte[] audioBytes = new byte[]{1, 2, 3, 4};
        VoiceToTextResponse response = client.transcribeAudio(audioBytes, "voice.webm", "whisper-1", "TE");

        assertNotNull(response);
        assertEquals("\u0C28\u0C2E\u0C38\u0C4D\u0C15\u0C3E\u0C30\u0C02, \u0C2E\u0C40\u0C30\u0C41 \u0C0E\u0C32\u0C3E \u0C09\u0C28\u0C4D\u0C28\u0C3E\u0C30\u0C41?", response.getText());
        assertEquals("TE", response.getDetectedLanguage());
        assertEquals("TE", response.getRequestedLanguage());

        ArgumentCaptor<MultiValueMap<String, Object>> bodyCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodySpec).body(bodyCaptor.capture());
        MultiValueMap<String, Object> body = bodyCaptor.getValue();

        assertNull(body.getFirst("language"), "Language parameter MUST be omitted for TE to prevent 400 Unsupported Language error");
        assertEquals("\u0C28\u0C2E\u0C38\u0C4D\u0C15\u0C3E\u0C30\u0C02, \u0C2E\u0C28\u0C4D \u0C15\u0C40 \u0C06\u0C35\u0C3E\u0C1C\u0C4D", body.getFirst("prompt"));
    }

    @Test
    void testTranscribeAudio_Gujarati_OmitsLanguageParamAndUsesGujaratiPrompt() {
        setupMockRestClientResponse("\u0AA8\u0AAE\u0AB8\u0ACD\u0AA4\u0AC7, \u0AA4\u0AAE\u0AC7 \u0A95\u0AC7\u0AAE \u0A9B\u0ACB?", "gu");

        byte[] audioBytes = new byte[]{1, 2, 3, 4};
        VoiceToTextResponse response = client.transcribeAudio(audioBytes, "voice.webm", "whisper-1", "GU");

        assertNotNull(response);
        assertEquals("\u0AA8\u0AAE\u0AB8\u0ACD\u0AA4\u0AC7, \u0AA4\u0AAE\u0AC7 \u0A95\u0AC7\u0AAE \u0A9B\u0ACB?", response.getText());
        assertEquals("GU", response.getDetectedLanguage());
        assertEquals("GU", response.getRequestedLanguage());

        ArgumentCaptor<MultiValueMap<String, Object>> bodyCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodySpec).body(bodyCaptor.capture());
        MultiValueMap<String, Object> body = bodyCaptor.getValue();

        assertNull(body.getFirst("language"), "Language parameter MUST be omitted for GU to prevent 400 Unsupported Language error");
        assertEquals("\u0AA8\u0AAE\u0AB8\u0ACD\u0A24\u0A47, \u0AAE\u0AA8 \u0A95\u0A40 \u0A86\u0AB5\u0ABE\u0A9C\u0AAE\u0ABE\u0A82 \u0AA4\u0AAE\u0ABE\u0AB0\u0AC1\u0A82 \u0AB8\u0ACD\u0AB5\u0ABE\u0A97\u0AA4 \u0A9B\u0AC7\u0964", body.getFirst("prompt"));
    }

    @SuppressWarnings("unchecked")
    private void setupMockRestClientResponse(String transcribedText, String detectedLang) {
        java.util.Map<String, Object> resMap = new java.util.HashMap<>();
        if (transcribedText != null) resMap.put("text", transcribedText);
        if (detectedLang != null) resMap.put("language", detectedLang);

        doReturn(requestBodyUriSpec).when(restClient).post();
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(anyString());
        doReturn(requestBodySpec).when(requestBodySpec).header(anyString(), anyString());
        doReturn(requestBodySpec).when(requestBodySpec).contentType(any());
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        doReturn(responseSpec).when(requestBodySpec).retrieve();
        doReturn(resMap).when(responseSpec).body(any(Class.class));
    }
}

