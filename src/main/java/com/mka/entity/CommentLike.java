package com.mka.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "comment_likes",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"comment_id", "user_id"})
        }
)
public class CommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public CommentLike() {}

    public CommentLike(Long id, Comment comment, User user, LocalDateTime createdAt) {
        this.id = id;
        this.comment = comment;
        this.user = user;
        this.createdAt = createdAt;
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Comment getComment() { return comment; }
    public void setComment(Comment comment) { this.comment = comment; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static CommentLikeBuilder builder() { return new CommentLikeBuilder(); }

    public static class CommentLikeBuilder {
        private Long id;
        private Comment comment;
        private User user;
        private LocalDateTime createdAt;

        public CommentLikeBuilder id(Long id) { this.id = id; return this; }
        public CommentLikeBuilder comment(Comment comment) { this.comment = comment; return this; }
        public CommentLikeBuilder user(User user) { this.user = user; return this; }
        public CommentLikeBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public CommentLike build() {
            return new CommentLike(id, comment, user, createdAt);
        }
    }
}
