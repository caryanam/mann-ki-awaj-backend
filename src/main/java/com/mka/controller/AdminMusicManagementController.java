package com.mka.controller;

import com.mka.dto.request.MusicTrackUpdateRequest;
import com.mka.dto.request.MusicTrackRejectRequest;
import com.mka.dto.response.AdminMusicTrackResponse;
import com.mka.dto.response.ApiResponse;
import com.mka.enums.LanguageCode;
import com.mka.enums.MusicMood;
import com.mka.enums.MusicTrackStatus;
import com.mka.service.AdminMusicManagementService;
import com.mka.service.MusicMediaResponseFactory;
import com.mka.service.MusicStorageService.StoredMusicResource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/admin/music/tracks")
@RequiredArgsConstructor
public class AdminMusicManagementController {

    private static final int MAX_PAGE_SIZE = 50;
    private static final CacheControl PRIVATE_MEDIA_CACHE = CacheControl.noStore().cachePrivate();
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    private final AdminMusicManagementService service;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminMusicTrackResponse>>> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) MusicTrackStatus status,
            @RequestParam(required = false) LanguageCode language,
            @RequestParam(required = false) MusicMood mood,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<AdminMusicTrackResponse> tracks = service.list(query, status, language, mood, genre, featured,
                PageRequest.of(safePage, safeSize, DEFAULT_SORT));
        return ResponseEntity.ok(ApiResponse.success("Admin music tracks retrieved", tracks));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminMusicTrackResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Admin music track retrieved", service.get(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminMusicTrackResponse>> update(@PathVariable Long id,
                                                                        @Valid @RequestBody MusicTrackUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Music metadata updated", service.update(id, request)));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<AdminMusicTrackResponse>> publish(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Music track published", service.publish(id)));
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<ApiResponse<AdminMusicTrackResponse>> unpublish(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Music track unpublished", service.unpublish(id)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<AdminMusicTrackResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Community music track approved", service.approve(id)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<AdminMusicTrackResponse>> reject(@PathVariable Long id,
            @Valid @RequestBody MusicTrackRejectRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Community music track rejected",
                service.reject(id, request.getReason())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/audio")
    public ResponseEntity<StreamingResponseBody> audio(@PathVariable Long id,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {
        StoredMusicResource media = service.getPrivateAudio(id);
        return MusicMediaResponseFactory.audio(media, rangeHeader, PRIVATE_MEDIA_CACHE);
    }

    @RequestMapping(value = "/{id}/audio", method = RequestMethod.HEAD)
    public ResponseEntity<Void> headAudio(@PathVariable Long id) {
        return MusicMediaResponseFactory.headAudio(service.getPrivateAudio(id), PRIVATE_MEDIA_CACHE);
    }

    @GetMapping("/{id}/cover")
    public ResponseEntity<Resource> cover(@PathVariable Long id) {
        return MusicMediaResponseFactory.cover(service.getPrivateCover(id), PRIVATE_MEDIA_CACHE);
    }

    @RequestMapping(value = "/{id}/cover", method = RequestMethod.HEAD)
    public ResponseEntity<Void> headCover(@PathVariable Long id) {
        return MusicMediaResponseFactory.headCover(service.getPrivateCover(id), PRIVATE_MEDIA_CACHE);
    }
}
