package com.mka.entity;

import com.mka.enums.ReactionType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "comment_reactions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"comment_id", "user_id"})
        }
)
public class CommentReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReactionType reactionType;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public CommentReaction() {}

    public CommentReaction(Long id, Comment comment, User user, ReactionType reactionType, LocalDateTime createdAt) {
        this.id = id;
        this.comment = comment;
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

    public Comment getComment() { return comment; }
    public void setComment(Comment comment) { this.comment = comment; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public ReactionType getReactionType() { return reactionType; }
    public void setReactionType(ReactionType reactionType) { this.reactionType = reactionType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static CommentReactionBuilder builder() { return new CommentReactionBuilder(); }

    public static class CommentReactionBuilder {
        private Long id;
        private Comment comment;
        private User user;
        private ReactionType reactionType;
        private LocalDateTime createdAt;

        public CommentReactionBuilder id(Long id) { this.id = id; return this; }
        public CommentReactionBuilder comment(Comment comment) { this.comment = comment; return this; }
        public CommentReactionBuilder user(User user) { this.user = user; return this; }
        public CommentReactionBuilder reactionType(ReactionType reactionType) { this.reactionType = reactionType; return this; }
        public CommentReactionBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public CommentReaction build() {
            return new CommentReaction(id, comment, user, reactionType, createdAt);
        }
    }
}
