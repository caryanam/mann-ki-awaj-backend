package com.mka.entity;

import com.mka.enums.MessageType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(length = 255)
    private String senderAvatar;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType messageType = MessageType.TEXT;

    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(nullable = false)
    private Instant createdAt;

    public ChatMessage() {}

    public ChatMessage(Long id, ChatRoom room, User sender, String senderAvatar, String content, MessageType messageType, Boolean isRead, Instant createdAt) {
        this.id = id;
        this.room = room;
        this.sender = sender;
        this.senderAvatar = senderAvatar;
        this.content = content;
        this.messageType = messageType != null ? messageType : MessageType.TEXT;
        this.isRead = isRead != null ? isRead : false;
        this.createdAt = createdAt;
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ChatRoom getRoom() { return room; }
    public void setRoom(ChatRoom room) { this.room = room; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public String getSenderAvatar() { return senderAvatar; }
    public void setSenderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static ChatMessageBuilder builder() { return new ChatMessageBuilder(); }

    public static class ChatMessageBuilder {
        private Long id;
        private ChatRoom room;
        private User sender;
        private String senderAvatar;
        private String content;
        private MessageType messageType = MessageType.TEXT;
        private Boolean isRead = false;
        private Instant createdAt;

        public ChatMessageBuilder id(Long id) { this.id = id; return this; }
        public ChatMessageBuilder room(ChatRoom room) { this.room = room; return this; }
        public ChatMessageBuilder sender(User sender) { this.sender = sender; return this; }
        public ChatMessageBuilder senderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; return this; }
        public ChatMessageBuilder content(String content) { this.content = content; return this; }
        public ChatMessageBuilder messageType(MessageType messageType) { this.messageType = messageType; return this; }
        public ChatMessageBuilder isRead(Boolean isRead) { this.isRead = isRead; return this; }
        public ChatMessageBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public ChatMessage build() {
            return new ChatMessage(id, room, sender, senderAvatar, content, messageType, isRead, createdAt);
        }
    }
}
