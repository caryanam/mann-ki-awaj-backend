package com.mka.translation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object for requesting translation operations.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationRequest {

    @NotBlank(message = "Text cannot be blank")
    private String text;

    @NotBlank(message = "Source language cannot be blank")
    private String sourceLanguage;

    @NotBlank(message = "Target language cannot be blank")
    private String targetLanguage;
}
