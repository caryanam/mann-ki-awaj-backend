package com.mka.service.impl;

import com.mka.entity.MusicTrack;
import com.mka.enums.LanguageCode;
import com.mka.enums.MusicMood;
import com.mka.enums.MusicTrackStatus;
import com.mka.exception.ResourceNotFoundException;
import com.mka.repository.MusicTrackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MusicCatalogServiceImplTest {

    private MusicTrackRepository repository;
    private MusicCatalogServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(MusicTrackRepository.class);
        service = new MusicCatalogServiceImpl(repository);
    }

    @Test
    void mapsStorageKeysToPlayerCompatiblePublicUrlsWithoutPrivateFields() {
        MusicTrack track = publishedTrack();
        when(repository.findByIdAndStatus(7L, MusicTrackStatus.PUBLISHED)).thenReturn(Optional.of(track));

        var response = service.getPublishedTrack(7L);

        assertThat(response.getArtist()).isEqualTo("Artist");
        assertThat(response.getAudioUrl()).isEqualTo("/media/music/audio/abc.mp3");
        assertThat(response.getCoverUrl()).isEqualTo("/media/music/covers/abc.webp");
        assertThat(response.toString()).doesNotContain("C:\\", "/var/", "storageKey", "email");
    }

    @Test
    void returnsNullCoverUrlWhenNoCoverKeyExists() {
        MusicTrack track = publishedTrack();
        track.setCoverStorageKey(null);
        when(repository.findByIdAndStatus(7L, MusicTrackStatus.PUBLISHED)).thenReturn(Optional.of(track));
        assertThat(service.getPublishedTrack(7L).getCoverUrl()).isNull();
    }

    @Test
    void missingOrNonPublishedTrackIsReportedAsNotFound() {
        when(repository.findByIdAndStatus(99L, MusicTrackStatus.PUBLISHED)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getPublishedTrack(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository).findByIdAndStatus(99L, MusicTrackStatus.PUBLISHED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void combinedSearchAndFiltersUseOneSpecificationQuery() {
        var pageable = PageRequest.of(0, 20);
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(publishedTrack()), pageable, 1));

        var result = service.getPublishedTracks("LOVE", LanguageCode.MR, MusicMood.CALM,
                "Lo-Fi", true, pageable);

        assertThat(result.getContent()).hasSize(1);
        ArgumentCaptor<Specification<MusicTrack>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(repository).findAll(captor.capture(), eq(pageable));
        assertThat(captor.getValue()).isNotNull();
    }

    private MusicTrack publishedTrack() {
        MusicTrack track = MusicTrack.builder()
                .title("Love Song")
                .artistName("Artist")
                .language(LanguageCode.MR)
                .mood(MusicMood.CALM)
                .genre("Lo-fi")
                .description("Description")
                .audioStorageKey("abc.mp3")
                .coverStorageKey("abc.webp")
                .durationSeconds(238)
                .mimeType("audio/mpeg")
                .fileSizeBytes(4096L)
                .status(MusicTrackStatus.PUBLISHED)
                .featured(true)
                .sortOrder(0)
                .publishedAt(LocalDateTime.of(2026, 8, 22, 12, 0))
                .build();
        track.setId(7L);
        return track;
    }
}
