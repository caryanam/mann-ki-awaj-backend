package com.mka.dto.response;

import com.mka.enums.LanguageCode;
import com.mka.enums.MusicMood;
import com.mka.enums.MusicTrackStatus;
import com.mka.enums.MusicTrackSource;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminMusicTrackResponse {
    private Long id;
    private String title;
    private String artist;
    private LanguageCode language;
    private Set<MusicMood> moods;
    private String genre;
    private String description;
    private String coverUrl;
    private String audioUrl;
    private Integer durationSeconds;
    private String mimeType;
    private Long fileSizeBytes;
    private MusicTrackStatus status;
    private MusicTrackSource source;
    private Boolean featured;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    private LocalDateTime reviewedAt;
    private String rejectionReason;
    private AdminMusicUploaderResponse uploader;
}
