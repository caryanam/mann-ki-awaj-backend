package com.mka.controller;

import com.mka.dto.response.ApiResponse;
import com.mka.dto.response.MusicTrackResponse;
import com.mka.enums.LanguageCode;
import com.mka.enums.MusicMood;
import com.mka.service.MusicCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/music/tracks")
@Tag(name = "Music Catalog", description = "Public published music catalog APIs")
@RequiredArgsConstructor
public class MusicCatalogController {

    private static final int MAX_PAGE_SIZE = 50;
    private static final Sort DEFAULT_SORT = Sort.by(
            Sort.Order.desc("featured"),
            Sort.Order.asc("sortOrder"),
            Sort.Order.desc("publishedAt"),
            Sort.Order.desc("id"));

    private final MusicCatalogService musicCatalogService;

    @GetMapping
    @Operation(summary = "Browse published music with optional search and filters")
    public ResponseEntity<ApiResponse<Page<MusicTrackResponse>>> getTracks(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) LanguageCode language,
            @RequestParam(required = false) MusicMood mood,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<MusicTrackResponse> tracks = musicCatalogService.getPublishedTracks(
                query, language, mood, genre, featured, PageRequest.of(safePage, safeSize, DEFAULT_SORT));
        return ResponseEntity.ok(ApiResponse.success("Published music catalog retrieved", tracks));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a published music track")
    public ResponseEntity<ApiResponse<MusicTrackResponse>> getTrack(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Published music track retrieved", musicCatalogService.getPublishedTrack(id)));
    }
}
