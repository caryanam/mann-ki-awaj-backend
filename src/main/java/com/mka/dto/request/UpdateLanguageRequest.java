package com.mka.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLanguageRequest {
    @NotBlank(message = "Preferred language is required")
    private String language;

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
