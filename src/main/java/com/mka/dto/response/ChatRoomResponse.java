package com.mka.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

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
    private Instant otherParticipantLastSeen;
    private Boolean otherParticipantIsOnline;
    private ChatMessageResponse lastMessage;
    private Long unreadCount;
    private Boolean hasUnread;
    private String requestStatus;
    private Long requestSenderId;
    private Instant createdAt;
    private Instant updatedAt;
}
