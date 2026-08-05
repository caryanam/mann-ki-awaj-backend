package com.mka.translation.client;

import com.mka.translation.dto.TranslationRequest;
import com.mka.translation.dto.TranslationResponse;
import com.mka.translation.exception.TranslationFailedException;
import com.mka.translation.exception.TranslationRequestException;
import com.mka.translation.exception.TranslationServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Production-ready implementation of IndicTrans2Client using Spring RestClient.
 */
@Component
public class IndicTrans2ClientImpl implements IndicTrans2Client {

    private static final Logger log = LoggerFactory.getLogger(IndicTrans2ClientImpl.class);
    private final RestClient restClient;

    public IndicTrans2ClientImpl(RestClient translationRestClient) {
        this.restClient = translationRestClient;
    }

    @Override
    public TranslationResponse translate(TranslationRequest request) {
        try {
            return restClient.post()
                    .uri("/api/v1/translate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        log.error("Translation client error: HTTP {}", resp.getStatusCode());
                        throw new TranslationRequestException("Invalid translation request: HTTP " + resp.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        log.error("Translation server error: HTTP {}", resp.getStatusCode());
                        throw new TranslationFailedException("Translation engine failure: HTTP " + resp.getStatusCode());
                    })
                    .body(TranslationResponse.class);
        } catch (ResourceAccessException ex) {
            log.error("Translation service unreachable or timed out: {}", ex.getMessage());
            throw new TranslationServiceUnavailableException("Translation service connection timed out or unreachable", ex);
        } catch (RestClientResponseException ex) {
            log.error("Translation API responded with error code {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new TranslationFailedException("Translation API error: " + ex.getMessage(), ex);
        } catch (TranslationRequestException | TranslationFailedException | TranslationServiceUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error invoking translation service: {}", ex.getMessage(), ex);
            throw new TranslationFailedException("Unexpected error during translation: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean isTranslationServiceAvailable() {
        try {
            HttpStatusCode statusCode = restClient.get()
                    .uri("/api/v1/translation/health")
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode();

            return statusCode.is2xxSuccessful();
        } catch (Exception ex) {
            log.warn("Translation service health check failed: {}", ex.getMessage());
            return false;
        }
    }
}
