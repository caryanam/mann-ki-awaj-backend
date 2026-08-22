package com.mka.service;

import com.mka.service.MusicStorageService.StoredMusicResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.StandardOpenOption;
import java.util.List;

public final class MusicMediaResponseFactory {

    private MusicMediaResponseFactory() { }

    public static ResponseEntity<StreamingResponseBody> audio(StoredMusicResource media,
                                                               String rangeHeader,
                                                               CacheControl cacheControl) {
        HttpHeaders headers = commonHeaders(media, cacheControl);
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        if (rangeHeader == null || rangeHeader.isBlank()) {
            headers.setContentLength(media.contentLength());
            return new ResponseEntity<>(stream(media.resource(), 0, media.contentLength()), headers, HttpStatus.OK);
        }
        try {
            List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
            if (ranges.size() != 1) return rangeNotSatisfiable(media.contentLength());
            HttpRange range = ranges.get(0);
            long start = range.getRangeStart(media.contentLength());
            long end = range.getRangeEnd(media.contentLength());
            long count = end - start + 1;
            headers.setContentLength(count);
            headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + media.contentLength());
            return new ResponseEntity<>(stream(media.resource(), start, count), headers, HttpStatus.PARTIAL_CONTENT);
        } catch (IllegalArgumentException ex) {
            return rangeNotSatisfiable(media.contentLength());
        }
    }

    public static ResponseEntity<Void> headAudio(StoredMusicResource media, CacheControl cacheControl) {
        HttpHeaders headers = commonHeaders(media, cacheControl);
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.setContentLength(media.contentLength());
        return new ResponseEntity<>(headers, HttpStatus.OK);
    }

    public static ResponseEntity<Resource> cover(StoredMusicResource media, CacheControl cacheControl) {
        HttpHeaders headers = commonHeaders(media, cacheControl);
        headers.setContentLength(media.contentLength());
        return new ResponseEntity<>(media.resource(), headers, HttpStatus.OK);
    }

    public static ResponseEntity<Void> headCover(StoredMusicResource media, CacheControl cacheControl) {
        HttpHeaders headers = commonHeaders(media, cacheControl);
        headers.setContentLength(media.contentLength());
        return new ResponseEntity<>(headers, HttpStatus.OK);
    }

    private static HttpHeaders commonHeaders(StoredMusicResource media, CacheControl cacheControl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(media.contentType()));
        headers.setCacheControl(cacheControl);
        return headers;
    }

    private static ResponseEntity<StreamingResponseBody> rangeNotSatisfiable(long length) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.set(HttpHeaders.CONTENT_RANGE, "bytes */" + length);
        return new ResponseEntity<>(headers, HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
    }

    private static StreamingResponseBody stream(Resource resource, long start, long count) {
        return output -> {
            try (FileChannel input = FileChannel.open(resource.getFile().toPath(), StandardOpenOption.READ);
                 WritableByteChannel destination = Channels.newChannel(output)) {
                long remaining = count;
                long position = start;
                while (remaining > 0) {
                    long transferred = input.transferTo(position, remaining, destination);
                    if (transferred <= 0) break;
                    position += transferred;
                    remaining -= transferred;
                }
            }
        };
    }
}
