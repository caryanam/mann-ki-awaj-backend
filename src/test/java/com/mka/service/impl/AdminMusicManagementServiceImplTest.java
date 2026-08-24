package com.mka.service.impl;

import com.mka.config.MusicStorageProperties;
import com.mka.controller.MusicMediaController;
import com.mka.dto.request.MusicTrackUpdateRequest;
import com.mka.entity.MusicTrack;
import com.mka.entity.User;
import com.mka.enums.LanguageCode;
import com.mka.enums.MusicMood;
import com.mka.enums.MusicTrackStatus;
import com.mka.enums.MusicTrackSource;
import com.mka.exception.MusicConflictException;
import com.mka.enums.Role;
import com.mka.exception.MusicOperationException;
import com.mka.exception.ResourceNotFoundException;
import com.mka.repository.MusicTrackRepository;
import com.mka.service.MusicStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminMusicManagementServiceImplTest {

    @TempDir java.nio.file.Path tempDir;
    private LocalMusicStorageService realStorage;
    private MusicTrackRepository repository;

    @BeforeEach
    void setUp() {
        MusicStorageProperties properties = new MusicStorageProperties();
        properties.setRoot(tempDir.resolve("music").toString());
        realStorage = new LocalMusicStorageService(properties);
        realStorage.initialize();
        repository = mock(MusicTrackRepository.class);
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listIncludesAllActiveStatusesAndReturnsSafeAdminFields() {
        var pageable = PageRequest.of(0, 20);
        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of(
                track(MusicTrackStatus.DRAFT, "draft.mp3", null),
                track(MusicTrackStatus.PUBLISHED, "published.mp3", null),
                track(MusicTrackStatus.UNPUBLISHED, "unpublished.mp3", null)), pageable, 3));
        var result = service(realStorage).list("song", null, LanguageCode.HI, MusicMood.CALM,
                "folk", true, pageable);
        assertThat(result.getContent()).extracting(r -> r.getStatus()).containsExactly(
                MusicTrackStatus.DRAFT, MusicTrackStatus.PUBLISHED, MusicTrackStatus.UNPUBLISHED);
        assertThat(result.getContent().get(0).getUploader().getDisplayName()).isEqualTo("Music Admin");
        assertThat(result.toString()).doesNotContain("admin@example.com", "encoded", tempDir.toString());
    }

    @Test
    void detailAllowsActiveStatusesButDeletedAndMissingAre404() {
        MusicTrack draft = track(MusicTrackStatus.DRAFT, "draft.mp3", null);
        when(repository.findByIdAndStatusNot(7L, MusicTrackStatus.DELETED)).thenReturn(Optional.of(draft));
        assertThat(service(realStorage).get(7L).getStatus()).isEqualTo(MusicTrackStatus.DRAFT);
        when(repository.findByIdAndStatusNot(9L, MusicTrackStatus.DELETED)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service(realStorage).get(9L)).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("MUSIC_TRACK_NOT_FOUND");
    }

    @Test
    void metadataUpdateChangesOnlyEditableFields() {
        MusicTrack track = track(MusicTrackStatus.PUBLISHED, "fixed.mp3", "fixed.png");
        track.setPublishedAt(LocalDateTime.of(2026, 1, 1, 1, 1));
        when(repository.findByIdAndStatusNot(7L, MusicTrackStatus.DELETED)).thenReturn(Optional.of(track));
        User uploader = track.getUploadedBy();
        LocalDateTime publishedAt = track.getPublishedAt();
        var response = service(realStorage).update(7L, update());
        assertThat(response.getTitle()).isEqualTo("Updated title");
        assertThat(track.getArtistName()).isEqualTo("Updated artist");
        assertThat(track.getLanguage()).isEqualTo(LanguageCode.MR);
        assertThat(track.getMood()).isEqualTo(MusicMood.ENERGETIC);
        assertThat(track.getGenre()).isEqualTo("Rock");
        assertThat(track.getFeatured()).isTrue();
        assertThat(track.getSortOrder()).isEqualTo(8);
        assertThat(track.getAudioStorageKey()).isEqualTo("fixed.mp3");
        assertThat(track.getCoverStorageKey()).isEqualTo("fixed.png");
        assertThat(track.getUploadedBy()).isSameAs(uploader);
        assertThat(track.getStatus()).isEqualTo(MusicTrackStatus.PUBLISHED);
        assertThat(track.getPublishedAt()).isEqualTo(publishedAt);
    }

    @Test
    void publishCopiesPrivateMediaMakesCatalogVisibleAndPreservesPublicRangeDelivery() throws Exception {
        MusicTrack track = privateTrack(MusicTrackStatus.DRAFT, true);
        stubActive(track);
        var response = service(realStorage).publish(7L);
        assertThat(response.getStatus()).isEqualTo(MusicTrackStatus.PUBLISHED);
        assertThat(response.getPublishedAt()).isNotNull();
        assertThat(realStorage.draftAudioExists(track.getAudioStorageKey())).isTrue();
        assertThat(realStorage.publicAudioExists(track.getAudioStorageKey())).isTrue();
        assertThat(realStorage.publicCoverExists(track.getCoverStorageKey())).isTrue();
        when(repository.findByIdAndStatus(7L, MusicTrackStatus.PUBLISHED)).thenReturn(Optional.of(track));
        assertThat(new MusicCatalogServiceImpl(repository).getPublishedTrack(7L).getAudioUrl())
                .isEqualTo("https://api.awaazmanki.com/media/music/audio/" + track.getAudioStorageKey());
        assertThat(new MusicCatalogServiceImpl(repository).getPublishedTrack(7L).getAudioUrl())
                .isEqualTo("https://api.awaazmanki.com/media/music/audio/" + track.getAudioStorageKey());
        MockMvc publicMedia = MockMvcBuilders.standaloneSetup(new MusicMediaController(realStorage)).build();
        MvcResult initial = publicMedia.perform(get("/media/music/audio/" + track.getAudioStorageKey())
                        .header("Range", "bytes=1-3"))
                .andExpect(request().asyncStarted()).andReturn();
        publicMedia.perform(asyncDispatch(initial)).andExpect(status().isPartialContent())
                .andExpect(content().bytes(new byte[]{2, 3, 4}));
    }

    @Test
    void repeatedPublishIsIdempotentAndDoesNotTouchStorage() {
        MusicStorageService storage = mock(MusicStorageService.class);
        MusicTrack track = track(MusicTrackStatus.PUBLISHED, "audio.mp3", null);
        stubActive(track);
        assertThat(service(storage).publish(7L).getStatus()).isEqualTo(MusicTrackStatus.PUBLISHED);
        verifyNoInteractions(storage);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void audioOrCoverPublishFailureLeavesPrivateMediaAndNoPublicFiles() {
        MusicTrack track = privateTrack(MusicTrackStatus.DRAFT, true);
        stubActive(track);
        LocalMusicStorageService storage = spy(realStorage);
        doThrow(new RuntimeException("cover failed")).when(storage).publishCover(track.getCoverStorageKey());
        assertThatThrownBy(() -> service(storage).publish(7L)).isInstanceOf(MusicOperationException.class)
                .hasMessage("MUSIC_PUBLISH_FAILED");
        assertThat(realStorage.publicAudioExists(track.getAudioStorageKey())).isFalse();
        assertThat(realStorage.publicCoverExists(track.getCoverStorageKey())).isFalse();
        assertThat(realStorage.draftAudioExists(track.getAudioStorageKey())).isTrue();
        assertThat(realStorage.draftCoverExists(track.getCoverStorageKey())).isTrue();
        assertThat(track.getStatus()).isEqualTo(MusicTrackStatus.DRAFT);
    }

    @Test
    void audioPublishFailureNeverPersistsOrCreatesPublicMedia() {
        MusicTrack track = privateTrack(MusicTrackStatus.DRAFT, false);
        stubActive(track);
        LocalMusicStorageService storage = spy(realStorage);
        doThrow(new RuntimeException("audio failed")).when(storage).publishAudio(track.getAudioStorageKey());
        assertThatThrownBy(() -> service(storage).publish(7L)).isInstanceOf(MusicOperationException.class)
                .hasMessage("MUSIC_PUBLISH_FAILED");
        assertThat(track.getStatus()).isEqualTo(MusicTrackStatus.DRAFT);
        assertThat(realStorage.publicAudioExists(track.getAudioStorageKey())).isFalse();
        assertThat(realStorage.draftAudioExists(track.getAudioStorageKey())).isTrue();
        verify(repository, never()).saveAndFlush(track);
    }

    @Test
    void databasePublishFailureRemovesPublicCopiesAndPreservesPreviousState() {
        MusicTrack track = privateTrack(MusicTrackStatus.UNPUBLISHED, true);
        stubActive(track);
        when(repository.saveAndFlush(track)).thenThrow(new RuntimeException("db failed"));
        assertThatThrownBy(() -> service(realStorage).publish(7L)).isInstanceOf(MusicOperationException.class);
        assertThat(track.getStatus()).isEqualTo(MusicTrackStatus.UNPUBLISHED);
        assertThat(realStorage.publicAudioExists(track.getAudioStorageKey())).isFalse();
        assertThat(realStorage.draftAudioExists(track.getAudioStorageKey())).isTrue();
    }

    @Test
    void transactionRollbackAfterPublishRemovesPublicCopies() {
        MusicTrack track = privateTrack(MusicTrackStatus.DRAFT, false);
        stubActive(track);
        withSynchronization(() -> service(realStorage).publish(7L), TransactionSynchronization.STATUS_ROLLED_BACK);
        assertThat(realStorage.publicAudioExists(track.getAudioStorageKey())).isFalse();
        assertThat(realStorage.draftAudioExists(track.getAudioStorageKey())).isTrue();
    }

    @Test
    void unpublishRemovesPublicMediaKeepsPrivateMediaAndClearsTimestamp() {
        MusicTrack track = privateTrack(MusicTrackStatus.DRAFT, true);
        stubActive(track);
        service(realStorage).publish(7L);
        var response = service(realStorage).unpublish(7L);
        assertThat(response.getStatus()).isEqualTo(MusicTrackStatus.UNPUBLISHED);
        assertThat(response.getPublishedAt()).isNull();
        assertThat(realStorage.publicAudioExists(track.getAudioStorageKey())).isFalse();
        assertThat(realStorage.publicCoverExists(track.getCoverStorageKey())).isFalse();
        assertThat(realStorage.draftAudioExists(track.getAudioStorageKey())).isTrue();
        when(repository.findByIdAndStatus(7L, MusicTrackStatus.PUBLISHED)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new MusicCatalogServiceImpl(repository).getPublishedTrack(7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void transactionRollbackAfterUnpublishRestoresPublicCopies() {
        MusicTrack track = privateTrack(MusicTrackStatus.DRAFT, false);
        stubActive(track);
        service(realStorage).publish(7L);
        withSynchronization(() -> service(realStorage).unpublish(7L), TransactionSynchronization.STATUS_ROLLED_BACK);
        assertThat(realStorage.publicAudioExists(track.getAudioStorageKey())).isTrue();
        assertThat(realStorage.draftAudioExists(track.getAudioStorageKey())).isTrue();
    }

    @Test
    void databaseUnpublishFailureRestoresPublicMediaAndPublishedState() {
        MusicTrack track = privateTrack(MusicTrackStatus.DRAFT, false);
        stubActive(track);
        service(realStorage).publish(7L);
        LocalDateTime publishedAt = track.getPublishedAt();
        when(repository.saveAndFlush(track)).thenThrow(new RuntimeException("db failed"));
        assertThatThrownBy(() -> service(realStorage).unpublish(7L)).isInstanceOf(MusicOperationException.class)
                .hasMessage("MUSIC_UNPUBLISH_FAILED");
        assertThat(track.getStatus()).isEqualTo(MusicTrackStatus.PUBLISHED);
        assertThat(track.getPublishedAt()).isEqualTo(publishedAt);
        assertThat(realStorage.publicAudioExists(track.getAudioStorageKey())).isTrue();
    }

    @Test
    void unpublishStorageFailureKeepsTrackPublishedAndPublicMediaAvailable() {
        MusicTrack track = privateTrack(MusicTrackStatus.DRAFT, false);
        stubActive(track);
        service(realStorage).publish(7L);
        LocalMusicStorageService storage = spy(realStorage);
        doThrow(new RuntimeException("remove failed")).when(storage).unpublishAudio(track.getAudioStorageKey());
        assertThatThrownBy(() -> service(storage).unpublish(7L)).isInstanceOf(MusicOperationException.class)
                .hasMessage("MUSIC_UNPUBLISH_FAILED");
        assertThat(track.getStatus()).isEqualTo(MusicTrackStatus.PUBLISHED);
        assertThat(realStorage.publicAudioExists(track.getAudioStorageKey())).isTrue();
    }

    @Test
    void draftCanPublishUnpublishAndRepublishWithoutChangingKeys() {
        MusicTrack track = privateTrack(MusicTrackStatus.DRAFT, false);
        stubActive(track);
        String key = track.getAudioStorageKey();
        service(realStorage).publish(7L);
        service(realStorage).unpublish(7L);
        service(realStorage).publish(7L);
        assertThat(track.getStatus()).isEqualTo(MusicTrackStatus.PUBLISHED);
        assertThat(track.getAudioStorageKey()).isEqualTo(key);
        assertThat(realStorage.publicAudioExists(key)).isTrue();
    }

    @Test
    void deleteDraftUnpublishedAndPublishedCleansEveryLocation() {
        for (MusicTrackStatus initial : List.of(MusicTrackStatus.DRAFT,
                MusicTrackStatus.UNPUBLISHED, MusicTrackStatus.PUBLISHED)) {
            reset(repository);
            when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
            MusicTrack track = privateTrack(initial == MusicTrackStatus.PUBLISHED
                    ? MusicTrackStatus.DRAFT : initial, true);
            stubActive(track);
            if (initial == MusicTrackStatus.PUBLISHED) service(realStorage).publish(7L);
            service(realStorage).delete(7L);
            assertThat(track.getStatus()).isEqualTo(MusicTrackStatus.DELETED);
            assertThat(realStorage.draftAudioExists(track.getAudioStorageKey())).isFalse();
            assertThat(realStorage.draftCoverExists(track.getCoverStorageKey())).isFalse();
            assertThat(realStorage.publicAudioExists(track.getAudioStorageKey())).isFalse();
            assertThat(realStorage.publicCoverExists(track.getCoverStorageKey())).isFalse();
        }
    }

    @Test
    void databaseDeleteFailureRestoresPrivateAndPublicFilesAndPreviousStatus() {
        MusicTrack track = privateTrack(MusicTrackStatus.DRAFT, true);
        stubActive(track);
        service(realStorage).publish(7L);
        when(repository.saveAndFlush(track)).thenThrow(new RuntimeException("db failed"));
        assertThatThrownBy(() -> service(realStorage).delete(7L)).isInstanceOf(MusicOperationException.class)
                .hasMessage("MUSIC_DELETE_FAILED");
        assertThat(track.getStatus()).isEqualTo(MusicTrackStatus.PUBLISHED);
        assertThat(realStorage.draftAudioExists(track.getAudioStorageKey())).isTrue();
        assertThat(realStorage.publicAudioExists(track.getAudioStorageKey())).isTrue();
    }

    @Test
    void publicQuarantineFailureDoesNotMarkDeletedAndRestoresEarlierMoves() {
        MusicTrack track = privateTrack(MusicTrackStatus.DRAFT, false);
        stubActive(track);
        service(realStorage).publish(7L);
        LocalMusicStorageService storage = spy(realStorage);
        doThrow(new RuntimeException("cannot remove public audio"))
                .when(storage).quarantinePublicAudio(track.getAudioStorageKey());
        assertThatThrownBy(() -> service(storage).delete(7L)).isInstanceOf(MusicOperationException.class);
        assertThat(track.getStatus()).isEqualTo(MusicTrackStatus.PUBLISHED);
        assertThat(realStorage.draftAudioExists(track.getAudioStorageKey())).isTrue();
        assertThat(realStorage.publicAudioExists(track.getAudioStorageKey())).isTrue();
    }

    @Test
    void transactionDeleteRollbackRestoresAllFiles() {
        MusicTrack track = privateTrack(MusicTrackStatus.DRAFT, false);
        stubActive(track);
        service(realStorage).publish(7L);
        withSynchronization(() -> service(realStorage).delete(7L), TransactionSynchronization.STATUS_ROLLED_BACK);
        assertThat(realStorage.draftAudioExists(track.getAudioStorageKey())).isTrue();
        assertThat(realStorage.publicAudioExists(track.getAudioStorageKey())).isTrue();
    }

    @Test
    void approvePendingCommunityTrackReusesPublicationAndMakesMediaPublic() {
        MusicTrack track = privateTrack(MusicTrackStatus.PENDING_REVIEW, true);
        track.setSource(MusicTrackSource.COMMUNITY);
        when(repository.findActiveByIdForUpdate(7L)).thenReturn(Optional.of(track));
        var response = service(realStorage).approve(7L);
        assertThat(response.getStatus()).isEqualTo(MusicTrackStatus.PUBLISHED);
        assertThat(response.getSource()).isEqualTo(MusicTrackSource.COMMUNITY);
        assertThat(response.getReviewedAt()).isNotNull();
        assertThat(realStorage.publicAudioExists(track.getAudioStorageKey())).isTrue();
        assertThat(realStorage.publicCoverExists(track.getCoverStorageKey())).isTrue();
    }

    @Test
    void rejectPendingCommunityTrackKeepsPrivateMediaAndStoresReason() {
        MusicTrack track = privateTrack(MusicTrackStatus.PENDING_REVIEW, true);
        track.setSource(MusicTrackSource.COMMUNITY);
        when(repository.findActiveByIdForUpdate(7L)).thenReturn(Optional.of(track));
        var response = service(realStorage).reject(7L, "  Rights could not be confirmed.  ");
        assertThat(response.getStatus()).isEqualTo(MusicTrackStatus.REJECTED);
        assertThat(response.getRejectionReason()).isEqualTo("Rights could not be confirmed.");
        assertThat(realStorage.draftAudioExists(track.getAudioStorageKey())).isTrue();
        assertThat(realStorage.publicAudioExists(track.getAudioStorageKey())).isFalse();
    }

    @Test
    void reviewRejectsNonPendingOrPlatformTracksWithStableConflict() {
        for (MusicTrack invalid : List.of(
                track(MusicTrackStatus.DRAFT, "draft.mp3", null),
                track(MusicTrackStatus.PUBLISHED, "published.mp3", null),
                track(MusicTrackStatus.REJECTED, "rejected.mp3", null))) {
            when(repository.findActiveByIdForUpdate(7L)).thenReturn(Optional.of(invalid));
            assertThatThrownBy(() -> service(realStorage).approve(7L))
                    .isInstanceOf(MusicConflictException.class)
                    .hasMessage("MUSIC_TRACK_NOT_PENDING_REVIEW");
        }
    }

    @Test
    void approvalFailureRollsBackPublicCopiesAndPendingStatus() {
        MusicTrack track = privateTrack(MusicTrackStatus.PENDING_REVIEW, true);
        track.setSource(MusicTrackSource.COMMUNITY);
        when(repository.findActiveByIdForUpdate(7L)).thenReturn(Optional.of(track));
        LocalMusicStorageService failedStorage = spy(realStorage);
        doThrow(new RuntimeException("cover promotion failed")).when(failedStorage)
                .publishCover(track.getCoverStorageKey());
        assertThatThrownBy(() -> service(failedStorage).approve(7L))
                .isInstanceOf(MusicOperationException.class);
        assertThat(track.getStatus()).isEqualTo(MusicTrackStatus.PENDING_REVIEW);
        assertThat(realStorage.publicAudioExists(track.getAudioStorageKey())).isFalse();
        assertThat(realStorage.draftAudioExists(track.getAudioStorageKey())).isTrue();
    }

    private AdminMusicManagementServiceImpl service(MusicStorageService storage) {
        return new AdminMusicManagementServiceImpl(repository, storage);
    }

    private void stubActive(MusicTrack track) {
        when(repository.findByIdAndStatusNot(7L, MusicTrackStatus.DELETED)).thenReturn(Optional.of(track));
    }

    private MusicTrack privateTrack(MusicTrackStatus status, boolean cover) {
        var stagedAudio = realStorage.stageAudio(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5}), "mp3");
        realStorage.promoteDraftAudio(stagedAudio);
        String coverKey = null;
        if (cover) {
            var stagedCover = realStorage.stageCover(new ByteArrayInputStream(new byte[]{6, 7, 8}), "png");
            realStorage.promoteDraftCover(stagedCover);
            coverKey = stagedCover.storageKey();
        }
        return track(status, stagedAudio.storageKey(), coverKey);
    }

    private MusicTrack track(MusicTrackStatus status, String audioKey, String coverKey) {
        User user = User.builder().fullName("Music Admin").email("admin@example.com")
                .password("encoded").role(Role.ADMIN).build();
        user.setId(2L);
        MusicTrack track = MusicTrack.builder().title("Song").artistName("Artist")
                .language(LanguageCode.HI).mood(MusicMood.CALM).genre("Folk")
                .audioStorageKey(audioKey).coverStorageKey(coverKey).mimeType("audio/mpeg")
                .fileSizeBytes(5L).status(status).featured(false).sortOrder(0).uploadedBy(user).build();
        track.setId(7L);
        track.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        if (status == MusicTrackStatus.PUBLISHED) track.setPublishedAt(LocalDateTime.now());
        return track;
    }

    private MusicTrackUpdateRequest update() {
        MusicTrackUpdateRequest request = new MusicTrackUpdateRequest();
        request.setTitle("Updated title"); request.setArtistName("Updated artist");
        request.setLanguage(LanguageCode.MR); request.setMood(MusicMood.ENERGETIC);
        request.setGenre("Rock"); request.setDescription("Updated description");
        request.setFeatured(true); request.setSortOrder(8);
        return request;
    }

    private void withSynchronization(Runnable operation, int completionStatus) {
        TransactionSynchronizationManager.initSynchronization();
        try {
            operation.run();
            var synchronizations = TransactionSynchronizationManager.getSynchronizations();
            synchronizations.forEach(sync -> sync.afterCompletion(completionStatus));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
