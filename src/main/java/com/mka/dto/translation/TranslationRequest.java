package com.mka.dto.translation;

import com.mka.enums.translation.EntityType;
import com.mka.enums.translation.SupportedLanguage;
import com.mka.enums.translation.TranslationProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object for translation request operations.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationRequest {

    @NotBlank(message = "Text to translate cannot be empty")
    @Size(max = 5000, message = "Text to translate cannot exceed 5000 characters")
    private String text;

    private SupportedLanguage sourceLanguage;

    @NotNull(message = "Target language is required")
    private SupportedLanguage targetLanguage;

    private EntityType entityType;

    private Long entityId;

    private TranslationProvider provider;

    @Builder.Default
    private boolean forceRefresh = false;
}
