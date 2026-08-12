package com.mka.client.openai;

import com.mka.config.openai.OpenAIProperties;
import com.mka.dto.openai.OpenAIHealthResponse;
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

        java.util.Map<String, Object> systemMessage = java.util.Map.of("role", "system", "content", systemPrompt);
        java.util.Map<String, Object> userMessage = java.util.Map.of("role", "user", "content", text);

        java.util.Map<String, Object> requestBody = java.util.Map.of(
                "model", targetModel,
                "messages", java.util.List.of(systemMessage, userMessage),
                "temperature", 0.3
        );

        try {
            log.info("Invoking OpenAI translation: [{}] -> [{}] using model [{}]", sourceLanguageName, targetLanguageName, targetModel);

            java.util.Map responseMap = restClient.post()
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
                    .body(java.util.Map.class);

            if (responseMap == null || !responseMap.containsKey("choices")) {
                throw new com.mka.exception.openai.OpenAiApiException("Invalid response structure received from OpenAI API.");
            }

            java.util.List choices = (java.util.List) responseMap.get("choices");
            if (choices.isEmpty()) {
                throw new com.mka.exception.openai.OpenAiApiException("OpenAI API returned empty choices list.");
            }

            java.util.Map choice = (java.util.Map) choices.get(0);
            java.util.Map message = (java.util.Map) choice.get("message");
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

        // Map target language code (e.g. 'MR' -> 'mr', 'HI' -> 'hi', 'EN' -> 'en')
        String whisperLangCode = mapToWhisperLanguageCode(language);

        org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
        body.add("file", contentsAsResource);
        body.add("model", targetModel);
        body.add("response_format", "verbose_json");

        // OpenAI /audio/transcriptions transcribes audio into text in the specified language (e.g. 'mr', 'hi', 'en')
        if (whisperLangCode != null) {
            body.add("language", whisperLangCode);
        }

        // Native-script prompt hints to guide speech decoding and script consistency
        String cleanRequested = (language != null) ? language.trim().toLowerCase(java.util.Locale.ROOT) : "";
        if (cleanRequested.equals("bengali") || cleanRequested.equals("bn") || cleanRequested.equals("ben_beng")) {
            body.add("prompt", "নমস্কার, মন কি আওয়াজে আপনাকে স্বাগতম।");
        } else if (cleanRequested.equals("punjabi") || cleanRequested.equals("pa") || cleanRequested.equals("pan_guru")) {
            body.add("prompt", "ਨਮਸਕਾਰ, ਮਨ ਕੀ ਆਵਾਜ਼ ਵਿੱਚ ਤੁਹਾਡਾ ਸਵਾਗਤ ਹੈ।");
        } else if (cleanRequested.equals("bhojpuri") || cleanRequested.equals("bho") || cleanRequested.equals("bho_deva")) {
            body.add("prompt", "नमस्कार, राउर मन की आवाज में स्वागत बा।");
        } else if (cleanRequested.equals("santali") || cleanRequested.equals("sat") || cleanRequested.equals("sat_olck")) {
            body.add("prompt", "ᱥᱟᱜᱩᱱ ᱫᱟᱨᱟᱢ");
        } else if (cleanRequested.equals("kashmiri") || cleanRequested.equals("ks") || cleanRequested.equals("kas_deva")) {
            body.add("prompt", "नमस्कार, कॉशुर मन की आवाज");
        } else if (cleanRequested.equals("manipuri") || cleanRequested.equals("mni") || cleanRequested.equals("mni_beng")) {
            body.add("prompt", "তরাম্না ওকচরি");
        } else if (cleanRequested.equals("dogri") || cleanRequested.equals("doi") || cleanRequested.equals("doi_deva")) {
            body.add("prompt", "नमस्कार, डोगरी मन की आवाज में आपका स्वागत है।");
        } else if (cleanRequested.equals("telugu") || cleanRequested.equals("te") || cleanRequested.equals("tel_telu")) {
            body.add("prompt", "నమస్కారం, మన్ కీ ఆవాజ్");
        } else if (whisperLangCode == null || whisperLangCode.equals("hi") || whisperLangCode.equals("mr") || whisperLangCode.equals("ur")) {
            body.add("prompt", "नमस्कार, मन की आवाज में आपका स्वागत है।");
        }

        try {
            log.info("Invoking OpenAI Audio Transcription API [/audio/transcriptions] for file [{}] ({} bytes) using model [{}] and language [{}]",
                    safeFileName, audioBytes.length, targetModel, whisperLangCode != null ? whisperLangCode : "AUTO (" + (language != null ? language : "AUTO") + ")");

            java.util.Map responseMap = restClient.post()
                    .uri("/audio/transcriptions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .onStatus(status -> status.value() == 401, (req, resp) -> {
                        log.error("OpenAI audio transcription failed: HTTP 401 Unauthorized.");
                        throw new OpenAiAuthException("OpenAI API authentication failed (HTTP 401).");
                    })
                    .onStatus(status -> status.value() == 429, (req, resp) -> {
                        log.error("OpenAI audio transcription rate limited: HTTP 429.");
                        throw new OpenAiRateLimitException("OpenAI API rate limit or quota exceeded (HTTP 429).");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        log.error("OpenAI service error during audio transcription: HTTP {}", resp.getStatusCode().value());
                        throw new com.mka.exception.openai.OpenAiApiException("OpenAI service error: HTTP " + resp.getStatusCode().value());
                    })
                    .body(java.util.Map.class);

            if (responseMap == null || !responseMap.containsKey("text")) {
                throw new com.mka.exception.openai.OpenAiApiException("Invalid response received from OpenAI Audio Transcription API.");
            }

            String transcribedText = (String) responseMap.get("text");
            String rawLanguage = (String) responseMap.get("language");
            String actualDetectedLang = mapWhisperLanguageToIsoCode(rawLanguage, null);
            String normalizedRequestedLang = normalizeRequestedLanguageCode(language);

            log.info("OpenAI Audio Transcription successful. Text length: {}, Detected Language: {}, Requested Language: {}",
                    transcribedText != null ? transcribedText.length() : 0, actualDetectedLang, normalizedRequestedLang);

            return com.mka.dto.response.VoiceToTextResponse.builder()
                    .text(transcribedText != null ? transcribedText.trim() : "")
                    .detectedLanguage(actualDetectedLang != null ? actualDetectedLang : normalizedRequestedLang)
                    .requestedLanguage(normalizedRequestedLang)
                    .build();

        } catch (OpenAiAuthException | OpenAiRateLimitException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            log.error("OpenAI API audio transcription request timed out: {}", ex.getMessage());
            throw new OpenAiTimeoutException("OpenAI audio transcription connection timed out.", ex);
        } catch (RestClientResponseException ex) {
            String responseBody = ex.getResponseBodyAsString();
            log.error("OpenAI API audio transcription returned HTTP error status {} for file [{}] and requested language [{}]. Response body: {}",
                    ex.getStatusCode().value(), safeFileName, language != null ? language : "AUTO", responseBody);
            throw new com.mka.exception.openai.OpenAiApiException("OpenAI API responded with HTTP status " + ex.getStatusCode().value() + ": " + responseBody, ex);
        } catch (Exception ex) {
            log.error("Unexpected error during OpenAI audio transcription execution: {}", ex.getMessage());
            throw new com.mka.exception.openai.OpenAiApiException("Error executing audio transcription with OpenAI: " + ex.getMessage(), ex);
        }
    }

    private String normalizeRequestedLanguageCode(String inputLang) {
        if (inputLang == null || inputLang.trim().isEmpty() || inputLang.equalsIgnoreCase("AUTO")) {
            return "AUTO";
        }
        String lang = inputLang.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (lang) {
            case "bengali", "bn", "ben_beng" -> "BN";
            case "punjabi", "pa", "pan_guru" -> "PA";
            case "marathi", "mr", "mar_deva" -> "MR";
            case "hindi", "hi", "hin_deva" -> "HI";
            case "tamil", "ta", "tam_taml" -> "TA";
            case "telugu", "te", "tel_telu" -> "TE";
            case "gujarati", "gu", "guj_gujr" -> "GU";
            case "kannada", "kn", "kan_knda" -> "KN";
            case "malayalam", "ml", "mal_mlym" -> "ML";
            case "odia", "or", "ory_orya" -> "OR";
            case "assamese", "as", "asm_beng" -> "AS";
            case "urdu", "ur", "urd_arab" -> "UR";
            case "english", "en", "eng_latn" -> "EN";
            case "santali", "sat", "sat_olck" -> "SAT";
            case "kashmiri", "ks", "kas_deva" -> "KS";
            case "manipuri", "mni", "mni_beng" -> "MNI";
            case "dogri", "doi", "doi_deva" -> "DOI";
            case "bhojpuri", "bho", "bho_deva" -> "BHO";
            default -> lang.length() <= 3 ? lang.toUpperCase(java.util.Locale.ROOT) : "AUTO";
        };
    }

    private String mapToWhisperLanguageCode(String inputLang) {
        if (inputLang == null || inputLang.trim().isEmpty() || inputLang.equalsIgnoreCase("AUTO")) {
            return null;
        }
        String lang = inputLang.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (lang) {
            case "marathi", "mr", "mar_deva" -> "mr";
            case "hindi", "hi", "hin_deva" -> "hi";
            case "tamil", "ta", "tam_taml" -> "ta";
            case "kannada", "kn", "kan_knda" -> "kn";
            case "urdu", "ur", "urd_arab" -> "ur";
            case "english", "en", "eng_latn" -> "en";
            case "gujarati", "gu", "guj_gujr" -> "gu";
            case "malayalam", "ml", "mal_mlym" -> "ml";
            case "odia", "or", "ory_orya" -> "or";
            case "assamese", "as", "asm_beng" -> "as";
            // Omit explicit language parameter for BN, PA, TE, SAT, KS, MNI, DOI, BHO
            // to allow Whisper auto-detection & script prompt hinting without triggering HTTP 400
            case "bengali", "bn", "ben_beng", "punjabi", "pa", "pan_guru",
                 "telugu", "te", "tel_telu", "bhojpuri", "bho", "bho_deva",
                 "santali", "sat", "sat_olck", "kashmiri", "ks", "kas_deva",
                 "manipuri", "mni", "mni_beng", "dogri", "doi", "doi_deva" -> null;
            default -> null;
        };
    }

    private String mapWhisperLanguageToIsoCode(String rawLang, String inputRequestedLang) {
        String cleanRequested = (inputRequestedLang != null) ? inputRequestedLang.trim().toLowerCase(java.util.Locale.ROOT) : "";

        if (rawLang != null && !rawLang.trim().isEmpty()) {
            String lang = rawLang.trim().toLowerCase(java.util.Locale.ROOT);
            String mapped = switch (lang) {
                case "marathi", "mr" -> "MR";
                case "hindi", "hi" -> "HI";
                case "bengali", "bn" -> "BN";
                case "tamil", "ta" -> "TA";
                case "telugu", "te" -> "TE";
                case "gujarati", "gu" -> "GU";
                case "punjabi", "pa" -> "PA";
                case "kannada", "kn" -> "KN";
                case "malayalam", "ml" -> "ML";
                case "odia", "or" -> "OR";
                case "assamese", "as" -> "AS";
                case "urdu", "ur" -> "UR";
                case "english", "en" -> "EN";
                case "santali", "sat" -> "SAT";
                case "kashmiri", "ks" -> "KS";
                case "manipuri", "mni" -> "MNI";
                case "dogri", "doi" -> "DOI";
                case "bhojpuri", "bho" -> "BHO";
                default -> lang.length() <= 3 ? lang.toUpperCase(java.util.Locale.ROOT) : null;
            };
            if (mapped != null) {
                return mapped;
            }
        }

        return switch (cleanRequested) {
            case "bengali", "bn", "ben_beng" -> "BN";
            case "punjabi", "pa", "pan_guru" -> "PA";
            case "marathi", "mr", "mar_deva" -> "MR";
            case "hindi", "hi", "hin_deva" -> "HI";
            case "tamil", "ta", "tam_taml" -> "TA";
            case "telugu", "te", "tel_telu" -> "TE";
            case "gujarati", "gu", "guj_gujr" -> "GU";
            case "kannada", "kn", "kan_knda" -> "KN";
            case "malayalam", "ml", "mal_mlym" -> "ML";
            case "odia", "or", "ory_orya" -> "OR";
            case "assamese", "as", "asm_beng" -> "AS";
            case "urdu", "ur", "urd_arab" -> "UR";
            case "english", "en", "eng_latn" -> "EN";
            case "santali", "sat", "sat_olck" -> "SAT";
            case "kashmiri", "ks", "kas_deva" -> "KS";
            case "manipuri", "mni", "mni_beng" -> "MNI";
            case "dogri", "doi", "doi_deva" -> "DOI";
            case "bhojpuri", "bho", "bho_deva" -> "BHO";
            default -> "EN";
        };
    }
}
