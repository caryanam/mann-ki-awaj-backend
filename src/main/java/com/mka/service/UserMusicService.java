package com.mka.service;

import com.mka.dto.request.UserMusicTrackUpdateRequest;
import com.mka.dto.request.UserMusicTrackUploadRequest;
import com.mka.dto.response.UserMusicTrackResponse;
import com.mka.enums.MusicTrackStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface UserMusicService {
    UserMusicTrackResponse upload(String email, UserMusicTrackUploadRequest request,
                                  MultipartFile audio, MultipartFile cover);
    Page<UserMusicTrackResponse> list(String email, MusicTrackStatus status, Pageable pageable);
    UserMusicTrackResponse get(String email, Long id);
    UserMusicTrackResponse update(String email, Long id, UserMusicTrackUpdateRequest request);
    void delete(String email, Long id);
    MusicStorageService.StoredMusicResource getPrivateAudio(String email, Long id);
    MusicStorageService.StoredMusicResource getPrivateCover(String email, Long id);
}
