package com.mka.service.impl;

import com.mka.config.MusicUploadProperties;
import com.mka.dto.request.UserMusicTrackUpdateRequest;
import com.mka.dto.request.UserMusicTrackUploadRequest;
import com.mka.dto.response.UserMusicTrackResponse;
import com.mka.entity.MusicTrack;
import com.mka.entity.User;
import com.mka.enums.MusicTrackSource;
import com.mka.enums.MusicTrackStatus;
import com.mka.exception.BadRequestException;
import com.mka.exception.MusicConflictException;
import com.mka.exception.MusicOperationException;
import com.mka.exception.ResourceNotFoundException;
import com.mka.repository.MusicTrackRepository;
import com.mka.repository.UserRepository;
import com.mka.service.AdminMusicManagementService;
import com.mka.service.MusicStorageService;
import com.mka.service.MusicUploadService;
import com.mka.service.UserMusicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class
UserMusicServiceImpl implements UserMusicService {
    private final MusicTrackRepository tracks;
    private final UserRepository users;
    private final MusicUploadService uploads;
    private final MusicStorageService storage;
    private final AdminMusicManagementService lifecycle;
    private final MusicUploadProperties properties;

    @Override
    @Transactional
    public UserMusicTrackResponse upload(String email, UserMusicTrackUploadRequest request,
                                         MultipartFile audio, MultipartFile cover) {
        if (!Boolean.TRUE.equals(request.getOriginalWorkConfirmed())
                || !Boolean.TRUE.equals(request.getRightsConfirmed())) {
            throw new BadRequestException("MUSIC_RIGHTS_CONFIRMATION_REQUIRED");
        }
        User owner = users.findByEmailForUpdate(email)
                .filter(user -> Boolean.TRUE.equals(user.getActive()) && !Boolean.TRUE.equals(user.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("AUTHENTICATED_USER_NOT_FOUND"));
        long pending = tracks.countByUploadedByIdAndSourceAndStatus(
                owner.getId(), MusicTrackSource.COMMUNITY, MusicTrackStatus.PENDING_REVIEW);
        if (pending >= properties.getUserMaxPending()) {
            throw new MusicConflictException("MUSIC_UPLOAD_LIMIT_REACHED");
        }
        return toResponse(uploads.uploadCommunity(owner, request, audio, cover));
    }

    @Override
    public Page<UserMusicTrackResponse> list(String email, MusicTrackStatus status, Pageable pageable) {
        User owner = owner(email);
        Page<MusicTrack> page = status == null
                ? tracks.findByUploadedByIdAndSourceAndStatusNot(owner.getId(), MusicTrackSource.COMMUNITY,
                    MusicTrackStatus.DELETED, pageable)
                : status == MusicTrackStatus.DELETED
                    ? Page.empty(pageable)
                    : tracks.findByUploadedByIdAndSourceAndStatus(owner.getId(), MusicTrackSource.COMMUNITY,
                        status, pageable);
        return page.map(this::toResponse);
    }

    @Override
    public UserMusicTrackResponse get(String email, Long id) {
        return toResponse(ownedActive(email, id, false));
    }

    @Override
    @Transactional
    public UserMusicTrackResponse update(String email, Long id, UserMusicTrackUpdateRequest request) {
        MusicTrack track = ownedActive(email, id, true);
        if (track.getStatus() != MusicTrackStatus.PENDING_REVIEW) {
            throw new MusicConflictException("MUSIC_TRACK_NOT_EDITABLE");
        }
        track.setTitle(request.getTitle());
        track.setArtistName(request.getArtistName());
        track.setLanguage(request.getLanguage());
        track.setMoods(request.getMoods());
        track.setGenre(request.getGenre());
        track.setDescription(request.getDescription());
        try {
            return toResponse(tracks.saveAndFlush(track));
        } catch (RuntimeException ex) {
            throw new MusicOperationException("MUSIC_UPDATE_FAILED", ex);
        }
    }

    @Override
    @Transactional
    public void delete(String email, Long id) {
        MusicTrack track = ownedActive(email, id, true);
        if (track.getStatus() != MusicTrackStatus.PENDING_REVIEW
                && track.getStatus() != MusicTrackStatus.REJECTED) {
            throw new MusicConflictException("MUSIC_TRACK_NOT_DELETABLE");
        }
        lifecycle.delete(id);
    }

    @Override
    public MusicStorageService.StoredMusicResource getPrivateAudio(String email, Long id) {
        return storage.getPrivateAudio(ownedActive(email, id, false).getAudioStorageKey());
    }

    @Override
    public MusicStorageService.StoredMusicResource getPrivateCover(String email, Long id) {
        MusicTrack track = ownedActive(email, id, false);
        if (track.getCoverStorageKey() == null) throw new ResourceNotFoundException("MUSIC_COVER_NOT_FOUND");
        return storage.getPrivateCover(track.getCoverStorageKey());
    }

    private User owner(String email) {
        return users.findByEmail(email)
                .filter(user -> Boolean.TRUE.equals(user.getActive()) && !Boolean.TRUE.equals(user.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("AUTHENTICATED_USER_NOT_FOUND"));
    }

    private MusicTrack ownedActive(String email, Long id, boolean lock) {
        User owner = owner(email);
        MusicTrack track = (lock ? tracks.findActiveByIdForUpdate(id)
                : tracks.findByIdAndStatusNot(id, MusicTrackStatus.DELETED))
                .orElseThrow(() -> new ResourceNotFoundException("MUSIC_TRACK_NOT_FOUND"));
        if (track.getSource() != MusicTrackSource.COMMUNITY
                || track.getUploadedBy() == null
                || !owner.getId().equals(track.getUploadedBy().getId())) {
            throw new ResourceNotFoundException("MUSIC_TRACK_NOT_FOUND");
        }
        return track;
    }

    private UserMusicTrackResponse toResponse(MusicTrack track) {
        boolean published = track.getStatus() == MusicTrackStatus.PUBLISHED;
        return UserMusicTrackResponse.builder()
                .id(track.getId()).title(track.getTitle()).artist(track.getArtistName())
                .language(track.getLanguage()).moods(track.getMoods()).genre(track.getGenre())
                .description(track.getDescription()).status(track.getStatus())
                .privateAudioUrl(com.mka.util.MediaUrlUtils.toAbsoluteUrl("/api/music/my-tracks/" + track.getId() + "/audio"))
                .privateCoverUrl(track.getCoverStorageKey() == null ? null
                        : com.mka.util.MediaUrlUtils.toAbsoluteUrl("/api/music/my-tracks/" + track.getId() + "/cover"))
                .publicAudioUrl(published ? com.mka.util.MediaUrlUtils.toAbsoluteUrl("/media/music/audio/" + track.getAudioStorageKey()) : null)
                .publicCoverUrl(published && track.getCoverStorageKey() != null
                        ? com.mka.util.MediaUrlUtils.toAbsoluteUrl("/media/music/covers/" + track.getCoverStorageKey()) : null)
                .durationSeconds(track.getDurationSeconds()).rejectionReason(track.getRejectionReason())
                .createdAt(track.getCreatedAt()).updatedAt(track.getUpdatedAt())
                .publishedAt(track.getPublishedAt()).build();
    }
}
