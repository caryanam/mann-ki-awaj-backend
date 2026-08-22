package com.mka.service;

import com.mka.dto.request.MusicTrackUpdateRequest;
import com.mka.dto.response.AdminMusicTrackResponse;
import com.mka.enums.LanguageCode;
import com.mka.enums.MusicMood;
import com.mka.enums.MusicTrackStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminMusicManagementService {
    Page<AdminMusicTrackResponse> list(String query, MusicTrackStatus status, LanguageCode language,
                                       MusicMood mood, String genre, Boolean featured, Pageable pageable);
    AdminMusicTrackResponse get(Long id);
    AdminMusicTrackResponse update(Long id, MusicTrackUpdateRequest request);
    AdminMusicTrackResponse publish(Long id);
    AdminMusicTrackResponse unpublish(Long id);
    AdminMusicTrackResponse approve(Long id);
    AdminMusicTrackResponse reject(Long id, String reason);
    void delete(Long id);
    MusicStorageService.StoredMusicResource getPrivateAudio(Long id);
    MusicStorageService.StoredMusicResource getPrivateCover(Long id);
}
