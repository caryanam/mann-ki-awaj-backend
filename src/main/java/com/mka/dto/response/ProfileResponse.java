package com.mka.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    private Long id;
    private Long userId;
    private String username;
    private String avatar;
    private String preferredLanguage;
    private String bio;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
