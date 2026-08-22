package com.mka.service.impl;

import com.mka.dto.request.MusicTrackUploadRequest;
import com.mka.dto.response.AdminMusicTrackResponse;
import com.mka.dto.response.AdminMusicUploaderResponse;
import com.mka.dto.request.UserMusicTrackUploadRequest;
import com.mka.entity.MusicTrack;
import com.mka.entity.User;
import com.mka.enums.MusicTrackStatus;
import com.mka.enums.MusicTrackSource;
import com.mka.enums.Role;
import com.mka.exception.MusicStorageException;
import com.mka.exception.ResourceNotFoundException;
import com.mka.repository.AdminRepository;
import com.mka.repository.MusicTrackRepository;
import com.mka.repository.UserRepository;
import com.mka.service.MusicFileValidator;
import com.mka.service.MusicStorageService;
import com.mka.service.MusicUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MusicUploadServiceImpl implements MusicUploadService {

    private final MusicFileValidator fileValidator;
    private final MusicStorageService storageService;
    private final MusicTrackRepository trackRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    @Override
    @Transactional
    public AdminMusicTrackResponse upload(String authenticatedAdminEmail,
                                          MusicTrackUploadRequest request,
                                          MultipartFile audio,
                                          MultipartFile cover) {
        User uploader = userRepository.findByEmail(authenticatedAdminEmail)
                .filter(user -> user.getRole() == Role.ADMIN)
                .orElseGet(() -> adminRepository.findByEmail(authenticatedAdminEmail)
                        .filter(admin -> admin.getRole() == Role.ADMIN)
                        .map(admin -> userRepository.findByEmail(admin.getEmail())
                                .orElseGet(() -> userRepository.save(User.builder()
                                        .fullName(admin.getFullName())
                                        .email(admin.getEmail())
                                        .mobileNumber(admin.getMobileNumber())
                                        .password(admin.getPassword())
                                        .role(Role.ADMIN)
                                        .active(true)
                                        .deleted(false)
                                        .emailVerified(true)
                                        .mobileVerified(true)
                                        .build())))
                        .orElseThrow(() -> new ResourceNotFoundException("Authenticated admin not found")));
        MusicTrack saved = storeTrack(uploader, request.getTitle(), request.getArtistName(), request.getLanguage(),
                request.getMood(), request.getGenre(), request.getDescription(), audio, cover,
                MusicTrackStatus.DRAFT, MusicTrackSource.PLATFORM, false, false,
                Boolean.TRUE.equals(request.getFeatured()), request.getSortOrder() == null ? 0 : request.getSortOrder());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public MusicTrack uploadCommunity(User uploader, UserMusicTrackUploadRequest request,
                                      MultipartFile audio, MultipartFile cover) {
        return storeTrack(uploader, request.getTitle(), request.getArtistName(), request.getLanguage(),
                request.getMood(), request.getGenre(), request.getDescription(), audio, cover,
                MusicTrackStatus.PENDING_REVIEW, MusicTrackSource.COMMUNITY,
                true, true, false, 0);
    }

    private MusicTrack storeTrack(User uploader, String title, String artistName,
                                  com.mka.enums.LanguageCode language, com.mka.enums.MusicMood mood,
                                  String genre, String description, MultipartFile audio, MultipartFile cover,
                                  MusicTrackStatus status, MusicTrackSource source,
                                  boolean originalWorkConfirmed, boolean rightsConfirmed,
                                  boolean featured, int sortOrder) {
        MusicFileValidator.ValidatedFile validAudio = fileValidator.validateAudio(audio);
        MusicFileValidator.ValidatedFile validCover = cover == null || cover.isEmpty()
                ? null : fileValidator.validateCover(cover);

        MusicStorageService.StagedMusicFile stagedAudio = null;
        MusicStorageService.StagedMusicFile stagedCover = null;
        String privateAudioKey = null;
        String privateCoverKey = null;
        try {
            stagedAudio = storageService.stageAudio(audio.getInputStream(), validAudio.extension());
            if (validCover != null) {
                stagedCover = storageService.stageCover(cover.getInputStream(), validCover.extension());
            }

            storageService.promoteDraftAudio(stagedAudio);
            privateAudioKey = stagedAudio.storageKey();
            if (stagedCover != null) {
                storageService.promoteDraftCover(stagedCover);
                privateCoverKey = stagedCover.storageKey();
            }

            registerRollbackCleanup(privateAudioKey, privateCoverKey);

            MusicTrack track = MusicTrack.builder()
                    .title(title)
                    .artistName(artistName)
                    .language(language)
                    .mood(mood)
                    .genre(genre)
                    .description(description)
                    .audioStorageKey(privateAudioKey)
                    .coverStorageKey(privateCoverKey)
                    .durationSeconds(null)
                    .mimeType(validAudio.mimeType())
                    .fileSizeBytes(validAudio.size())
                    .uploadedBy(uploader)
                    .status(status)
                    .source(source)
                    .originalWorkConfirmed(originalWorkConfirmed)
                    .rightsConfirmed(rightsConfirmed)
                    .featured(featured)
                    .sortOrder(sortOrder)
                    .publishedAt(null)
                    .build();

            return trackRepository.saveAndFlush(track);
        } catch (MusicStorageException ex) {
            cleanup(stagedAudio, stagedCover, privateAudioKey, privateCoverKey);
            throw ex;
        } catch (Exception ex) {
            cleanup(stagedAudio, stagedCover, privateAudioKey, privateCoverKey);
            throw new MusicStorageException(ex);
        }
    }

    private void registerRollbackCleanup(String audioKey, String coverKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) cleanup(null, null, audioKey, coverKey);
            }
        });
    }

    private void cleanup(MusicStorageService.StagedMusicFile stagedAudio,
                         MusicStorageService.StagedMusicFile stagedCover,
                         String privateAudioKey,
                         String privateCoverKey) {
        quietly(() -> storageService.discardStaged(stagedAudio));
        quietly(() -> storageService.discardStaged(stagedCover));
        if (privateAudioKey != null) quietly(() -> storageService.deleteDraftAudio(privateAudioKey));
        if (privateCoverKey != null) quietly(() -> storageService.deleteDraftCover(privateCoverKey));
    }

    private void quietly(Runnable cleanup) {
        try { cleanup.run(); } catch (RuntimeException ignored) { }
    }

    private AdminMusicTrackResponse toResponse(MusicTrack track) {
        return AdminMusicTrackResponse.builder()
                .id(track.getId())
                .title(track.getTitle())
                .artist(track.getArtistName())
                .language(track.getLanguage())
                .mood(track.getMood())
                .genre(track.getGenre())
                .description(track.getDescription())
                .audioUrl("/api/admin/music/tracks/" + track.getId() + "/audio")
                .coverUrl(track.getCoverStorageKey() == null ? null
                        : "/api/admin/music/tracks/" + track.getId() + "/cover")
                .durationSeconds(track.getDurationSeconds())
                .mimeType(track.getMimeType())
                .fileSizeBytes(track.getFileSizeBytes())
                .status(track.getStatus())
                .source(track.getSource())
                .featured(track.getFeatured())
                .sortOrder(track.getSortOrder())
                .createdAt(track.getCreatedAt())
                .updatedAt(track.getUpdatedAt())
                .publishedAt(track.getPublishedAt())
                .reviewedAt(track.getReviewedAt())
                .rejectionReason(track.getRejectionReason())
                .uploader(AdminMusicUploaderResponse.builder()
                        .id(track.getUploadedBy().getId())
                        .displayName(track.getUploadedBy().getFullName())
                        .build())
                .build();
    }
}
