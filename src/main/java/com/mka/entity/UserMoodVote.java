package com.mka.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_mood_votes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMoodVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 50)
    private String mood;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void onSaveOrUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
