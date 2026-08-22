package com.mka.dto.response;

import com.mka.enums.LanguageCode;
import com.mka.enums.MusicMood;
import com.mka.enums.MusicTrackStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMusicTrackResponse {
    private Long id;
    private String title;
    private String artist;
    private LanguageCode language;
    private MusicMood mood;
    private String genre;
    private String description;
    private MusicTrackStatus status;
    private String privateAudioUrl;
    private String privateCoverUrl;
    private String publicAudioUrl;
    private String publicCoverUrl;
    private Integer durationSeconds;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}
