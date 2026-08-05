package com.mka.translation.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Spring RestClient configuration for the IndicTrans2 Translation Microservice.
 * Configures connection and read timeouts dynamically from TranslationProperties.
 */
@Configuration
@EnableConfigurationProperties(TranslationProperties.class)
public class TranslationClientConfig {

    private final TranslationProperties properties;

    public TranslationClientConfig(@Qualifier("indicTrans2TranslationProperties") TranslationProperties properties) {
        this.properties = properties;
    }

    @Bean
    public RestClient translationRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) properties.getReadTimeout().toMillis());

        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
