package com.mka.entity;

import com.mka.enums.ReviewStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "content_review_queue")
public class ContentReviewQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String contentType;

    @Column(nullable = false)
    private Long contentId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contentSnippet;

    @Column(nullable = false, length = 100)
    private String flaggedReason;

    private Double aiConfidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status = ReviewStatus.PENDING_REVIEW;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    public ContentReviewQueue() {}

    public ContentReviewQueue(Long id, String contentType, Long contentId, String contentSnippet, String flaggedReason, Double aiConfidenceScore, ReviewStatus status, LocalDateTime createdAt, LocalDateTime reviewedAt) {
        this.id = id;
        this.contentType = contentType;
        this.contentId = contentId;
        this.contentSnippet = contentSnippet;
        this.flaggedReason = flaggedReason;
        this.aiConfidenceScore = aiConfidenceScore;
        this.status = status != null ? status : ReviewStatus.PENDING_REVIEW;
        this.createdAt = createdAt;
        this.reviewedAt = reviewedAt;
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }

    public String getContentSnippet() { return contentSnippet; }
    public void setContentSnippet(String contentSnippet) { this.contentSnippet = contentSnippet; }

    public String getFlaggedReason() { return flaggedReason; }
    public void setFlaggedReason(String flaggedReason) { this.flaggedReason = flaggedReason; }

    public Double getAiConfidenceScore() { return aiConfidenceScore; }
    public void setAiConfidenceScore(Double aiConfidenceScore) { this.aiConfidenceScore = aiConfidenceScore; }

    public ReviewStatus getStatus() { return status; }
    public void setStatus(ReviewStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public static ContentReviewQueueBuilder builder() { return new ContentReviewQueueBuilder(); }

    public static class ContentReviewQueueBuilder {
        private Long id;
        private String contentType;
        private Long contentId;
        private String contentSnippet;
        private String flaggedReason;
        private Double aiConfidenceScore;
        private ReviewStatus status = ReviewStatus.PENDING_REVIEW;
        private LocalDateTime createdAt;
        private LocalDateTime reviewedAt;

        public ContentReviewQueueBuilder id(Long id) { this.id = id; return this; }
        public ContentReviewQueueBuilder contentType(String contentType) { this.contentType = contentType; return this; }
        public ContentReviewQueueBuilder contentId(Long contentId) { this.contentId = contentId; return this; }
        public ContentReviewQueueBuilder contentSnippet(String contentSnippet) { this.contentSnippet = contentSnippet; return this; }
        public ContentReviewQueueBuilder flaggedReason(String flaggedReason) { this.flaggedReason = flaggedReason; return this; }
        public ContentReviewQueueBuilder aiConfidenceScore(Double aiConfidenceScore) { this.aiConfidenceScore = aiConfidenceScore; return this; }
        public ContentReviewQueueBuilder status(ReviewStatus status) { this.status = status; return this; }
        public ContentReviewQueueBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ContentReviewQueueBuilder reviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; return this; }

        public ContentReviewQueue build() {
            return new ContentReviewQueue(id, contentType, contentId, contentSnippet, flaggedReason, aiConfidenceScore, status, createdAt, reviewedAt);
        }
    }
}
