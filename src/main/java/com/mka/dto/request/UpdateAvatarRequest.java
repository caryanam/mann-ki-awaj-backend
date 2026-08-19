package com.mka.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAvatarRequest {
    @NotBlank(message = "Avatar is required")
    @Size(max = 500, message = "Avatar path cannot exceed 500 characters")
    private String avatar;

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
}
