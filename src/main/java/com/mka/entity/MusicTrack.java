package com.mka.entity;

import com.mka.enums.LanguageCode;
import com.mka.enums.MusicMood;
import com.mka.enums.MusicTrackStatus;
import com.mka.enums.MusicTrackSource;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "music_tracks",
        uniqueConstraints = @UniqueConstraint(name = "uk_music_tracks_audio_storage_key",
                columnNames = "audio_storage_key"),
        indexes = {
                @Index(name = "idx_music_tracks_status", columnList = "status"),
                @Index(name = "idx_music_tracks_status_created", columnList = "status,created_at"),
                @Index(name = "idx_music_tracks_status_language", columnList = "status,language"),
                @Index(name = "idx_music_tracks_status_mood", columnList = "status,mood"),
                @Index(name = "idx_music_tracks_status_featured", columnList = "status,is_featured")
                ,@Index(name = "idx_music_tracks_status_source", columnList = "status,source")
                ,@Index(name = "idx_music_tracks_uploader_status", columnList = "uploaded_by,status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MusicTrack extends BaseEntity {

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String title;

    @NotBlank
    @Size(max = 150)
    @Column(name = "artist_name", nullable = false, length = 150)
    private String artistName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LanguageCode language;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MusicMood mood;

    @Size(max = 80)
    @Column(length = 80)
    private String genre;

    @Size(max = 1000)
    @Column(length = 1000)
    private String description;

    @NotBlank
    @Size(max = 255)
    @Column(name = "audio_storage_key", nullable = false, length = 255)
    private String audioStorageKey;

    @Size(max = 255)
    @Column(name = "cover_storage_key", length = 255)
    private String coverStorageKey;

    @Positive
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @NotBlank
    @Size(max = 100)
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @NotNull
    @Positive
    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MusicTrackStatus status = MusicTrackStatus.DRAFT;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'PLATFORM'")
    @Builder.Default
    private MusicTrackSource source = MusicTrackSource.PLATFORM;

    @NotNull
    @Column(name = "original_work_confirmed", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean originalWorkConfirmed = false;

    @NotNull
    @Column(name = "rights_confirmed", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean rightsConfirmed = false;

    @NotNull
    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean featured = false;

    @NotNull
    @PositiveOrZero
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Size(max = 500)
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @PrePersist
    @PreUpdate
    void normalizeAndValidate() {
        title = requireTrimmed(title, "title");
        artistName = requireTrimmed(artistName, "artistName");
        audioStorageKey = requireStorageKey(audioStorageKey);
        mimeType = requireTrimmed(mimeType, "mimeType");
        genre = trimToNull(genre);
        description = trimToNull(description);
        coverStorageKey = coverStorageKey == null ? null : requireStorageKey(coverStorageKey);

        rejectionReason = trimToNull(rejectionReason);
        if (language == null || mood == null || status == null || source == null || uploadedBy == null) {
            throw new IllegalStateException("language, mood, status, source and uploadedBy are required");
        }
        if (originalWorkConfirmed == null || rightsConfirmed == null) {
            throw new IllegalStateException("rights declarations must not be null");
        }
        if (durationSeconds != null && durationSeconds <= 0) {
            throw new IllegalStateException("durationSeconds must be positive");
        }
        if (fileSizeBytes == null || fileSizeBytes <= 0) {
            throw new IllegalStateException("fileSizeBytes must be positive");
        }
        if (sortOrder == null || sortOrder < 0) {
            throw new IllegalStateException("sortOrder must be non-negative");
        }
    }

    private static String requireTrimmed(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalStateException(field + " is required");
        }
        return trimmed;
    }

    private static String requireStorageKey(String value) {
        String key = requireTrimmed(value, "storage key");
        if (key.contains("/") || key.contains("\\") || key.equals(".") || key.equals("..")) {
            throw new IllegalStateException("storage key must be a filename only");
        }
        return key;
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
