package com.mka.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProfileRequest {

    @NotBlank(message = "Username handle is required")
    @Size(min = 3, max = 30, message = "Username handle must be between 3 and 30 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9._]+$",
            message = "Username handle can contain only letters, numbers, dot(.) and underscore(_)"
    )
    private String username;

    @Size(max = 250, message = "Bio cannot exceed 250 characters")
    private String bio;

    private String avatar;

    @Builder.Default
    private String preferredLanguage = "EN";
}
