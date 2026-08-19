package com.mka.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLanguageRequest {
    @NotBlank(message = "Preferred language is required")
    @Size(max = 10, message = "Language code cannot exceed 10 characters")
    private String language;

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
