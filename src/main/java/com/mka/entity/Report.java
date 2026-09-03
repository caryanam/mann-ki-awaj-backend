package com.mka.entity;

import com.mka.enums.ReportReason;
import com.mka.enums.ReportStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(nullable = false, length = 20)
    private String contentType;

    @Column(nullable = false)
    private Long contentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportReason reason;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    public Report() {}

    public Report(Long id, User reporter, String contentType, Long contentId, ReportReason reason, String description, ReportStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.reporter = reporter;
        this.contentType = contentType;
        this.contentId = contentId;
        this.reason = reason;
        this.description = description;
        this.status = status != null ? status : ReportStatus.PENDING;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public String getFormattedReportId() {
        return id != null ? String.format("REPORT_%02d", id) : null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getReporter() { return reporter; }
    public void setReporter(User reporter) { this.reporter = reporter; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }

    public ReportReason getReason() { return reason; }
    public void setReason(ReportReason reason) { this.reason = reason; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static ReportBuilder builder() { return new ReportBuilder(); }

    public static class ReportBuilder {
        private Long id;
        private User reporter;
        private String contentType;
        private Long contentId;
        private ReportReason reason;
        private String description;
        private ReportStatus status = ReportStatus.PENDING;
        private Instant createdAt;
        private Instant updatedAt;

        public ReportBuilder id(Long id) { this.id = id; return this; }
        public ReportBuilder reporter(User reporter) { this.reporter = reporter; return this; }
        public ReportBuilder contentType(String contentType) { this.contentType = contentType; return this; }
        public ReportBuilder contentId(Long contentId) { this.contentId = contentId; return this; }
        public ReportBuilder reason(ReportReason reason) { this.reason = reason; return this; }
        public ReportBuilder description(String description) { this.description = description; return this; }
        public ReportBuilder status(ReportStatus status) { this.status = status; return this; }
        public ReportBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public ReportBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public Report build() {
            return new Report(id, reporter, contentType, contentId, reason, description, status, createdAt, updatedAt);
        }
    }
}
