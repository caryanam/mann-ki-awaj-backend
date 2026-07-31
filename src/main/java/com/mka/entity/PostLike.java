package com.mka.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "post_likes",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"post_id", "user_id"})
        }
)
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public PostLike() {}

    public PostLike(Long id, Post post, User user, LocalDateTime createdAt) {
        this.id = id;
        this.post = post;
        this.user = user;
        this.createdAt = createdAt;
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static PostLikeBuilder builder() { return new PostLikeBuilder(); }

    public static class PostLikeBuilder {
        private Long id;
        private Post post;
        private User user;
        private LocalDateTime createdAt;

        public PostLikeBuilder id(Long id) { this.id = id; return this; }
        public PostLikeBuilder post(Post post) { this.post = post; return this; }
        public PostLikeBuilder user(User user) { this.user = user; return this; }
        public PostLikeBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public PostLike build() {
            return new PostLike(id, post, user, createdAt);
        }
    }
}
