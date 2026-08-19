package com.mka.dto.request;

import com.mka.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMessageRequest {

    private Long roomId;
    private Long recipientUserId;

    @NotBlank(message = "Message content cannot be empty")
    @Size(max = 2000, message = "Message content cannot exceed 2000 characters")
    private String content;

    private MessageType messageType;

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public Long getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(Long recipientUserId) { this.recipientUserId = recipientUserId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }
}
