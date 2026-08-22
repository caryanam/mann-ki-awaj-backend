package com.mka.service.impl;

import com.mka.config.MusicStorageProperties;
import com.mka.config.MusicUploadProperties;
import com.mka.dto.request.MusicTrackUploadRequest;
import com.mka.dto.request.UserMusicTrackUploadRequest;
import com.mka.entity.MusicTrack;
import com.mka.entity.User;
import com.mka.enums.LanguageCode;
import com.mka.enums.MusicMood;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MusicUploadServiceImplTest {

    @TempDir Path tempDir;
    private LocalMusicStorageService storage;
    private MusicTrackRepository trackRepository;
    private UserRepository userRepository;
    private AdminRepository adminRepository;
    private MusicUploadServiceImpl service;

    @BeforeEach
    void setUp() {
        MusicStorageProperties storageProperties = new MusicStorageProperties();
        storageProperties.setRoot(tempDir.resolve("music").toString());
        storage = new LocalMusicStorageService(storageProperties);
        storage.initialize();
        trackRepository = mock(MusicTrackRepository.class);
        userRepository = mock(UserRepository.class);
        adminRepository = mock(AdminRepository.class);
        service = new MusicUploadServiceImpl(new MusicFileValidator(new MusicUploadProperties()),
                storage, trackRepository, userRepository, adminRepository);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin()));
    }

    @Test
    void validUploadPersistsDraftWithAuthenticatedAdminAndPrivateFiles() throws Exception {
        when(trackRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            MusicTrack track = invocation.getArgument(0);
            track.setId(91L);
            track.onCreate();
            return track;
        });

        var response = service.upload("admin@example.com", metadata(), mp3(), png());

        ArgumentCaptor<MusicTrack> captor = ArgumentCaptor.forClass(MusicTrack.class);
        verify(trackRepository).saveAndFlush(captor.capture());
        MusicTrack saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(MusicTrackStatus.DRAFT);
        assertThat(saved.getUploadedBy().getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getAudioStorageKey()).matches("[0-9a-f-]{36}\\.mp3");
        assertThat(saved.getCoverStorageKey()).matches("[0-9a-f-]{36}\\.png");
        assertThat(storage.draftAudioExists(saved.getAudioStorageKey())).isTrue();
        assertThat(storage.draftCoverExists(saved.getCoverStorageKey())).isTrue();
        assertThatThrownBy(() -> storage.getAudio(saved.getAudioStorageKey()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
        when(trackRepository.findByIdAndStatus(91L, MusicTrackStatus.PUBLISHED)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new MusicCatalogServiceImpl(trackRepository).getPublishedTrack(91L))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(response.getStatus()).isEqualTo(MusicTrackStatus.DRAFT);
        assertThat(response.toString()).doesNotContain(tempDir.toString(), "storageKey", "/media/music/");
    }

    @Test
    void optionalCoverRemainsNull() {
        when(trackRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.upload("admin@example.com", metadata(), mp3(), null);
        ArgumentCaptor<MusicTrack> captor = ArgumentCaptor.forClass(MusicTrack.class);
        verify(trackRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getCoverStorageKey()).isNull();
    }

    @Test
    void communityUploadUsesSameValidatedPrivatePipelineAndServerControlledFields() {
        when(trackRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            MusicTrack track = invocation.getArgument(0);
            track.setId(92L); track.onCreate(); return track;
        });
        UserMusicTrackUploadRequest request = new UserMusicTrackUploadRequest();
        request.setTitle("My original"); request.setArtistName("Community artist");
        request.setLanguage(LanguageCode.MR); request.setMood(MusicMood.FOCUS);
        request.setOriginalWorkConfirmed(true); request.setRightsConfirmed(true);
        MusicTrack saved = service.uploadCommunity(user(), request, mp3(), null);
        assertThat(saved.getStatus()).isEqualTo(MusicTrackStatus.PENDING_REVIEW);
        assertThat(saved.getSource()).isEqualTo(MusicTrackSource.COMMUNITY);
        assertThat(saved.getOriginalWorkConfirmed()).isTrue();
        assertThat(saved.getRightsConfirmed()).isTrue();
        assertThat(saved.getFeatured()).isFalse();
        assertThat(saved.getSortOrder()).isZero();
        assertThat(storage.draftAudioExists(saved.getAudioStorageKey())).isTrue();
        assertThat(storage.publicAudioExists(saved.getAudioStorageKey())).isFalse();
    }

    @Test
    void databaseFailureDeletesPromotedAudioAndCoverAndLeavesNoStagingFiles() throws Exception {
        when(trackRepository.saveAndFlush(any())).thenThrow(new RuntimeException("database unavailable at C:\\secret"));
        assertThatThrownBy(() -> service.upload("admin@example.com", metadata(), mp3(), png()))
                .isInstanceOf(MusicStorageException.class).hasMessage("MUSIC_STORAGE_ERROR");

        ArgumentCaptor<MusicTrack> captor = ArgumentCaptor.forClass(MusicTrack.class);
        verify(trackRepository).saveAndFlush(captor.capture());
        assertThat(storage.draftAudioExists(captor.getValue().getAudioStorageKey())).isFalse();
        assertThat(storage.draftCoverExists(captor.getValue().getCoverStorageKey())).isFalse();
        try (var staged = Files.list(tempDir.resolve("music/.staging"))) {
            assertThat(staged).isEmpty();
        }
    }

    @Test
    void audioStorageFailureDoesNotAttemptPersistence() throws Exception {
        MusicStorageService failedStorage = mock(MusicStorageService.class);
        when(failedStorage.stageAudio(any(), eq("mp3"))).thenThrow(new MusicStorageException());
        MusicUploadServiceImpl failedService = new MusicUploadServiceImpl(
                new MusicFileValidator(new MusicUploadProperties()), failedStorage, trackRepository, userRepository, adminRepository);

        assertThatThrownBy(() -> failedService.upload("admin@example.com", metadata(), mp3(), png()))
                .isInstanceOf(MusicStorageException.class);
        verify(trackRepository, never()).saveAndFlush(any());
        verify(failedStorage, never()).stageCover(any(), any());
    }

    @Test
    void coverPromotionFailureCompensatesAudioAndDoesNotPersist() throws Exception {
        MusicStorageService failedStorage = mock(MusicStorageService.class);
        var stagedAudio = new MusicStorageService.StagedMusicFile("audio.stage", "audio.mp3");
        var stagedCover = new MusicStorageService.StagedMusicFile("cover.stage", "cover.png");
        when(failedStorage.stageAudio(any(), eq("mp3"))).thenReturn(stagedAudio);
        when(failedStorage.stageCover(any(), eq("png"))).thenReturn(stagedCover);
        doThrow(new MusicStorageException()).when(failedStorage).promoteDraftCover(stagedCover);
        MusicUploadServiceImpl failedService = new MusicUploadServiceImpl(
                new MusicFileValidator(new MusicUploadProperties()), failedStorage, trackRepository, userRepository, adminRepository);

        assertThatThrownBy(() -> failedService.upload("admin@example.com", metadata(), mp3(), png()))
                .isInstanceOf(MusicStorageException.class);
        verify(failedStorage).deleteDraftAudio("audio.mp3");
        verify(failedStorage).discardStaged(stagedCover);
        verify(trackRepository, never()).saveAndFlush(any());
    }

    private MusicTrackUploadRequest metadata() {
        MusicTrackUploadRequest request = new MusicTrackUploadRequest();
        request.setTitle("Safe Song");
        request.setArtistName("Artist");
        request.setLanguage(LanguageCode.HI);
        request.setMood(MusicMood.CALM);
        request.setGenre("Acoustic");
        request.setDescription("A draft upload");
        request.setFeatured(true);
        request.setSortOrder(2);
        return request;
    }

    private User admin() {
        User user = User.builder().email("admin@example.com").fullName("Admin")
                .password("encoded").role(Role.ADMIN).build();
        user.setId(5L);
        return user;
    }

    private User user() {
        User user = User.builder().email("user@example.com").fullName("User")
                .password("encoded").role(Role.USER).build();
        user.setId(6L);
        return user;
    }

    private MockMultipartFile mp3() {
        return new MockMultipartFile("audio", "original.mp3", "audio/mpeg",
                new byte[]{'I', 'D', '3', 4, 0, 0, 0, 0});
    }

    private MockMultipartFile png() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return new MockMultipartFile("cover", "original.png", "image/png", output.toByteArray());
    }
}
