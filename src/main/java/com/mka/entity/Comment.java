package com.mka.entity;

import com.mka.enums.CommentStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

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

    @Column(nullable = false)
    private Long likeCount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommentStatus status = CommentStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Comment() {}

    public Comment(Long id, Post post, User user, Comment parentComment, String authorAvatar, String originalContent, String originalLanguage, Long likeCount, CommentStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
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
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }

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

    public Long getLikeCount() { return likeCount; }
    public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }

    public CommentStatus getStatus() { return status; }
    public void setStatus(CommentStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static CommentBuilder builder() { return new CommentBuilder(); }

    public static class CommentBuilder {
        private Long id;
        private Post post;
        private User user;
        private Comment parentComment;
        private String authorAvatar;
        private String originalContent;
        private String originalLanguage = "EN";
        private Long likeCount = 0L;
        private CommentStatus status = CommentStatus.ACTIVE;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public CommentBuilder id(Long id) { this.id = id; return this; }
        public CommentBuilder post(Post post) { this.post = post; return this; }
        public CommentBuilder user(User user) { this.user = user; return this; }
        public CommentBuilder parentComment(Comment parentComment) { this.parentComment = parentComment; return this; }
        public CommentBuilder authorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; return this; }
        public CommentBuilder originalContent(String originalContent) { this.originalContent = originalContent; return this; }
        public CommentBuilder originalLanguage(String originalLanguage) { this.originalLanguage = originalLanguage; return this; }
        public CommentBuilder likeCount(Long likeCount) { this.likeCount = likeCount; return this; }
        public CommentBuilder status(CommentStatus status) { this.status = status; return this; }
        public CommentBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public CommentBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Comment build() {
            return new Comment(id, post, user, parentComment, authorAvatar, originalContent, originalLanguage, likeCount, status, createdAt, updatedAt);
        }
    }
}
