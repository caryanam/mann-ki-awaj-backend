package com.mka.config.translation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized configuration properties for the translation module.
 */
@Component
@ConfigurationProperties(prefix = "translation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TranslationProperties {

    private boolean enabled = true;
    private String provider = "INDICTRANS2";
    private String baseUrl = "http://localhost:5000";
    private int timeout = 5000;
    private int retryCount = 3;
    private int maxTextLength = 2000;
}
