package com.mka.entity;

import com.mka.enums.NotificationType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 255)
    private String message;

    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(length = 255)
    private String senderAvatar;

    private Long targetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(nullable = false)
    private Instant createdAt;

    public Notification() {}

    public Notification(Long id, User recipient, NotificationType type, String message, Boolean isRead, String senderAvatar, Long targetId, Instant createdAt) {
        this.id = id;
        this.recipient = recipient;
        this.type = type;
        this.message = message;
        this.isRead = isRead != null ? isRead : false;
        this.senderAvatar = senderAvatar;
        this.targetId = targetId;
        this.createdAt = createdAt;
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return recipient; }
    public void setUser(User user) { this.recipient = user; }

    public User getRecipient() { return recipient; }
    public void setRecipient(User recipient) { this.recipient = recipient; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public String getSenderAvatar() { return senderAvatar; }
    public void setSenderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static NotificationBuilder builder() { return new NotificationBuilder(); }

    public static class NotificationBuilder {
        private Long id;
        private User user;
        private User sender;
        private NotificationType type;
        private String message;
        private Boolean isRead = false;
        private String senderAvatar;
        private Long targetId;
        private Instant createdAt;

        public NotificationBuilder id(Long id) { this.id = id; return this; }
        public NotificationBuilder user(User user) { this.user = user; return this; }
        public NotificationBuilder recipient(User recipient) { this.user = recipient; return this; }
        public NotificationBuilder sender(User sender) { this.sender = sender; return this; }
        public NotificationBuilder type(NotificationType type) { this.type = type; return this; }
        public NotificationBuilder message(String message) { this.message = message; return this; }
        public NotificationBuilder isRead(Boolean isRead) { this.isRead = isRead; return this; }
        public NotificationBuilder senderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; return this; }
        public NotificationBuilder targetId(Long targetId) { this.targetId = targetId; return this; }
        public NotificationBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public Notification build() {
            Notification n = new Notification(id, user, type, message, isRead, senderAvatar, targetId, createdAt);
            n.setSender(sender);
            return n;
        }
    }
}
