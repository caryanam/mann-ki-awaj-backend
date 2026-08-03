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
public class ChatRoomResponse {

    private Long id;
    private Long participant1Id;
    private Long participant2Id;
    private String participant1Username;
    private String participant2Username;
    private String participant1Avatar;
    private String participant2Avatar;
    private Long otherParticipantId;
    private String otherParticipantUsername;
    private String otherParticipantAvatar;
    private ChatMessageResponse lastMessage;
    private String requestStatus;
    private Long requestSenderId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
