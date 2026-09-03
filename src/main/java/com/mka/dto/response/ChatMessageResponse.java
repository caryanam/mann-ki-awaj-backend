package com.mka.dto.response;

import com.mka.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    private Long id;
    private Long roomId;
    private Long senderId;
    private String senderUsername;
    private String senderAvatar;
    private String content;
    private MessageType messageType;
    private Boolean isRead;
    private Instant createdAt;
}
