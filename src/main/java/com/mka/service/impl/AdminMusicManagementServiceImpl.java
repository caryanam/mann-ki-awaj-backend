package com.mka.service.impl;

import com.mka.dto.request.MusicTrackUpdateRequest;
import com.mka.dto.request.MusicTrackApprovalRequest;
import com.mka.dto.response.AdminMusicTrackResponse;
import com.mka.dto.response.AdminMusicUploaderResponse;
import com.mka.entity.MusicTrack;
import com.mka.enums.LanguageCode;
import com.mka.enums.MusicMood;
import com.mka.enums.MusicTrackStatus;
import com.mka.enums.MusicTrackSource;
import com.mka.exception.BadRequestException;
import com.mka.exception.MusicOperationException;
import com.mka.exception.MusicConflictException;
import com.mka.exception.ResourceNotFoundException;
import com.mka.repository.MusicTrackRepository;
import com.mka.service.AdminMusicManagementService;
import com.mka.service.MusicStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMusicManagementServiceImpl implements AdminMusicManagementService {

    private final MusicTrackRepository repository;
    private final MusicStorageService storage;

    @Override
    public Page<AdminMusicTrackResponse> list(String query, MusicTrackStatus status, LanguageCode language,
                                              MusicMood mood, String genre, Boolean featured, Pageable pageable) {
        Specification<MusicTrack> specification = (root, ignored, cb) ->
                cb.notEqual(root.get("status"), MusicTrackStatus.DELETED);
        if (query != null && !query.isBlank()) {
            String pattern = "%" + query.trim().toLowerCase() + "%";
            specification = specification.and((root, ignored, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("artistName")), pattern)));
        }
        if (status != null) {
            if (status == MusicTrackStatus.DELETED) return Page.empty(pageable);
            specification = specification.and((root, ignored, cb) -> cb.equal(root.get("status"), status));
        }
        if (language != null) specification = specification.and((root, ignored, cb) ->
                cb.equal(root.get("language"), language));
        if (mood != null) specification = specification.and((root, querySpec, cb) -> {
            querySpec.distinct(true);
            return cb.equal(root.join("moods"), mood);
        });
        if (genre != null && !genre.isBlank()) specification = specification.and((root, ignored, cb) ->
                cb.equal(cb.lower(root.get("genre")), genre.trim().toLowerCase()));
        if (featured != null) specification = specification.and((root, ignored, cb) ->
                cb.equal(root.get("featured"), featured));
        return repository.findAll(specification, pageable).map(this::toResponse);
    }

    @Override
    public AdminMusicTrackResponse get(Long id) {
        return toResponse(activeTrack(id));
    }

    @Override
    @Transactional
    public AdminMusicTrackResponse update(Long id, MusicTrackUpdateRequest request) {
        MusicTrack track = activeTrack(id);
        track.setTitle(request.getTitle());
        track.setArtistName(request.getArtistName());
        track.setLanguage(request.getLanguage());
        track.setMoods(request.getMoods());
        track.setGenre(request.getGenre());
        track.setDescription(request.getDescription());
        track.setFeatured(request.getFeatured());
        track.setSortOrder(request.getSortOrder());
        try {
            return toResponse(repository.saveAndFlush(track));
        } catch (RuntimeException ex) {
            throw new MusicOperationException("MUSIC_UPDATE_FAILED", ex);
        }
    }

    @Override
    @Transactional
    public AdminMusicTrackResponse publish(Long id) {
        MusicTrack track = activeTrack(id);
        if (track.getStatus() == MusicTrackStatus.PUBLISHED) return toResponse(track);
        if (track.getSource() == MusicTrackSource.COMMUNITY) {
            throw new MusicConflictException("MUSIC_TRACK_REQUIRES_REVIEW");
        }
        if (track.getStatus() != MusicTrackStatus.DRAFT && track.getStatus() != MusicTrackStatus.UNPUBLISHED) {
            throw new MusicConflictException("MUSIC_TRACK_NOT_PUBLISHABLE");
        }
        return publishTrack(track);
    }

    private AdminMusicTrackResponse publishTrack(MusicTrack track) {
        MusicTrackStatus previousStatus = track.getStatus();
        LocalDateTime previousPublishedAt = track.getPublishedAt();
        try {
            if (!storage.draftAudioExists(track.getAudioStorageKey())
                    || track.getCoverStorageKey() != null && !storage.draftCoverExists(track.getCoverStorageKey())) {
                throw new IllegalStateException("Private media is incomplete");
            }
            storage.publishAudio(track.getAudioStorageKey());
            if (track.getCoverStorageKey() != null) storage.publishCover(track.getCoverStorageKey());
            registerPublishRollback(track.getAudioStorageKey(), track.getCoverStorageKey());
            track.setStatus(MusicTrackStatus.PUBLISHED);
            track.setPublishedAt(LocalDateTime.now());
            return toResponse(repository.saveAndFlush(track));
        } catch (RuntimeException ex) {
            unpublishQuietly(track.getAudioStorageKey(), track.getCoverStorageKey());
            track.setStatus(previousStatus);
            track.setPublishedAt(previousPublishedAt);
            throw new MusicOperationException("MUSIC_PUBLISH_FAILED", ex);
        }
    }

    @Override
    @Transactional
    public AdminMusicTrackResponse approve(Long id, MusicTrackApprovalRequest request) {
        MusicTrack track = lockedActiveTrack(id);
        requirePendingCommunity(track);
        track.setMoods(request.getMoods());
        publishTrack(track);
        try {
            track.setReviewedAt(LocalDateTime.now());
            track.setRejectionReason(null);
            return toResponse(repository.saveAndFlush(track));
        } catch (RuntimeException ex) {
            throw new MusicOperationException("MUSIC_APPROVAL_FAILED", ex);
        }
    }

    @Override
    @Transactional
    public AdminMusicTrackResponse reject(Long id, String reason) {
        MusicTrack track = lockedActiveTrack(id);
        requirePendingCommunity(track);
        String cleanReason = reason == null ? "" : reason.trim();
        if (cleanReason.isEmpty() || cleanReason.length() > 500) {
            throw new BadRequestException("MUSIC_REJECTION_REASON_REQUIRED");
        }
        track.setStatus(MusicTrackStatus.REJECTED);
        track.setPublishedAt(null);
        track.setReviewedAt(LocalDateTime.now());
        track.setRejectionReason(cleanReason);
        try {
            return toResponse(repository.saveAndFlush(track));
        } catch (RuntimeException ex) {
            throw new MusicOperationException("MUSIC_REJECTION_FAILED", ex);
        }
    }

    @Override
    @Transactional
    public AdminMusicTrackResponse unpublish(Long id) {
        MusicTrack track = activeTrack(id);
        if (track.getStatus() == MusicTrackStatus.UNPUBLISHED) return toResponse(track);
        if (track.getStatus() != MusicTrackStatus.PUBLISHED) {
            throw new BadRequestException("MUSIC_NOT_PUBLISHED");
        }
        LocalDateTime previousPublishedAt = track.getPublishedAt();
        try {
            if (!storage.draftAudioExists(track.getAudioStorageKey())
                    || track.getCoverStorageKey() != null && !storage.draftCoverExists(track.getCoverStorageKey())) {
                throw new IllegalStateException("Private media is incomplete");
            }
            storage.unpublishAudio(track.getAudioStorageKey());
            if (track.getCoverStorageKey() != null) storage.unpublishCover(track.getCoverStorageKey());
            registerUnpublishRollback(track.getAudioStorageKey(), track.getCoverStorageKey());
            track.setStatus(MusicTrackStatus.UNPUBLISHED);
            track.setPublishedAt(null);
            return toResponse(repository.saveAndFlush(track));
        } catch (RuntimeException ex) {
            publishQuietly(track.getAudioStorageKey(), track.getCoverStorageKey());
            track.setStatus(MusicTrackStatus.PUBLISHED);
            track.setPublishedAt(previousPublishedAt);
            throw new MusicOperationException("MUSIC_UNPUBLISH_FAILED", ex);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        MusicTrack track = activeTrack(id);
        MusicTrackStatus previousStatus = track.getStatus();
        LocalDateTime previousPublishedAt = track.getPublishedAt();
        List<MusicStorageService.QuarantinedMusicFile> quarantined = new ArrayList<>();
        try {
            add(quarantined, storage.quarantinePrivateAudio(track.getAudioStorageKey()));
            add(quarantined, storage.quarantinePrivateCover(track.getCoverStorageKey()));
            add(quarantined, storage.quarantinePublicAudio(track.getAudioStorageKey()));
            add(quarantined, storage.quarantinePublicCover(track.getCoverStorageKey()));
            registerDeleteCompletion(quarantined);
            track.setStatus(MusicTrackStatus.DELETED);
            track.setPublishedAt(null);
            repository.saveAndFlush(track);
            if (!TransactionSynchronizationManager.isSynchronizationActive()) purgeAll(quarantined);
        } catch (RuntimeException ex) {
            restoreAll(quarantined);
            track.setStatus(previousStatus);
            track.setPublishedAt(previousPublishedAt);
            throw new MusicOperationException("MUSIC_DELETE_FAILED", ex);
        }
    }

    @Override
    public MusicStorageService.StoredMusicResource getPrivateAudio(Long id) {
        MusicTrack track = activeTrack(id);
        return storage.getPrivateAudio(track.getAudioStorageKey());
    }

    @Override
    public MusicStorageService.StoredMusicResource getPrivateCover(Long id) {
        MusicTrack track = activeTrack(id);
        if (track.getCoverStorageKey() == null) throw new ResourceNotFoundException("MUSIC_COVER_NOT_FOUND");
        return storage.getPrivateCover(track.getCoverStorageKey());
    }

    private MusicTrack activeTrack(Long id) {
        return repository.findByIdAndStatusNot(id, MusicTrackStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("MUSIC_TRACK_NOT_FOUND"));
    }

    private MusicTrack lockedActiveTrack(Long id) {
        return repository.findActiveByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("MUSIC_TRACK_NOT_FOUND"));
    }

    private void requirePendingCommunity(MusicTrack track) {
        if (track.getSource() != MusicTrackSource.COMMUNITY
                || track.getStatus() != MusicTrackStatus.PENDING_REVIEW) {
            throw new MusicConflictException("MUSIC_TRACK_NOT_PENDING_REVIEW");
        }
    }

    private void registerPublishRollback(String audioKey, String coverKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) unpublishQuietly(audioKey, coverKey);
            }
        });
    }

    private void registerUnpublishRollback(String audioKey, String coverKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) publishQuietly(audioKey, coverKey);
            }
        });
    }

    private void registerDeleteCompletion(List<MusicStorageService.QuarantinedMusicFile> quarantined) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        List<MusicStorageService.QuarantinedMusicFile> snapshot = List.copyOf(quarantined);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) purgeAll(snapshot); else restoreAll(snapshot);
            }
        });
    }

    private void publishQuietly(String audioKey, String coverKey) {
        quietly(() -> storage.publishAudio(audioKey));
        if (coverKey != null) quietly(() -> storage.publishCover(coverKey));
    }

    private void unpublishQuietly(String audioKey, String coverKey) {
        quietly(() -> storage.unpublishAudio(audioKey));
        if (coverKey != null) quietly(() -> storage.unpublishCover(coverKey));
    }

    private void restoreAll(List<MusicStorageService.QuarantinedMusicFile> files) {
        List<MusicStorageService.QuarantinedMusicFile> reverse = new ArrayList<>(files);
        Collections.reverse(reverse);
        reverse.forEach(file -> quietly(() -> storage.restoreQuarantined(file)));
    }

    private void purgeAll(List<MusicStorageService.QuarantinedMusicFile> files) {
        files.forEach(file -> quietly(() -> storage.purgeQuarantined(file)));
    }

    private void add(List<MusicStorageService.QuarantinedMusicFile> files,
                     MusicStorageService.QuarantinedMusicFile file) {
        if (file != null) files.add(file);
    }

    private void quietly(Runnable operation) {
        try { operation.run(); } catch (RuntimeException ignored) { }
    }

    private AdminMusicTrackResponse toResponse(MusicTrack track) {
        return AdminMusicTrackResponse.builder()
                .id(track.getId()).title(track.getTitle()).artist(track.getArtistName())
                .language(track.getLanguage()).moods(track.getMoods()).genre(track.getGenre())
                .description(track.getDescription())
                .audioUrl(com.mka.util.MediaUrlUtils.toAbsoluteUrl("/api/admin/music/tracks/" + track.getId() + "/audio"))
                .coverUrl(track.getCoverStorageKey() == null ? null
                        : com.mka.util.MediaUrlUtils.toAbsoluteUrl("/api/admin/music/tracks/" + track.getId() + "/cover"))
                .durationSeconds(track.getDurationSeconds()).mimeType(track.getMimeType())
                .fileSizeBytes(track.getFileSizeBytes()).status(track.getStatus())
                .source(track.getSource())
                .featured(track.getFeatured()).sortOrder(track.getSortOrder())
                .createdAt(track.getCreatedAt()).updatedAt(track.getUpdatedAt())
                .publishedAt(track.getPublishedAt())
                .reviewedAt(track.getReviewedAt()).rejectionReason(track.getRejectionReason())
                .uploader(AdminMusicUploaderResponse.builder().id(track.getUploadedBy().getId())
                        .displayName(track.getUploadedBy().getFullName()).build())
                .build();
    }
}
