package com.mka.dto.response;

import com.mka.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private Long userId;
    private NotificationType type;
    private String message;
    private Boolean isRead;
    private String senderUsername;
    private String senderAvatar;
    private Long targetId;
    private LocalDateTime createdAt;
}
