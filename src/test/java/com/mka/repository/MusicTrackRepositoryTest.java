package com.mka.repository;

import com.mka.entity.MusicTrack;
import com.mka.entity.User;
import com.mka.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import com.mka.service.impl.MusicCatalogServiceImpl;
import com.mka.service.impl.AdminMusicManagementServiceImpl;
import com.mka.service.MusicStorageService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class MusicTrackRepositoryTest {

    @Autowired
    private MusicTrackRepository musicTrackRepository;

    @Autowired
    private UserRepository userRepository;

    private User uploader;

    @BeforeEach
    void setUp() {
        uploader = userRepository.saveAndFlush(User.builder()
                .fullName("Catalog Admin")
                .email("music-admin@example.test")
                .password("not-used-in-test")
                .role(Role.ADMIN)
                .build());
    }

    @Test
    void persistsTrackStatusAndLazyUploaderRelationship() {
        MusicTrack saved = musicTrackRepository.saveAndFlush(track("persist.mp3", MusicTrackStatus.PUBLISHED));
        MusicTrack loaded = musicTrackRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getStatus()).isEqualTo(MusicTrackStatus.PUBLISHED);
        assertThat(loaded.getUploadedBy().getId()).isEqualTo(uploader.getId());
        assertThat(loaded.getCreatedAt()).isNotNull();
    }

    @Test
    void enforcesUniqueAudioStorageKey() {
        musicTrackRepository.saveAndFlush(track("unique.mp3", MusicTrackStatus.PUBLISHED));
        assertThatThrownBy(() -> musicTrackRepository.saveAndFlush(track("unique.mp3", MusicTrackStatus.DRAFT)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsOnlyRequestedPublishedStatusById() {
        MusicTrack published = musicTrackRepository.saveAndFlush(track("public.mp3", MusicTrackStatus.PUBLISHED));
        MusicTrack draft = musicTrackRepository.saveAndFlush(track("draft.mp3", MusicTrackStatus.DRAFT));

        assertThat(musicTrackRepository.findByIdAndStatus(published.getId(), MusicTrackStatus.PUBLISHED)).isPresent();
        assertThat(musicTrackRepository.findByIdAndStatus(draft.getId(), MusicTrackStatus.PUBLISHED)).isEmpty();
    }

    @Test
    void specificationPaginationReturnsOnlyPublishedRows() {
        musicTrackRepository.save(track("one.mp3", MusicTrackStatus.PUBLISHED));
        musicTrackRepository.save(track("two.mp3", MusicTrackStatus.PUBLISHED));
        musicTrackRepository.save(track("hidden.mp3", MusicTrackStatus.UNPUBLISHED));
        musicTrackRepository.flush();

        var page = musicTrackRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("status"), MusicTrackStatus.PUBLISHED),
                PageRequest.of(0, 1));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void catalogServiceAppliesPublishedOnlySearchAndEveryFilterCaseInsensitively() {
        MusicTrack love = track("love.mp3", MusicTrackStatus.PUBLISHED);
        love.setTitle("Marathi Love Ballad");
        love.setArtistName("Asha Voice");
        love.setFeatured(true);
        musicTrackRepository.save(love);

        MusicTrack other = track("folk.mp3", MusicTrackStatus.PUBLISHED);
        other.setTitle("Village Song");
        other.setArtistName("Folk Ensemble");
        other.setLanguage(LanguageCode.HI);
        other.setMood(MusicMood.ENERGETIC);
        other.setGenre("Folk");
        musicTrackRepository.save(other);

        musicTrackRepository.save(track("draft-filter.mp3", MusicTrackStatus.DRAFT));
        musicTrackRepository.save(track("unpublished-filter.mp3", MusicTrackStatus.UNPUBLISHED));
        musicTrackRepository.save(track("deleted-filter.mp3", MusicTrackStatus.DELETED));
        musicTrackRepository.flush();

        MusicCatalogServiceImpl service = new MusicCatalogServiceImpl(musicTrackRepository);
        var page = PageRequest.of(0, 20);

        assertThat(service.getPublishedTracks(null, null, null, null, null, page).getTotalElements()).isEqualTo(2);
        assertThat(service.getPublishedTracks("LOVE", null, null, null, null, page).getContent())
                .extracting("title").containsExactly("Marathi Love Ballad");
        assertThat(service.getPublishedTracks("asha", null, null, null, null, page).getTotalElements()).isEqualTo(1);
        assertThat(service.getPublishedTracks(null, LanguageCode.MR, null, null, null, page).getTotalElements()).isEqualTo(1);
        assertThat(service.getPublishedTracks(null, null, MusicMood.CALM, null, null, page).getTotalElements()).isEqualTo(1);
        assertThat(service.getPublishedTracks(null, null, null, "lo-FI", null, page).getTotalElements()).isEqualTo(1);
        assertThat(service.getPublishedTracks(null, null, null, null, true, page).getTotalElements()).isEqualTo(1);
    }

    @Test
    void adminServiceListsAndFiltersEveryNonDeletedStatus() {
        MusicTrack draft = track("admin-draft.mp3", MusicTrackStatus.DRAFT);
        draft.setTitle("Quiet Draft");
        draft.setArtistName("Draft Singer");
        draft.setFeatured(true);
        musicTrackRepository.save(draft);

        MusicTrack published = track("admin-published.mp3", MusicTrackStatus.PUBLISHED);
        published.setTitle("Live Anthem");
        published.setArtistName("Public Band");
        published.setLanguage(LanguageCode.HI);
        published.setMood(MusicMood.ENERGETIC);
        published.setGenre("Folk");
        musicTrackRepository.save(published);

        MusicTrack unpublished = track("admin-unpublished.mp3", MusicTrackStatus.UNPUBLISHED);
        unpublished.setTitle("Archive Song");
        unpublished.setArtistName("Hidden Voice");
        unpublished.setLanguage(LanguageCode.EN);
        unpublished.setMood(MusicMood.FOCUS);
        unpublished.setGenre("Jazz");
        musicTrackRepository.save(unpublished);
        musicTrackRepository.save(track("admin-deleted.mp3", MusicTrackStatus.DELETED));
        musicTrackRepository.flush();

        AdminMusicManagementServiceImpl service = new AdminMusicManagementServiceImpl(
                musicTrackRepository, mock(MusicStorageService.class));
        var page = PageRequest.of(0, 20);
        assertThat(service.list(null, null, null, null, null, null, page).getTotalElements()).isEqualTo(3);
        assertThat(service.list("anthem", null, null, null, null, null, page).getTotalElements()).isEqualTo(1);
        assertThat(service.list("hidden voice", null, null, null, null, null, page).getTotalElements()).isEqualTo(1);
        assertThat(service.list(null, MusicTrackStatus.DRAFT, null, null, null, null, page).getTotalElements()).isEqualTo(1);
        assertThat(service.list(null, null, LanguageCode.HI, null, null, null, page).getTotalElements()).isEqualTo(1);
        assertThat(service.list(null, null, null, MusicMood.FOCUS, null, null, page).getTotalElements()).isEqualTo(1);
        assertThat(service.list(null, null, null, null, "fOlK", null, page).getTotalElements()).isEqualTo(1);
        assertThat(service.list(null, null, null, null, null, true, page).getTotalElements()).isEqualTo(1);
        assertThat(service.list(null, MusicTrackStatus.DELETED, null, null, null, null, page)).isEmpty();
    }

    private MusicTrack track(String audioKey, MusicTrackStatus status) {
        return MusicTrack.builder()
                .title(" Test Track ")
                .artistName(" Test Artist ")
                .language(LanguageCode.MR)
                .mood(MusicMood.CALM)
                .genre("Lo-fi")
                .description("Test-only metadata")
                .audioStorageKey(audioKey)
                .coverStorageKey(audioKey.replace(".mp3", ".webp"))
                .durationSeconds(120)
                .mimeType("audio/mpeg")
                .fileSizeBytes(1024L)
                .status(status)
                .featured(false)
                .sortOrder(0)
                .uploadedBy(uploader)
                .publishedAt(status == MusicTrackStatus.PUBLISHED ? LocalDateTime.now() : null)
                .build();
    }
}
