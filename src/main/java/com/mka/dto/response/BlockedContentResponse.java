package com.mka.dto.response;

import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedContentResponse {
    private Long id;
    private Long userId;
    private String contentType;
    private String authorUsername;
    private String authorEmail;
    private String originalContent;
    private String flaggedReason;
    private String status;
    private Instant blockedAt;
}
