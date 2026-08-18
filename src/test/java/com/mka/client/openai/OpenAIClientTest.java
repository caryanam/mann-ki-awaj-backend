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
        setupMockRestClientResponse("नमस्ते", "hi");

        byte[] audioBytes = new byte[]{1, 2, 3, 4};
        VoiceToTextResponse response = client.transcribeAudio(audioBytes, "voice.webm", "whisper-1", "HI");

        assertNotNull(response);
        assertEquals("नमस्ते", response.getText());
        assertEquals("HI", response.getDetectedLanguage());

        ArgumentCaptor<MultiValueMap<String, Object>> bodyCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodySpec).body(bodyCaptor.capture());
        MultiValueMap<String, Object> body = bodyCaptor.getValue();

        assertEquals("hi", body.getFirst("language"));
        assertEquals("नमस्कार, मन की आवाज में आपका स्वागत है।", body.getFirst("prompt"));
    }

    @Test
    void testTranscribeAudio_Marathi_SendsExplicitLanguageCodeAndPrompt() {
        setupMockRestClientResponse("नमस्कार", "mr");

        byte[] audioBytes = new byte[]{1, 2, 3, 4};
        VoiceToTextResponse response = client.transcribeAudio(audioBytes, "voice.webm", "whisper-1", "MR");

        assertNotNull(response);
        assertEquals("नमस्कार", response.getText());
        assertEquals("MR", response.getDetectedLanguage());

        ArgumentCaptor<MultiValueMap<String, Object>> bodyCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodySpec).body(bodyCaptor.capture());
        MultiValueMap<String, Object> body = bodyCaptor.getValue();

        assertEquals("mr", body.getFirst("language"));
        assertEquals("नमस्कार, मन की आवाज में आपका स्वागत है।", body.getFirst("prompt"));
    }

    @Test
    void testTranscribeAudio_Bengali_OmitsLanguageParamAndUsesBengaliPrompt() {
        setupMockRestClientResponse("আপনি কেমন আছেন?", "bn");

        byte[] audioBytes = new byte[]{1, 2, 3, 4};
        VoiceToTextResponse response = client.transcribeAudio(audioBytes, "voice.webm", "whisper-1", "BN");

        assertNotNull(response);
        assertEquals("আপনি কেমন আছেন?", response.getText());
        assertEquals("BN", response.getDetectedLanguage());

        ArgumentCaptor<MultiValueMap<String, Object>> bodyCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodySpec).body(bodyCaptor.capture());
        MultiValueMap<String, Object> body = bodyCaptor.getValue();

        assertNull(body.getFirst("language"), "Language parameter MUST be omitted for BN to prevent 400 Unsupported Language error");
        assertEquals("নমস্কার, মন কি আওয়াজে আপনাকে স্বাগতম।", body.getFirst("prompt"));
    }

    @Test
    void testTranscribeAudio_Punjabi_OmitsLanguageParamAndUsesPunjabiPrompt() {
        setupMockRestClientResponse("ਤੁਸੀਂ ਕਿਵੇਂ ਹੋ?", "pa");

        byte[] audioBytes = new byte[]{1, 2, 3, 4};
        VoiceToTextResponse response = client.transcribeAudio(audioBytes, "voice.webm", "whisper-1", "PA");

        assertNotNull(response);
        assertEquals("ਤੁਸੀਂ ਕਿਵੇਂ ਹੋ?", response.getText());
        assertEquals("PA", response.getDetectedLanguage());

        ArgumentCaptor<MultiValueMap<String, Object>> bodyCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodySpec).body(bodyCaptor.capture());
        MultiValueMap<String, Object> body = bodyCaptor.getValue();

        assertNull(body.getFirst("language"), "Language parameter MUST be omitted for PA to prevent 400 Unsupported Language error");
        assertEquals("ਨਮਸਕਾਰ, ਮਨ ਕੀ ਆਵਾਜ਼ ਵਿੱਚ ਤੁਹਾਡਾ ਸਵਾਗਤ ਹੈ।", body.getFirst("prompt"));
    }

    @Test
    void testTranscribeAudio_Bhojpuri_PreservesRequestedLanguageAndSeparatesActualDetection() {
        setupMockRestClientResponse("रऊआ कईसन बानी?", "hi");

        byte[] audioBytes = new byte[]{1, 2, 3, 4};
        VoiceToTextResponse response = client.transcribeAudio(audioBytes, "voice.webm", "whisper-1", "BHO");

        assertNotNull(response);
        assertEquals("रऊआ कईसन बानी?", response.getText());
        assertEquals("HI", response.getDetectedLanguage());
        assertEquals("BHO", response.getRequestedLanguage());

        ArgumentCaptor<MultiValueMap<String, Object>> bodyCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodySpec).body(bodyCaptor.capture());
        MultiValueMap<String, Object> body = bodyCaptor.getValue();

        assertNull(body.getFirst("language"), "Language parameter MUST be omitted for BHO to prevent 400 Unsupported Language error");
        assertEquals("नमस्कार, राउर मन की आवाज में स्वागत बा।", body.getFirst("prompt"));
    }

    @Test
    void testTranscribeAudio_Telugu_OmitsLanguageParamAndUsesTeluguPrompt() {
        setupMockRestClientResponse("నమస్కారం, మీరు ఎలా ఉన్నారు?", "te");

        byte[] audioBytes = new byte[]{1, 2, 3, 4};
        VoiceToTextResponse response = client.transcribeAudio(audioBytes, "voice.webm", "whisper-1", "TE");

        assertNotNull(response);
        assertEquals("నమస్కారం, మీరు ఎలా ఉన్నారు?", response.getText());
        assertEquals("TE", response.getDetectedLanguage());
        assertEquals("TE", response.getRequestedLanguage());

        ArgumentCaptor<MultiValueMap<String, Object>> bodyCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodySpec).body(bodyCaptor.capture());
        MultiValueMap<String, Object> body = bodyCaptor.getValue();

        assertNull(body.getFirst("language"), "Language parameter MUST be omitted for TE to prevent 400 Unsupported Language error");
        assertEquals("నమస్కారం, మన్ కీ ఆవాజ్", body.getFirst("prompt"));
    }

    @Test
    void testTranscribeAudio_Gujarati_OmitsLanguageParamAndUsesGujaratiPrompt() {
        setupMockRestClientResponse("નમસ્તે, તમે કેમ છો?", "gu");

        byte[] audioBytes = new byte[]{1, 2, 3, 4};
        VoiceToTextResponse response = client.transcribeAudio(audioBytes, "voice.webm", "whisper-1", "GU");

        assertNotNull(response);
        assertEquals("નમસ્ਤੇ, તમે કેમ છો?", response.getText());
        assertEquals("GU", response.getDetectedLanguage());
        assertEquals("GU", response.getRequestedLanguage());

        ArgumentCaptor<MultiValueMap<String, Object>> bodyCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodySpec).body(bodyCaptor.capture());
        MultiValueMap<String, Object> body = bodyCaptor.getValue();

        assertNull(body.getFirst("language"), "Language parameter MUST be omitted for GU to prevent 400 Unsupported Language error");
        assertEquals("નમસ્ਤੇ, મન કੀ આવાજમાં તમારું સ્વાગત છે।", body.getFirst("prompt"));
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
        doReturn(requestBodySpec).when(requestBodySpec).body(any());
        doReturn(responseSpec).when(requestBodySpec).retrieve();
        doReturn(resMap).when(responseSpec).body(any());
    }
}
