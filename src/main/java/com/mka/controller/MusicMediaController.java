package com.mka.controller;

import com.mka.service.MusicStorageService;
import com.mka.service.MusicStorageService.StoredMusicResource;
import com.mka.service.MusicMediaResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class MusicMediaController {

    private static final CacheControl MEDIA_CACHE = CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable();
    private final MusicStorageService storageService;

    @GetMapping("/media/music/audio/{filename:.+}")
    public ResponseEntity<StreamingResponseBody> getAudio(@PathVariable String filename,
            @org.springframework.web.bind.annotation.RequestHeader(value = HttpHeaders.RANGE,
                    required = false) String rangeHeader) {
        StoredMusicResource media = storageService.getAudio(filename);
        return MusicMediaResponseFactory.audio(media, rangeHeader, MEDIA_CACHE);
    }

    @RequestMapping(value = "/media/music/audio/{filename:.+}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> headAudio(@PathVariable String filename) {
        StoredMusicResource media = storageService.getAudio(filename);
        return MusicMediaResponseFactory.headAudio(media, MEDIA_CACHE);
    }

    @GetMapping("/media/music/covers/{filename:.+}")
    public ResponseEntity<Resource> getCover(@PathVariable String filename) {
        StoredMusicResource media = storageService.getCover(filename);
        return MusicMediaResponseFactory.cover(media, MEDIA_CACHE);
    }

    @RequestMapping(value = "/media/music/covers/{filename:.+}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> headCover(@PathVariable String filename) {
        StoredMusicResource media = storageService.getCover(filename);
        return MusicMediaResponseFactory.headCover(media, MEDIA_CACHE);
    }
}
