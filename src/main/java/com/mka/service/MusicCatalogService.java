package com.mka.service;

import com.mka.dto.response.MusicTrackResponse;
import com.mka.enums.LanguageCode;
import com.mka.enums.MusicMood;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MusicCatalogService {
    Page<MusicTrackResponse> getPublishedTracks(String query, LanguageCode language, MusicMood mood,
                                                String genre, Boolean featured, Pageable pageable);

    MusicTrackResponse getPublishedTrack(Long id);
}
