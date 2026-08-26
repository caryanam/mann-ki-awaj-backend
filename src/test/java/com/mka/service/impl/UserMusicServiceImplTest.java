package com.mka.service.impl;

import com.mka.config.MusicUploadProperties;
import com.mka.dto.request.UserMusicTrackUpdateRequest;
import com.mka.dto.request.UserMusicTrackUploadRequest;
import com.mka.entity.MusicTrack;
import com.mka.entity.User;
import com.mka.enums.LanguageCode;
import com.mka.enums.MusicMood;
import com.mka.enums.MusicTrackSource;
import com.mka.enums.MusicTrackStatus;
import com.mka.enums.Role;
import com.mka.exception.BadRequestException;
import com.mka.exception.MusicConflictException;
import com.mka.exception.ResourceNotFoundException;
import com.mka.repository.MusicTrackRepository;
import com.mka.repository.UserRepository;
import com.mka.service.AdminMusicManagementService;
import com.mka.service.MusicStorageService;
import com.mka.service.MusicUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserMusicServiceImplTest {
    private MusicTrackRepository tracks;
    private UserRepository users;
    private MusicUploadService uploads;
    private MusicStorageService storage;
    private AdminMusicManagementService lifecycle;
    private UserMusicServiceImpl service;
    private User owner;

    @BeforeEach
    void setUp() {
        tracks = mock(MusicTrackRepository.class);
        users = mock(UserRepository.class);
        uploads = mock(MusicUploadService.class);
        storage = mock(MusicStorageService.class);
        lifecycle = mock(AdminMusicManagementService.class);
        MusicUploadProperties properties = new MusicUploadProperties();
        properties.setUserMaxPending(5);
        service = new UserMusicServiceImpl(tracks, users, uploads, storage, lifecycle, properties);
        owner = user(10L, "a@example.com");
        when(users.findByEmail("a@example.com")).thenReturn(Optional.of(owner));
        when(users.findByEmailForUpdate("a@example.com")).thenReturn(Optional.of(owner));
    }

    @Test
    void uploadRequiresBothRightsDeclarations() {
        UserMusicTrackUploadRequest request = request();
        request.setRightsConfirmed(false);
        assertThatThrownBy(() -> service.upload("a@example.com", request, audio(), null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("MUSIC_RIGHTS_CONFIRMATION_REQUIRED");
        verifyNoInteractions(uploads);
    }

    @Test
    void uploadCreatesAuthenticatedOwnersPendingCommunityTrack() {
        MusicTrack pending = track(owner, MusicTrackStatus.PENDING_REVIEW);
        when(uploads.uploadCommunity(eq(owner), any(), any(), isNull())).thenReturn(pending);
        var response = service.upload("a@example.com", request(), audio(), null);
        assertThat(response.getStatus()).isEqualTo(MusicTrackStatus.PENDING_REVIEW);
        assertThat(response.getPublicAudioUrl()).isNull();
        assertThat(response.getPrivateAudioUrl()).isEqualTo("/api/music/my-tracks/42/audio");
        verify(uploads).uploadCommunity(eq(owner), any(), any(), isNull());
    }

    @Test
    void pendingQuotaReturnsStableConflict() {
        when(tracks.countByUploadedByIdAndSourceAndStatus(10L, MusicTrackSource.COMMUNITY,
                MusicTrackStatus.PENDING_REVIEW)).thenReturn(5L);
        assertThatThrownBy(() -> service.upload("a@example.com", request(), audio(), null))
                .isInstanceOf(MusicConflictException.class).hasMessage("MUSIC_UPLOAD_LIMIT_REACHED");
        verifyNoInteractions(uploads);
    }

    @Test
    void myTracksListIsOwnerAndSourceScopedAndHidesDeleted() {
        var pageable = PageRequest.of(0, 20);
        when(tracks.findByUploadedByIdAndSourceAndStatusNot(10L, MusicTrackSource.COMMUNITY,
                MusicTrackStatus.DELETED, pageable)).thenReturn(new PageImpl<>(
                        List.of(track(owner, MusicTrackStatus.PENDING_REVIEW)), pageable, 1));
        var result = service.list("a@example.com", null, pageable);
        assertThat(result.getTotalElements()).isOne();
        verify(tracks).findByUploadedByIdAndSourceAndStatusNot(10L, MusicTrackSource.COMMUNITY,
                MusicTrackStatus.DELETED, pageable);
    }

    @Test
    void ownerDetailAndPrivatePreviewReturnSafeIdBasedResources() {
        MusicTrack track = track(owner, MusicTrackStatus.REJECTED);
        when(tracks.findByIdAndStatusNot(42L, MusicTrackStatus.DELETED)).thenReturn(Optional.of(track));
        var audioResource = mock(MusicStorageService.StoredMusicResource.class);
        when(storage.getPrivateAudio("private.mp3")).thenReturn(audioResource);
        assertThat(service.get("a@example.com", 42L).getRejectionReason()).isEqualTo("Needs proof");
        assertThat(service.getPrivateAudio("a@example.com", 42L)).isSameAs(audioResource);
        assertThat(service.get("a@example.com", 42L).toString()).doesNotContain("private.mp3");
    }

    @Test
    void userBCannotReadPreviewEditOrDeleteUserATrack() {
        User other = user(11L, "b@example.com");
        when(users.findByEmail("b@example.com")).thenReturn(Optional.of(other));
        when(tracks.findByIdAndStatusNot(42L, MusicTrackStatus.DELETED))
                .thenReturn(Optional.of(track(owner, MusicTrackStatus.PENDING_REVIEW)));
        when(tracks.findActiveByIdForUpdate(42L))
                .thenReturn(Optional.of(track(owner, MusicTrackStatus.PENDING_REVIEW)));
        assertNotFound(() -> service.get("b@example.com", 42L));
        assertNotFound(() -> service.getPrivateAudio("b@example.com", 42L));
        assertNotFound(() -> service.getPrivateCover("b@example.com", 42L));
        assertNotFound(() -> service.update("b@example.com", 42L, update()));
        assertNotFound(() -> service.delete("b@example.com", 42L));
        verifyNoInteractions(lifecycle);
    }

    @Test
    void metadataEditOnlyAllowsPendingReviewAndCannotChangeControlledFields() {
        MusicTrack pending = track(owner, MusicTrackStatus.PENDING_REVIEW);
        when(tracks.findActiveByIdForUpdate(42L)).thenReturn(Optional.of(pending));
        when(tracks.saveAndFlush(pending)).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.update("a@example.com", 42L, update());
        assertThat(response.getTitle()).isEqualTo("Updated");
        assertThat(pending.getStatus()).isEqualTo(MusicTrackStatus.PENDING_REVIEW);
        assertThat(pending.getSource()).isEqualTo(MusicTrackSource.COMMUNITY);
        assertThat(pending.getFeatured()).isFalse();

        MusicTrack published = track(owner, MusicTrackStatus.PUBLISHED);
        when(tracks.findActiveByIdForUpdate(42L)).thenReturn(Optional.of(published));
        assertThatThrownBy(() -> service.update("a@example.com", 42L, update()))
                .isInstanceOf(MusicConflictException.class).hasMessage("MUSIC_TRACK_NOT_EDITABLE");
    }

    @Test
    void ownerCanDeletePendingAndRejectedButNotPublished() {
        for (MusicTrackStatus status : List.of(MusicTrackStatus.PENDING_REVIEW, MusicTrackStatus.REJECTED)) {
            when(tracks.findActiveByIdForUpdate(42L)).thenReturn(Optional.of(track(owner, status)));
            service.delete("a@example.com", 42L);
            verify(lifecycle, times(status == MusicTrackStatus.PENDING_REVIEW ? 1 : 2)).delete(42L);
        }
        when(tracks.findActiveByIdForUpdate(42L)).thenReturn(Optional.of(track(owner, MusicTrackStatus.PUBLISHED)));
        assertThatThrownBy(() -> service.delete("a@example.com", 42L))
                .isInstanceOf(MusicConflictException.class).hasMessage("MUSIC_TRACK_NOT_DELETABLE");
    }

    private void assertNotFound(Runnable operation) {
        assertThatThrownBy(operation::run).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("MUSIC_TRACK_NOT_FOUND");
    }

    private User user(long id, String email) {
        User user = User.builder().fullName("User " + id).email(email).password("encoded")
                .role(Role.USER).active(true).deleted(false).build();
        user.setId(id);
        return user;
    }

    private MusicTrack track(User user, MusicTrackStatus status) {
        MusicTrack track = MusicTrack.builder().title("Original").artistName("Artist")
                .language(LanguageCode.MR).moods(Set.of(MusicMood.CALM)).audioStorageKey("private.mp3")
                .coverStorageKey("cover.png").mimeType("audio/mpeg").fileSizeBytes(8L)
                .status(status).source(MusicTrackSource.COMMUNITY).originalWorkConfirmed(true)
                .rightsConfirmed(true).featured(false).sortOrder(0).uploadedBy(user)
                .rejectionReason(status == MusicTrackStatus.REJECTED ? "Needs proof" : null).build();
        track.setId(42L);
        track.setCreatedAt(LocalDateTime.now());
        return track;
    }

    private UserMusicTrackUploadRequest request() {
        UserMusicTrackUploadRequest request = new UserMusicTrackUploadRequest();
        request.setTitle("Original"); request.setArtistName("Artist");
        request.setLanguage(LanguageCode.MR); request.setMoods(Set.of(MusicMood.CALM));
        request.setOriginalWorkConfirmed(true); request.setRightsConfirmed(true);
        return request;
    }

    private UserMusicTrackUpdateRequest update() {
        UserMusicTrackUpdateRequest request = new UserMusicTrackUpdateRequest();
        request.setTitle("Updated"); request.setArtistName("Updated artist");
        request.setLanguage(LanguageCode.HI); request.setMoods(Set.of(MusicMood.FOCUS));
        request.setGenre("Lo-fi"); request.setDescription("Updated description");
        return request;
    }

    private MockMultipartFile audio() {
        return new MockMultipartFile("audio", "track.mp3", "audio/mpeg", new byte[]{'I','D','3',0});
    }
}
