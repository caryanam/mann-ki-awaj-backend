package com.mka.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

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

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public ChatRoom() {}

    public ChatRoom(Long id, User participant1, User participant2, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.participant1 = participant1;
        this.participant2 = participant2;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static ChatRoomBuilder builder() { return new ChatRoomBuilder(); }

    public static class ChatRoomBuilder {
        private Long id;
        private User participant1;
        private User participant2;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public ChatRoomBuilder id(Long id) { this.id = id; return this; }
        public ChatRoomBuilder participant1(User participant1) { this.participant1 = participant1; return this; }
        public ChatRoomBuilder user1(User user1) { this.participant1 = user1; return this; }
        public ChatRoomBuilder participant2(User participant2) { this.participant2 = participant2; return this; }
        public ChatRoomBuilder user2(User user2) { this.participant2 = user2; return this; }
        public ChatRoomBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ChatRoomBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ChatRoom build() {
            return new ChatRoom(id, participant1, participant2, createdAt, updatedAt);
        }
    }
}
