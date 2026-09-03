package com.mka.entity;

import com.mka.enums.CommentStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_topic_id")
    private CustomTopic customTopic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    @Column(nullable = false, length = 100)
    private String authorAvatar;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String originalContent;

    @Column(nullable = false, length = 10)
    private String originalLanguage = "EN";

    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private Long likeCount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommentStatus status = CommentStatus.ACTIVE;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    public Comment() {}

    public Comment(Long id, Post post, User user, Comment parentComment, String authorAvatar, String originalContent, String originalLanguage, Long likeCount, CommentStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.post = post;
        this.user = user;
        this.parentComment = parentComment;
        this.authorAvatar = authorAvatar;
        this.originalContent = originalContent;
        this.originalLanguage = originalLanguage != null ? originalLanguage : "EN";
        this.likeCount = likeCount != null ? likeCount : 0L;
        this.status = status != null ? status : CommentStatus.ACTIVE;
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

    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }

    public CustomTopic getCustomTopic() { return customTopic; }
    public void setCustomTopic(CustomTopic customTopic) { this.customTopic = customTopic; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Comment getParentComment() { return parentComment; }
    public void setParentComment(Comment parentComment) { this.parentComment = parentComment; }

    public String getAuthorAvatar() { return authorAvatar; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }

    public String getOriginalContent() { return originalContent; }
    public void setOriginalContent(String originalContent) { this.originalContent = originalContent; }

    public String getOriginalLanguage() { return originalLanguage; }
    public void setOriginalLanguage(String originalLanguage) { this.originalLanguage = originalLanguage; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Long getLikeCount() { return likeCount; }
    public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }

    public CommentStatus getStatus() { return status; }
    public void setStatus(CommentStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static CommentBuilder builder() { return new CommentBuilder(); }

    public static class CommentBuilder {
        private Long id;
        private Post post;
        private CustomTopic customTopic;
        private User user;
        private Comment parentComment;
        private String authorAvatar;
        private String originalContent;
        private String originalLanguage = "EN";
        private String imageUrl;
        private Long likeCount = 0L;
        private CommentStatus status = CommentStatus.ACTIVE;
        private Instant createdAt;
        private Instant updatedAt;

        public CommentBuilder id(Long id) { this.id = id; return this; }
        public CommentBuilder post(Post post) { this.post = post; return this; }
        public CommentBuilder customTopic(CustomTopic customTopic) { this.customTopic = customTopic; return this; }
        public CommentBuilder user(User user) { this.user = user; return this; }
        public CommentBuilder parentComment(Comment parentComment) { this.parentComment = parentComment; return this; }
        public CommentBuilder authorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; return this; }
        public CommentBuilder originalContent(String originalContent) { this.originalContent = originalContent; return this; }
        public CommentBuilder originalLanguage(String originalLanguage) { this.originalLanguage = originalLanguage; return this; }
        public CommentBuilder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public CommentBuilder likeCount(Long likeCount) { this.likeCount = likeCount; return this; }
        public CommentBuilder status(CommentStatus status) { this.status = status; return this; }
        public CommentBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public CommentBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public Comment build() {
            Comment comment = new Comment(id, post, user, parentComment, authorAvatar, originalContent, originalLanguage, likeCount, status, createdAt, updatedAt);
            comment.setCustomTopic(customTopic);
            comment.setImageUrl(imageUrl);
            return comment;
        }
    }
}
