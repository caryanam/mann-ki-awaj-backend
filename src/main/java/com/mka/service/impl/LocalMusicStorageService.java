package com.mka.service.impl;

import com.mka.config.MusicStorageProperties;
import com.mka.service.MusicStorageService;
import com.mka.exception.MusicStorageException;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLDecoder;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class LocalMusicStorageService implements MusicStorageService {

    private static final Map<String, String> AUDIO_TYPES = Map.of(
            "mp3", "audio/mpeg",
            "m4a", "audio/mp4",
            "aac", "audio/aac"
    );
    private static final Map<String, String> COVER_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp"
    );

    private final Path root;
    private final Path audioRoot;
    private final Path coverRoot;
    private final Path privateAudioRoot;
    private final Path privateCoverRoot;
    private final Path stagingRoot;
    private final Path quarantineRoot;

    public LocalMusicStorageService(MusicStorageProperties properties) {
        if (properties.getRoot() == null || properties.getRoot().isBlank()) {
            throw new IllegalStateException("music.storage.root must not be blank");
        }
        this.root = Paths.get(properties.getRoot()).toAbsolutePath().normalize();
        this.audioRoot = resolveConfiguredDirectory(root, properties.getAudioDir(), "audio-dir");
        this.coverRoot = resolveConfiguredDirectory(root, properties.getCoverDir(), "cover-dir");
        this.privateAudioRoot = root.resolve("private/audio").normalize();
        this.privateCoverRoot = root.resolve("private/covers").normalize();
        this.stagingRoot = root.resolve(".staging").normalize();
        this.quarantineRoot = root.resolve(".quarantine").normalize();
    }

    @PostConstruct
    public void initialize() {
        try {
            Files.createDirectories(audioRoot);
            Files.createDirectories(coverRoot);
            Files.createDirectories(privateAudioRoot);
            Files.createDirectories(privateCoverRoot);
            Files.createDirectories(stagingRoot);
            Files.createDirectories(quarantineRoot);
            if (!Files.isDirectory(audioRoot) || !Files.isWritable(audioRoot)
                    || !Files.isDirectory(coverRoot) || !Files.isWritable(coverRoot)
                    || !Files.isDirectory(privateAudioRoot) || !Files.isWritable(privateAudioRoot)
                    || !Files.isDirectory(privateCoverRoot) || !Files.isWritable(privateCoverRoot)
                    || !Files.isDirectory(stagingRoot) || !Files.isWritable(stagingRoot)
                    || !Files.isDirectory(quarantineRoot) || !Files.isWritable(quarantineRoot)) {
                throw new IllegalStateException("Configured music storage directories are not writable");
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Could not initialize configured music storage", ex);
        }
    }

    @Override
    public StoredMusicResource getAudio(String filename) {
        return resolveExistingFile(audioRoot, filename, AUDIO_TYPES);
    }

    @Override
    public StoredMusicResource getCover(String filename) {
        return resolveExistingFile(coverRoot, filename, COVER_TYPES);
    }

    @Override
    public StoredMusicResource getPrivateAudio(String storageKey) {
        return resolveExistingFile(privateAudioRoot, storageKey, AUDIO_TYPES);
    }

    @Override
    public StoredMusicResource getPrivateCover(String storageKey) {
        return resolveExistingFile(privateCoverRoot, storageKey, COVER_TYPES);
    }

    @Override
    public StagedMusicFile stageAudio(InputStream source, String extension) {
        return stage(source, extension);
    }

    @Override
    public StagedMusicFile stageCover(InputStream source, String extension) {
        return stage(source, extension);
    }

    @Override
    public void promoteDraftAudio(StagedMusicFile staged) {
        promote(staged, privateAudioRoot);
    }

    @Override
    public void promoteDraftCover(StagedMusicFile staged) {
        promote(staged, privateCoverRoot);
    }

    @Override
    public void discardStaged(StagedMusicFile staged) {
        if (staged == null) return;
        deleteConstrained(stagingRoot, staged.token());
    }

    @Override
    public void deleteDraftAudio(String storageKey) {
        deleteConstrained(privateAudioRoot, storageKey);
    }

    @Override
    public void deleteDraftCover(String storageKey) {
        if (storageKey != null) deleteConstrained(privateCoverRoot, storageKey);
    }

    @Override
    public boolean draftAudioExists(String storageKey) {
        return existsConstrained(privateAudioRoot, storageKey);
    }

    @Override
    public boolean draftCoverExists(String storageKey) {
        return storageKey != null && existsConstrained(privateCoverRoot, storageKey);
    }

    @Override
    public boolean publicAudioExists(String storageKey) {
        return existsConstrained(audioRoot, storageKey);
    }

    @Override
    public boolean publicCoverExists(String storageKey) {
        return storageKey != null && existsConstrained(coverRoot, storageKey);
    }

    @Override
    public void publishAudio(String storageKey) {
        copyConstrained(privateAudioRoot, audioRoot, storageKey);
    }

    @Override
    public void publishCover(String storageKey) {
        if (storageKey != null) copyConstrained(privateCoverRoot, coverRoot, storageKey);
    }

    @Override
    public void unpublishAudio(String storageKey) {
        deleteConstrained(audioRoot, storageKey);
    }

    @Override
    public void unpublishCover(String storageKey) {
        if (storageKey != null) deleteConstrained(coverRoot, storageKey);
    }

    @Override
    public QuarantinedMusicFile quarantinePrivateAudio(String storageKey) {
        return quarantine(privateAudioRoot, storageKey, MusicFileArea.PRIVATE_AUDIO);
    }

    @Override
    public QuarantinedMusicFile quarantinePrivateCover(String storageKey) {
        return storageKey == null ? null : quarantine(privateCoverRoot, storageKey, MusicFileArea.PRIVATE_COVER);
    }

    @Override
    public QuarantinedMusicFile quarantinePublicAudio(String storageKey) {
        return quarantine(audioRoot, storageKey, MusicFileArea.PUBLIC_AUDIO);
    }

    @Override
    public QuarantinedMusicFile quarantinePublicCover(String storageKey) {
        return storageKey == null ? null : quarantine(coverRoot, storageKey, MusicFileArea.PUBLIC_COVER);
    }

    @Override
    public void restoreQuarantined(QuarantinedMusicFile quarantined) {
        if (quarantined == null) return;
        Path source = safeChild(quarantineRoot, quarantined.token());
        Path target = safeChild(rootFor(quarantined.area()), quarantined.storageKey());
        move(source, target, true);
    }

    @Override
    public void purgeQuarantined(QuarantinedMusicFile quarantined) {
        if (quarantined != null) deleteConstrained(quarantineRoot, quarantined.token());
    }

    private void copyConstrained(Path sourceRoot, Path destinationRoot, String key) {
        Path source = safeChild(sourceRoot, key);
        Path target = safeChild(destinationRoot, key);
        try {
            if (!Files.isRegularFile(source) || !Files.isReadable(source)) throw new MusicStorageException();
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (MusicStorageException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MusicStorageException(ex);
        }
    }

    private QuarantinedMusicFile quarantine(Path sourceRoot, String key, MusicFileArea area) {
        Path source = safeChild(sourceRoot, key);
        if (!Files.exists(source)) return null;
        String token = UUID.randomUUID() + ".quarantine";
        Path target = safeChild(quarantineRoot, token);
        move(source, target, false);
        return new QuarantinedMusicFile(token, key, area);
    }

    private Path rootFor(MusicFileArea area) {
        return switch (area) {
            case PRIVATE_AUDIO -> privateAudioRoot;
            case PRIVATE_COVER -> privateCoverRoot;
            case PUBLIC_AUDIO -> audioRoot;
            case PUBLIC_COVER -> coverRoot;
        };
    }

    private void move(Path source, Path target, boolean replaceExisting) {
        try {
            if (!Files.isRegularFile(source)) throw new MusicStorageException();
            if (replaceExisting) {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                return;
            }
            try {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(source, target);
            }
        } catch (MusicStorageException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MusicStorageException(ex);
        }
    }

    private StagedMusicFile stage(InputStream source, String extension) {
        if (source == null || extension == null || !extension.matches("[a-z0-9]{2,5}")) {
            throw new MusicStorageException();
        }
        String storageKey = UUID.randomUUID() + "." + extension;
        String token = UUID.randomUUID() + ".stage";
        Path target = stagingRoot.resolve(token).normalize();
        try (source) {
            Files.copy(source, target);
            return new StagedMusicFile(token, storageKey);
        } catch (Exception ex) {
            try { Files.deleteIfExists(target); } catch (Exception ignored) { }
            throw new MusicStorageException(ex);
        }
    }

    private void promote(StagedMusicFile staged, Path destinationRoot) {
        if (staged == null) throw new MusicStorageException();
        Path source = safeChild(stagingRoot, staged.token());
        Path target = safeChild(destinationRoot, staged.storageKey());
        try {
            try {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(source, target);
            }
        } catch (Exception ex) {
            throw new MusicStorageException(ex);
        }
    }

    private void deleteConstrained(Path expectedRoot, String key) {
        try {
            Files.deleteIfExists(safeChild(expectedRoot, key));
        } catch (Exception ex) {
            throw new MusicStorageException(ex);
        }
    }

    private boolean existsConstrained(Path expectedRoot, String key) {
        try {
            return Files.isRegularFile(safeChild(expectedRoot, key));
        } catch (Exception ex) {
            return false;
        }
    }

    private Path safeChild(Path expectedRoot, String key) {
        if (key == null || key.isBlank() || key.contains("/") || key.contains("\\")) {
            throw new MusicStorageException();
        }
        Path child = expectedRoot.resolve(key).normalize();
        if (!child.startsWith(expectedRoot) || child.equals(expectedRoot)) throw new MusicStorageException();
        return child;
    }

    private StoredMusicResource resolveExistingFile(Path expectedRoot, String filename,
                                                     Map<String, String> supportedTypes) {
        String decoded = decodeFilename(filename);
        if (decoded.isBlank() || decoded.equals(".") || decoded.equals("..")
                || decoded.contains("/") || decoded.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid media filename");
        }

        try {
            Path requested = Paths.get(decoded);
            if (requested.isAbsolute() || requested.getNameCount() != 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid media filename");
            }

            String extension = extensionOf(decoded);
            String contentType = supportedTypes.get(extension);
            if (contentType == null) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type");
            }

            Path resolved = expectedRoot.resolve(requested).normalize();
            if (!resolved.startsWith(expectedRoot)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid media filename");
            }
            if (!Files.isRegularFile(resolved) || !Files.isReadable(resolved)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found");
            }
            return new StoredMusicResource(new FileSystemResource(resolved), contentType, Files.size(resolved));
        } catch (InvalidPathException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid media filename");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found");
        }
    }

    private static Path resolveConfiguredDirectory(Path root, String directory, String propertyName) {
        if (directory == null || directory.isBlank()) {
            throw new IllegalStateException("music.storage." + propertyName + " must not be blank");
        }
        Path configured = Paths.get(directory);
        if (configured.isAbsolute()) {
            throw new IllegalStateException("music.storage." + propertyName + " must be relative to music.storage.root");
        }
        Path resolved = root.resolve(configured).normalize();
        if (!resolved.startsWith(root) || resolved.equals(root)) {
            throw new IllegalStateException("music.storage." + propertyName + " escapes music.storage.root");
        }
        return resolved;
    }

    private static String decodeFilename(String filename) {
        if (filename == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid media filename");
        }
        try {
            String decoded = filename;
            for (int i = 0; i < 3; i++) {
                String next = URLDecoder.decode(decoded, StandardCharsets.UTF_8);
                if (next.equals(decoded)) {
                    return next;
                }
                decoded = next;
            }
            if (decoded.contains("%")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid media filename");
            }
            return decoded;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid media filename");
        }
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
