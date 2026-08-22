package com.mka.service.impl;

import com.mka.dto.response.MusicTrackResponse;
import com.mka.entity.MusicTrack;
import com.mka.enums.LanguageCode;
import com.mka.enums.MusicMood;
import com.mka.enums.MusicTrackStatus;
import com.mka.exception.ResourceNotFoundException;
import com.mka.repository.MusicTrackRepository;
import com.mka.service.MusicCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MusicCatalogServiceImpl implements MusicCatalogService {

    private final MusicTrackRepository repository;

    @Override
    public Page<MusicTrackResponse> getPublishedTracks(String query, LanguageCode language, MusicMood mood,
                                                       String genre, Boolean featured, Pageable pageable) {
        Specification<MusicTrack> specification = published();
        if (query != null && !query.isBlank()) {
            String pattern = "%" + query.trim().toLowerCase() + "%";
            specification = specification.and((root, ignored, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("artistName")), pattern)));
        }
        if (language != null) {
            specification = specification.and((root, ignored, cb) -> cb.equal(root.get("language"), language));
        }
        if (mood != null) {
            specification = specification.and((root, ignored, cb) -> cb.equal(root.get("mood"), mood));
        }
        if (genre != null && !genre.isBlank()) {
            specification = specification.and((root, ignored, cb) ->
                    cb.equal(cb.lower(root.get("genre")), genre.trim().toLowerCase()));
        }
        if (featured != null) {
            specification = specification.and((root, ignored, cb) -> cb.equal(root.get("featured"), featured));
        }
        return repository.findAll(specification, pageable).map(this::toResponse);
    }

    @Override
    public MusicTrackResponse getPublishedTrack(Long id) {
        MusicTrack track = repository.findByIdAndStatus(id, MusicTrackStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Music track not found"));
        return toResponse(track);
    }

    private Specification<MusicTrack> published() {
        return (root, ignored, cb) -> cb.equal(root.get("status"), MusicTrackStatus.PUBLISHED);
    }

    private MusicTrackResponse toResponse(MusicTrack track) {
        return MusicTrackResponse.builder()
                .id(track.getId())
                .title(track.getTitle())
                .artist(track.getArtistName())
                .language(track.getLanguage())
                .mood(track.getMood())
                .genre(track.getGenre())
                .description(track.getDescription())
                .coverUrl(track.getCoverStorageKey() == null ? null
                        : "/media/music/covers/" + track.getCoverStorageKey())
                .audioUrl("/media/music/audio/" + track.getAudioStorageKey())
                .durationSeconds(track.getDurationSeconds())
                .featured(track.getFeatured())
                .publishedAt(track.getPublishedAt())
                .build();
    }
}
