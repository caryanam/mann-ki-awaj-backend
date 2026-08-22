package com.mka.service;

import com.mka.config.MusicUploadProperties;
import com.mka.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MusicFileValidator {
    private static final Set<String> MP3_MIME_TYPES = Set.of("audio/mpeg", "audio/mp3");
    private final MusicUploadProperties properties;

    public ValidatedFile validateAudio(MultipartFile file) {
        requirePresent(file, "INVALID_AUDIO_FILE");
        if (file.getSize() > properties.getMaxAudioSize().toBytes()) {
            throw new BadRequestException("AUDIO_FILE_TOO_LARGE");
        }
        String extension = safeExtension(file.getOriginalFilename(), "INVALID_AUDIO_FILE");
        if (!extension.equals("mp3")) {
            throw new BadRequestException("UNSUPPORTED_AUDIO_FORMAT");
        }
        String declaredMime = normalizeMime(file.getContentType());
        if (!MP3_MIME_TYPES.contains(declaredMime)) {
            throw new BadRequestException("INVALID_AUDIO_FILE");
        }
        try (InputStream input = new BufferedInputStream(file.getInputStream())) {
            byte[] signature = input.readNBytes(4);
            boolean id3 = signature.length >= 3 && signature[0] == 'I' && signature[1] == 'D' && signature[2] == '3';
            boolean frame = signature.length >= 2
                    && (signature[0] & 0xFF) == 0xFF
                    && (signature[1] & 0xE0) == 0xE0
                    && (signature[1] & 0x18) != 0x08
                    && (signature[1] & 0x06) != 0;
            if (!id3 && !frame) {
                throw new BadRequestException("INVALID_AUDIO_FILE");
            }
        } catch (IOException ex) {
            throw new BadRequestException("INVALID_AUDIO_FILE");
        }
        return new ValidatedFile("mp3", "audio/mpeg", file.getSize());
    }

    public ValidatedFile validateCover(MultipartFile file) {
        requirePresent(file, "INVALID_COVER_FILE");
        if (file.getSize() > properties.getMaxCoverSize().toBytes()) {
            throw new BadRequestException("COVER_FILE_TOO_LARGE");
        }
        String extension = safeExtension(file.getOriginalFilename(), "INVALID_COVER_FILE");
        String mime;
        if (extension.equals("jpg") || extension.equals("jpeg")) mime = "image/jpeg";
        else if (extension.equals("png")) mime = "image/png";
        else throw new BadRequestException("INVALID_COVER_FILE");

        if (!mime.equals(normalizeMime(file.getContentType()))) {
            throw new BadRequestException("INVALID_COVER_FILE");
        }
        try (InputStream input = new BufferedInputStream(file.getInputStream())) {
            input.mark(16);
            byte[] signature = input.readNBytes(8);
            input.reset();
            boolean jpeg = mime.equals("image/jpeg") && signature.length >= 3
                    && (signature[0] & 0xFF) == 0xFF && (signature[1] & 0xFF) == 0xD8
                    && (signature[2] & 0xFF) == 0xFF;
            boolean png = mime.equals("image/png") && signature.length == 8
                    && (signature[0] & 0xFF) == 0x89 && signature[1] == 'P' && signature[2] == 'N'
                    && signature[3] == 'G' && signature[4] == 0x0D && signature[5] == 0x0A
                    && signature[6] == 0x1A && signature[7] == 0x0A;
            if ((!jpeg && !png) || ImageIO.read(input) == null) {
                throw new BadRequestException("INVALID_COVER_FILE");
            }
        } catch (IOException ex) {
            throw new BadRequestException("INVALID_COVER_FILE");
        }
        return new ValidatedFile(extension.equals("jpeg") ? "jpg" : extension, mime, file.getSize());
    }

    private void requirePresent(MultipartFile file, String code) {
        if (file == null || file.isEmpty()) throw new BadRequestException(code);
    }

    private String safeExtension(String filename, String code) {
        if (filename == null || filename.isBlank() || filename.contains("/") || filename.contains("\\")) {
            throw new BadRequestException(code);
        }
        int dot = filename.lastIndexOf('.');
        if (dot <= 0 || dot == filename.length() - 1) throw new BadRequestException(code);
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeMime(String mime) {
        if (mime == null) return "";
        int semicolon = mime.indexOf(';');
        return (semicolon >= 0 ? mime.substring(0, semicolon) : mime).trim().toLowerCase(Locale.ROOT);
    }

    public record ValidatedFile(String extension, String mimeType, long size) {}
}
