package com.mka.controller;

import com.mka.dto.request.UserMusicTrackUpdateRequest;
import com.mka.dto.request.UserMusicTrackUploadRequest;
import com.mka.dto.response.ApiResponse;
import com.mka.dto.response.UserMusicTrackResponse;
import com.mka.dto.response.MusicPageResponse;
import com.mka.enums.MusicTrackStatus;
import com.mka.service.MusicMediaResponseFactory;
import com.mka.service.UserMusicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.security.Principal;

@RestController
@RequestMapping("/api/music/my-tracks")
@RequiredArgsConstructor
public class UserMusicController {
    private static final int MAX_PAGE_SIZE = 50;
    private static final CacheControl PRIVATE_CACHE = CacheControl.noStore().cachePrivate();
    private final UserMusicService service;

    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserMusicTrackResponse>> upload(
            @Valid @ModelAttribute UserMusicTrackUploadRequest metadata,
            @RequestParam("audio") MultipartFile audio,
            @RequestParam(value = "cover", required = false) MultipartFile cover,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Track uploaded and pending review", service.upload(principal.getName(), metadata, audio, cover)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<MusicPageResponse<UserMusicTrackResponse>>> list(
            @RequestParam(required = false) MusicTrackStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<UserMusicTrackResponse> tracks = service.list(
                principal.getName(), status, PageRequest.of(safePage, safeSize,
                        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return ResponseEntity.ok(ApiResponse.success("My music tracks retrieved", MusicPageResponse.from(tracks)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserMusicTrackResponse>> get(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("My music track retrieved", service.get(principal.getName(), id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserMusicTrackResponse>> update(@PathVariable Long id,
            @Valid @RequestBody UserMusicTrackUpdateRequest request, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Music metadata updated",
                service.update(principal.getName(), id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        service.delete(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/audio")
    public ResponseEntity<StreamingResponseBody> audio(@PathVariable Long id,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String range,
            Principal principal) {
        return MusicMediaResponseFactory.audio(service.getPrivateAudio(principal.getName(), id), range, PRIVATE_CACHE);
    }

    @RequestMapping(value = "/{id}/audio", method = RequestMethod.HEAD)
    public ResponseEntity<Void> headAudio(@PathVariable Long id, Principal principal) {
        return MusicMediaResponseFactory.headAudio(service.getPrivateAudio(principal.getName(), id), PRIVATE_CACHE);
    }

    @GetMapping("/{id}/cover")
    public ResponseEntity<Resource> cover(@PathVariable Long id, Principal principal) {
        return MusicMediaResponseFactory.cover(service.getPrivateCover(principal.getName(), id), PRIVATE_CACHE);
    }

    @RequestMapping(value = "/{id}/cover", method = RequestMethod.HEAD)
    public ResponseEntity<Void> headCover(@PathVariable Long id, Principal principal) {
        return MusicMediaResponseFactory.headCover(service.getPrivateCover(principal.getName(), id), PRIVATE_CACHE);
    }
}
