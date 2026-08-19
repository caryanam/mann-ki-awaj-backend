package com.mka.client.openai;

import com.mka.config.openai.OpenAIProperties;
import com.mka.dto.openai.OpenAIHealthResponse;
import com.mka.dto.response.ModerationResult;
import com.mka.exception.openai.OpenAiAuthException;
import com.mka.exception.openai.OpenAiRateLimitException;
import com.mka.exception.openai.OpenAiTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service implementation of OpenAIClient.
 * Builds authenticated requests to OpenAI API while protecting secrets from logs and exceptions.
 */
@Service
public class OpenAIClientImpl implements OpenAIClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAIClientImpl.class);

    private final RestClient restClient;
    private final OpenAIProperties properties;

    public OpenAIClientImpl(@Qualifier("openAiRestClient") RestClient restClient,
                            OpenAIProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public boolean isConfigured() {
        String apiKey = properties.getApiKey();
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.startsWith("${");
    }

    @Override
    public OpenAIHealthResponse checkConnection() {
        if (!isConfigured()) {
            log.warn("OpenAI API key check failed: OPENAI_API_KEY is not configured.");
            return OpenAIHealthResponse.builder()
                    .status("MISCONFIGURED")
                    .connected(false)
                    .provider("OpenAI")
                    .translationModel(properties.getTranslationModel())
                    .transcriptionModel(properties.getTranscriptionModel())
                    .moderationModel(properties.getModerationModel())
                    .message("OPENAI_API_KEY environment variable is missing or empty.")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        try {
            log.info("Testing connection to OpenAI API endpoint...");
            HttpStatusCode statusCode = restClient.get()
                    .uri("/models")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .retrieve()
                    .onStatus(status -> status.value() == 401, (req, resp) -> {
                        log.error("OpenAI authentication failed: HTTP 401 Unauthorized.");
                        throw new OpenAiAuthException("Authentication with OpenAI failed: Invalid or expired API Key.");
                    })
                    .onStatus(status -> status.value() == 429, (req, resp) -> {
                        log.error("OpenAI rate limit / quota exceeded: HTTP 429.");
                        throw new OpenAiRateLimitException("OpenAI quota exceeded or rate limited.");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        log.error("OpenAI server error: HTTP {}", resp.getStatusCode().value());
                        throw new RuntimeException("OpenAI service unavailable: HTTP " + resp.getStatusCode().value());
                    })
                    .toBodilessEntity()
                    .getStatusCode();

            boolean success = statusCode.is2xxSuccessful();
            log.info("OpenAI API connection test result: HTTP {}", statusCode.value());

            return OpenAIHealthResponse.builder()
                    .status(success ? "UP" : "DOWN")
                    .connected(success)
                    .provider("OpenAI")
                    .translationModel(properties.getTranslationModel())
                    .transcriptionModel(properties.getTranscriptionModel())
                    .moderationModel(properties.getModerationModel())
                    .message(success ? "OpenAI API connection successful and authenticated." : "Unexpected HTTP status: " + statusCode)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (OpenAiAuthException ex) {
            return OpenAIHealthResponse.builder()
                    .status("UNAUTHORIZED")
                    .connected(false)
                    .provider("OpenAI")
                    .translationModel(properties.getTranslationModel())
                    .transcriptionModel(properties.getTranscriptionModel())
                    .moderationModel(properties.getModerationModel())
                    .message(ex.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (OpenAiRateLimitException ex) {
            return OpenAIHealthResponse.builder()
                    .status("RATE_LIMITED")
                    .connected(false)
                    .provider("OpenAI")
                    .translationModel(properties.getTranslationModel())
                    .transcriptionModel(properties.getTranscriptionModel())
                    .moderationModel(properties.getModerationModel())
                    .message(ex.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (ResourceAccessException ex) {
            log.error("OpenAI API connection timed out or unreachable: {}", ex.getMessage());
            return OpenAIHealthResponse.builder()
                    .status("DOWN")
                    .connected(false)
                    .provider("OpenAI")
                    .translationModel(properties.getTranslationModel())
                    .transcriptionModel(properties.getTranscriptionModel())
                    .moderationModel(properties.getModerationModel())
                    .message("OpenAI request timed out or network unreachable.")
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (RestClientResponseException ex) {
            log.error("OpenAI API returned error status HTTP {}: {}", ex.getStatusCode().value(), ex.getStatusText());
            return OpenAIHealthResponse.builder()
                    .status("DOWN")
                    .connected(false)
                    .provider("OpenAI")
                    .translationModel(properties.getTranslationModel())
                    .transcriptionModel(properties.getTranscriptionModel())
                    .moderationModel(properties.getModerationModel())
                    .message("OpenAI API responded with HTTP status " + ex.getStatusCode().value())
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (Exception ex) {
            log.error("Unexpected error during OpenAI connection test: {}", ex.getMessage());
            return OpenAIHealthResponse.builder()
                    .status("DOWN")
                    .connected(false)
                    .provider("OpenAI")
                    .translationModel(properties.getTranslationModel())
                    .transcriptionModel(properties.getTranscriptionModel())
                    .moderationModel(properties.getModerationModel())
                    .message("Unexpected error establishing connection to OpenAI.")
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    public ModerationResult moderateMultimodal(String text, byte[] imageBytes, String imageMimeType) {
        if (!isConfigured()) {
            log.error("MODERATION_FAILED: OPENAI_API_KEY is missing or not configured.");
            return ModerationResult.failClosed("OPENAI_NOT_CONFIGURED", "Content safety verification is temporarily unavailable. Please try again.");
        }

        List<Map<String, Object>> inputList = new ArrayList<>();

        if (text != null && !text.isBlank()) {
            inputList.add(Map.of("type", "text", "text", text.trim()));
        }

        if (imageBytes != null && imageBytes.length > 0) {
            String mime = (imageMimeType != null && !imageMimeType.isBlank()) ? imageMimeType : "image/jpeg";
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String dataUrl = "data:" + mime + ";base64," + base64Image;
            inputList.add(Map.of("type", "image_url", "image_url", Map.of("url", dataUrl)));
        }

        if (inputList.isEmpty()) {
            return ModerationResult.approved();
        }

        Map<String, Object> requestBody = Map.of(
                "model", "omni-moderation-latest",
                "input", inputList
        );

        try {
            log.info("MODERATION_STARTED: Invoking OpenAI /v1/moderations API using model omni-moderation-latest (Inputs count: {})...", inputList.size());

            Map responseMap = restClient.post()
                    .uri("/moderations")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (responseMap == null || !responseMap.containsKey("results")) {
                log.error("MODERATION_FAILED: OpenAI Moderation API returned null or missing results key.");
                return ModerationResult.failClosed("INVALID_RESPONSE", "Content safety verification is temporarily unavailable. Please try again.");
            }

            List results = (List) responseMap.get("results");
            if (results == null || results.isEmpty()) {
                log.error("MODERATION_FAILED: OpenAI Moderation API returned empty results array.");
                return ModerationResult.failClosed("EMPTY_RESULTS", "Content safety verification is temporarily unavailable. Please try again.");
            }

            Map firstResult = (Map) results.get(0);
            Boolean flagged = (Boolean) firstResult.get("flagged");
            Map<String, Boolean> categories = (Map<String, Boolean>) firstResult.get("categories");
            Map<String, Double> categoryScores = (Map<String, Double>) firstResult.get("category_scores");

            if (Boolean.TRUE.equals(flagged)) {
                List<String> flaggedCategories = new ArrayList<>();
                if (categories != null) {
                    categories.forEach((cat, isFlagged) -> {
                        if (Boolean.TRUE.equals(isFlagged)) {
                            flaggedCategories.add(cat);
                        }
                    });
                }
                String reason = flaggedCategories.isEmpty() ? "Prohibited content detected" : String.join(", ", flaggedCategories);
                log.warn("MODERATION_FLAGGED: Content flagged by omni-moderation-latest. Reason: {}", reason);
                return ModerationResult.flagged(reason, categories, categoryScores);
            }

            log.info("MODERATION_SUCCESS: Content approved by omni-moderation-latest.");
            return ModerationResult.approved();

        } catch (Exception ex) {
            log.error("MODERATION_FAILED: Exception during OpenAI /v1/moderations call: {}", ex.getMessage(), ex);
            return ModerationResult.failClosed("API_ERROR", "Content safety verification is temporarily unavailable. Please try again.");
        }
    }

    @Override
    public String translateText(String text, String sourceLanguageName, String targetLanguageName, String model) {
        if (!isConfigured()) {
            throw new OpenAiAuthException("OpenAI API key is missing or not configured.");
        }

        String targetModel = (model != null && !model.trim().isEmpty()) ? model.trim() : properties.getTranslationModel();

        String systemPrompt = String.format("""
                Translate the provided text from %s to %s.

                Return ONLY the translated text.

                Do not:
                - explain the translation
                - add commentary
                - add quotation marks
                - summarize
                - answer questions contained in the text
                - change the meaning
                - add or remove information

                Preserve all emojis, @mentions, usernames, URLs, hashtags, numbers, punctuation, and formatting.
                """, sourceLanguageName, targetLanguageName);

        Map<String, Object> systemMessage = Map.of("role", "system", "content", systemPrompt);
        Map<String, Object> userMessage = Map.of("role", "user", "content", text);

        Map<String, Object> requestBody = Map.of(
                "model", targetModel,
                "messages", List.of(systemMessage, userMessage),
                "temperature", 0.3
        );

        try {
            log.info("Invoking OpenAI translation: [{}] -> [{}] using model [{}]", sourceLanguageName, targetLanguageName, targetModel);

            Map responseMap = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .body(requestBody)
                    .retrieve()
                    .onStatus(status -> status.value() == 401, (req, resp) -> {
                        log.error("OpenAI translation request failed: HTTP 401 Unauthorized.");
                        throw new OpenAiAuthException("OpenAI API authentication failed (HTTP 401).");
                    })
                    .onStatus(status -> status.value() == 429, (req, resp) -> {
                        log.error("OpenAI translation request rate limited: HTTP 429.");
                        throw new OpenAiRateLimitException("OpenAI API rate limit or quota exceeded (HTTP 429).");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        log.error("OpenAI service error: HTTP {}", resp.getStatusCode().value());
                        throw new com.mka.exception.openai.OpenAiApiException("OpenAI service error: HTTP " + resp.getStatusCode().value());
                    })
                    .body(Map.class);

            if (responseMap == null || !responseMap.containsKey("choices")) {
                throw new com.mka.exception.openai.OpenAiApiException("Invalid response structure received from OpenAI API.");
            }

            List choices = (List) responseMap.get("choices");
            if (choices.isEmpty()) {
                throw new com.mka.exception.openai.OpenAiApiException("OpenAI API returned empty choices list.");
            }

            Map choice = (Map) choices.get(0);
            Map message = (Map) choice.get("message");
            String translatedContent = (String) message.get("content");

            if (translatedContent == null) {
                throw new com.mka.exception.openai.OpenAiApiException("OpenAI API returned null content in message.");
            }

            return translatedContent.trim();

        } catch (OpenAiAuthException | OpenAiRateLimitException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            log.error("OpenAI API translation request timed out: {}", ex.getMessage());
            throw new OpenAiTimeoutException("OpenAI translation connection timed out.", ex);
        } catch (RestClientResponseException ex) {
            log.error("OpenAI API returned HTTP error status {}: {}", ex.getStatusCode().value(), ex.getStatusText());
            throw new com.mka.exception.openai.OpenAiApiException("OpenAI API responded with HTTP status " + ex.getStatusCode().value(), ex);
        } catch (Exception ex) {
            log.error("Unexpected error during OpenAI translation execution: {}", ex.getMessage());
            throw new com.mka.exception.openai.OpenAiApiException("Error executing translation with OpenAI: " + ex.getMessage(), ex);
        }
    }

    @Override
    public com.mka.dto.response.VoiceToTextResponse transcribeAudio(byte[] audioBytes, String fileName, String model, String language) {
        if (!isConfigured()) {
            log.warn("OpenAI API key check failed: OPENAI_API_KEY is not configured for audio transcription.");
            throw new OpenAiAuthException("OpenAI API key is missing or not configured.");
        }

        if (audioBytes == null || audioBytes.length == 0) {
            throw new com.mka.exception.openai.OpenAiApiException("Audio content cannot be empty.");
        }

        String targetModel = (model != null && !model.trim().isEmpty()) ? model.trim() : properties.getTranscriptionModel();
        if (targetModel == null || targetModel.trim().isEmpty() || targetModel.startsWith("gpt-4o-mini")) {
            targetModel = "whisper-1";
        }

        String safeFileName = (fileName != null && !fileName.trim().isEmpty()) ? fileName.trim() : "voice_recording.webm";

        org.springframework.core.io.ByteArrayResource contentsAsResource = new org.springframework.core.io.ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return safeFileName;
            }
        };

        String whisperLangCode = mapToWhisperLanguageCode(language);
        String whisperPrompt = mapToWhisperPrompt(language);

        org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
        body.add("file", contentsAsResource);
        body.add("model", targetModel);
        body.add("response_format", "verbose_json");

        if (whisperLangCode != null) {
            body.add("language", whisperLangCode);
        }

        if (whisperPrompt != null) {
            body.add("prompt", whisperPrompt);
        }

        try {
            log.info("Invoking OpenAI Audio Transcription API [/audio/transcriptions] for file [{}] ({} bytes)...", safeFileName, audioBytes.length);

            Map responseMap = restClient.post()
                    .uri("/audio/transcriptions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (responseMap == null || !responseMap.containsKey("text")) {
                throw new com.mka.exception.openai.OpenAiApiException("Invalid response received from OpenAI Audio Transcription API.");
            }

            String transcribedText = (String) responseMap.get("text");
            String rawLanguage = (String) responseMap.get("language");
            String actualDetectedLang = mapWhisperLanguageToIsoCode(rawLanguage, null);
            String normalizedRequestedLang = normalizeRequestedLanguageCode(language);

            return com.mka.dto.response.VoiceToTextResponse.builder()
                    .text(transcribedText != null ? transcribedText.trim() : "")
                    .detectedLanguage(actualDetectedLang != null ? actualDetectedLang : normalizedRequestedLang)
                    .requestedLanguage(normalizedRequestedLang)
                    .build();

        } catch (Exception ex) {
            log.error("Unexpected error during OpenAI audio transcription execution: {}", ex.getMessage());
            throw new com.mka.exception.openai.OpenAiApiException("Error executing audio transcription with OpenAI: " + ex.getMessage(), ex);
        }
    }

    private String normalizeRequestedLanguageCode(String inputLang) {
        if (inputLang == null || inputLang.trim().isEmpty() || inputLang.equalsIgnoreCase("AUTO")) return "AUTO";
        String lang = inputLang.trim().toLowerCase(Locale.ROOT);
        return switch (lang) {
            case "bengali", "bn" -> "BN";
            case "punjabi", "pa" -> "PA";
            case "marathi", "mr" -> "MR";
            case "hindi", "hi" -> "HI";
            case "english", "en" -> "EN";
            default -> lang.length() <= 3 ? lang.toUpperCase(Locale.ROOT) : "AUTO";
        };
    }

    private String mapToWhisperLanguageCode(String inputLang) {
        if (inputLang == null || inputLang.trim().isEmpty() || inputLang.equalsIgnoreCase("AUTO")) return null;
        String lang = inputLang.trim().toLowerCase(Locale.ROOT);
        return switch (lang) {
            case "marathi", "mr" -> "mr";
            case "hindi", "hi" -> "hi";
            case "english", "en" -> "en";
            default -> null;
        };
    }

    private String mapToWhisperPrompt(String inputLang) {
        if (inputLang == null || inputLang.trim().isEmpty()) return null;
        String lang = inputLang.trim().toLowerCase(Locale.ROOT);
        return switch (lang) {
            case "hindi", "hi" -> "\u0928\u092E\u0938\u094D\u0915\u093E\u0930, \u092E\u0928 \u0915\u0940 \u0906\u0935\u093E\u091C \u092E\u0947\u0902 \u0906\u092A\u0915\u093E \u0938\u094D\u0935\u093E\u0917\u0924 \u0939\u0948\u0964";
            case "marathi", "mr" -> "\u0928\u092E\u0938\u094D\u0915\u093E\u0930, \u092E\u0928 \u0915\u0940 \u0906\u0935\u093E\u091C \u092E\u0947\u0902 \u0906\u092A\u0915\u093E \u0938\u094D\u0935\u093E\u0917\u0924 \u0939\u0948\u0964";
            case "bengali", "bn" -> "\u09A8\u09AE\u09B8\u09CD\u0995\u09BE\u09B0, \u09AE\u09A8 \u0995\u09BF \u0986\u0993\u09AF\u09BC\u09BE\u099C\u09C7 \u0986\u09AA\u09A8\u09BE\u0995\u09C7 \u09B8\u09CD\u09AC\u09BE\u0997\u09A4\u09AE\u0964";
            case "punjabi", "pa" -> "\u0A28\u0A2E\u0A38\u0A15\u0A3E\u0A30, \u0A2E\u0A28 \u0A15\u0A40 \u0A06\u0A35\u0A3E\u0A1C\u0A3C \u0A35\u0A3F\u0A71\u0A1A \u0A24\u0A41\u0A39\u0A3E\u0A21\u0A3E \u0A38\u0A35\u0A3E\u0A17\u0A24 \u0A39\u0A48\u0964";
            case "bhojpuri", "bho" -> "\u0928\u092E\u0938\u094D\u0915\u093E\u0930, \u0930\u093E\u0909\u0930 \u092E\u0928 \u0915\u0940 \u0906\u0935\u093E\u091C \u092E\u0947\u0902 \u0938\u094D\u0935\u093E\u0917\u0924 \u092C\u093E\u0964";
            case "telugu", "te" -> "\u0C28\u0C2E\u0C38\u0C4D\u0C15\u0C3E\u0C30\u0C02, \u0C2E\u0C28\u0C4D \u0C15\u0C40 \u0C06\u0C35\u0C3E\u0C1C\u0C4D";
            case "gujarati", "gu" -> "\u0AA8\u0AAE\u0AB8\u0ACD\u0A24\u0A47, \u0AAE\u0AA8 \u0A95\u0A40 \u0A86\u0AB5\u0ABE\u0A9C\u0AAE\u0ABE\u0A82 \u0AA4\u0AAE\u0ABE\u0AB0\u0AC1\u0A82 \u0AB8\u0ACD\u0AB5\u0ABE\u0A97\u0AA4 \u0A9B\u0AC7\u0964";
            default -> null;
        };
    }

    private String mapWhisperLanguageToIsoCode(String rawLang, String inputRequestedLang) {
        if (rawLang != null && !rawLang.trim().isEmpty()) {
            String lang = rawLang.trim().toLowerCase(Locale.ROOT);
            return switch (lang) {
                case "marathi", "mr" -> "MR";
                case "hindi", "hi" -> "HI";
                case "english", "en" -> "EN";
                default -> lang.length() <= 3 ? lang.toUpperCase(Locale.ROOT) : "EN";
            };
        }
        return "EN";
    }

    @Override
    public String moderateText(String text) {
        ModerationResult res = moderateMultimodal(text, null, null);
        if (!res.isSuccessful()) return "SAFE";
        return res.isFlagged() ? "UNSAFE: " + res.getReason() : "SAFE";
    }

    @Override
    public String moderateImage(byte[] imageBytes, String mimeType) {
        ModerationResult res = moderateMultimodal(null, imageBytes, mimeType);
        if (!res.isSuccessful()) return "SAFE";
        return res.isFlagged() ? "UNSAFE: " + res.getReason() : "SAFE";
    }
}
