package com.mka.controller;

import com.mka.dto.request.MusicTrackUploadRequest;
import com.mka.dto.response.AdminMusicTrackResponse;
import com.mka.dto.response.ApiResponse;
import com.mka.service.MusicUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/admin/music/tracks")
@RequiredArgsConstructor
public class AdminMusicUploadController {

    private final MusicUploadService musicUploadService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AdminMusicTrackResponse>> upload(
            @Valid @ModelAttribute MusicTrackUploadRequest metadata,
            @RequestParam("audio") MultipartFile audio,
            @RequestParam(value = "cover", required = false) MultipartFile cover,
            Principal principal) {
        AdminMusicTrackResponse track = musicUploadService.upload(principal.getName(), metadata, audio, cover);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Music track uploaded as draft", track));
    }
}
