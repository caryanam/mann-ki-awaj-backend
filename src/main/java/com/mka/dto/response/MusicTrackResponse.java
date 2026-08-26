package com.mka.dto.response;

import com.mka.enums.LanguageCode;
import com.mka.enums.MusicMood;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MusicTrackResponse {
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
    private Boolean featured;
    private LocalDateTime publishedAt;
}
