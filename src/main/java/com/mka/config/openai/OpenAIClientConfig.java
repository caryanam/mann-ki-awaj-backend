package com.mka.config.openai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Spring RestClient configuration for OpenAI API communication.
 */
@Configuration
@EnableConfigurationProperties(OpenAIProperties.class)
public class OpenAIClientConfig {

    private final OpenAIProperties openAIProperties;

    public OpenAIClientConfig(OpenAIProperties openAIProperties) {
        this.openAIProperties = openAIProperties;
    }

    @Bean
    public RestClient openAiRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) openAIProperties.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) openAIProperties.getReadTimeout().toMillis());

        return RestClient.builder()
                .baseUrl(openAIProperties.getApiUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
