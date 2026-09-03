package com.mka.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "blocked_content")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "content_type", nullable = false)
    private String contentType; // POST, COMMENT, MESSAGE, POST_IMAGE

    @Column(name = "author_username")
    private String authorUsername;

    @Column(name = "author_email")
    private String authorEmail;

    @Column(name = "original_content", columnDefinition = "LONGTEXT", nullable = false)
    private String originalContent;

    @Column(name = "flagged_reason")
    private String flaggedReason;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "PENDING"; // PENDING, WARNING_ISSUED

    @Column(name = "blocked_at", nullable = false)
    private Instant blockedAt;

    @PrePersist
    public void onCreate() {
        this.blockedAt = Instant.now();
    }
}
