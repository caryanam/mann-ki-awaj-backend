package com.mka.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "chat_rooms",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"participant1_id", "participant2_id"})
        }
)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant1_id", nullable = false)
    private User participant1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant2_id", nullable = false)
    private User participant2;

    @Column(name = "request_status", nullable = false, length = 20)
    private String requestStatus = "PENDING";

    @Column(name = "request_sender_id")
    private Long requestSenderId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public ChatRoom() {}

    public ChatRoom(Long id, User participant1, User participant2, String requestStatus, Long requestSenderId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.participant1 = participant1;
        this.participant2 = participant2;
        this.requestStatus = requestStatus != null ? requestStatus : "PENDING";
        this.requestSenderId = requestSenderId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getParticipant1() { return participant1; }
    public void setParticipant1(User participant1) { this.participant1 = participant1; }

    public User getUser1() { return participant1; }
    public void setUser1(User user1) { this.participant1 = user1; }

    public User getParticipant2() { return participant2; }
    public void setParticipant2(User participant2) { this.participant2 = participant2; }

    public User getUser2() { return participant2; }
    public void setUser2(User user2) { this.participant2 = user2; }

    public String getRequestStatus() { return requestStatus; }
    public void setRequestStatus(String requestStatus) { this.requestStatus = requestStatus; }

    public Long getRequestSenderId() { return requestSenderId; }
    public void setRequestSenderId(Long requestSenderId) { this.requestSenderId = requestSenderId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static ChatRoomBuilder builder() { return new ChatRoomBuilder(); }

    public static class ChatRoomBuilder {
        private Long id;
        private User participant1;
        private User participant2;
        private String requestStatus = "PENDING";
        private Long requestSenderId;
        private Instant createdAt;
        private Instant updatedAt;

        public ChatRoomBuilder id(Long id) { this.id = id; return this; }
        public ChatRoomBuilder participant1(User participant1) { this.participant1 = participant1; return this; }
        public ChatRoomBuilder user1(User user1) { this.participant1 = user1; return this; }
        public ChatRoomBuilder participant2(User participant2) { this.participant2 = participant2; return this; }
        public ChatRoomBuilder user2(User user2) { this.participant2 = user2; return this; }
        public ChatRoomBuilder requestStatus(String requestStatus) { this.requestStatus = requestStatus; return this; }
        public ChatRoomBuilder requestSenderId(Long requestSenderId) { this.requestSenderId = requestSenderId; return this; }
        public ChatRoomBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public ChatRoomBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public ChatRoom build() {
            return new ChatRoom(id, participant1, participant2, requestStatus, requestSenderId, createdAt, updatedAt);
        }
    }
}
