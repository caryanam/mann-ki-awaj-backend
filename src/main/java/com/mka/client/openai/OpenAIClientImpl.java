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
}
