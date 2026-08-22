package com.mka.service;

import com.mka.dto.request.MusicTrackUploadRequest;
import com.mka.dto.response.AdminMusicTrackResponse;
import com.mka.dto.request.UserMusicTrackUploadRequest;
import com.mka.entity.MusicTrack;
import com.mka.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface MusicUploadService {
    AdminMusicTrackResponse upload(String authenticatedAdminEmail,
                                   MusicTrackUploadRequest request,
                                   MultipartFile audio,
                                   MultipartFile cover);
    MusicTrack uploadCommunity(User uploader,
                               UserMusicTrackUploadRequest request,
                               MultipartFile audio,
                               MultipartFile cover);
}
