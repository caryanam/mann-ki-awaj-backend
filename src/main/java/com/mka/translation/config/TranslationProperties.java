package com.mka.translation.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Configuration properties for the external Python IndicTrans2 translation microservice.
 * Reads properties under the `translation` prefix in application.properties or application.yml.
 */
@Getter
@Setter
@Component("indicTrans2TranslationProperties")
@ConfigurationProperties(prefix = "translation")
public class TranslationProperties {

    /**
     * Base URL of the Python FastAPI Translation Microservice.
     */
    private String baseUrl = "http://localhost:8001";

    /**
     * HTTP connection establishment timeout.
     */
    private Duration connectTimeout = Duration.ofSeconds(2);

    /**
     * HTTP socket read timeout for translation inference responses.
     */
    private Duration readTimeout = Duration.ofSeconds(10);
}
