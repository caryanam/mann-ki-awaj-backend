package com.mka.entity;

import com.mka.enums.ReactionType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "post_reactions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"post_id", "user_id"})
        }
)
public class PostReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReactionType reactionType;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public PostReaction() {}

    public PostReaction(Long id, Post post, User user, ReactionType reactionType, LocalDateTime createdAt) {
        this.id = id;
        this.post = post;
        this.user = user;
        this.reactionType = reactionType;
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

    public ReactionType getReactionType() { return reactionType; }
    public void setReactionType(ReactionType reactionType) { this.reactionType = reactionType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static PostReactionBuilder builder() { return new PostReactionBuilder(); }

    public static class PostReactionBuilder {
        private Long id;
        private Post post;
        private User user;
        private ReactionType reactionType;
        private LocalDateTime createdAt;

        public PostReactionBuilder id(Long id) { this.id = id; return this; }
        public PostReactionBuilder post(Post post) { this.post = post; return this; }
        public PostReactionBuilder user(User user) { this.user = user; return this; }
        public PostReactionBuilder reactionType(ReactionType reactionType) { this.reactionType = reactionType; return this; }
        public PostReactionBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public PostReaction build() {
            return new PostReaction(id, post, user, reactionType, createdAt);
        }
    }
}
