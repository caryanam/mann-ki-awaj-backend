package com.mka.service;

import com.mka.config.MusicUploadProperties;
import com.mka.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MusicFileValidatorTest {

    private MusicUploadProperties properties;
    private MusicFileValidator validator;

    @BeforeEach
    void setUp() {
        properties = new MusicUploadProperties();
        validator = new MusicFileValidator(properties);
    }

    @Test
    void acceptsId3AndMpegFrameMp3Signatures() {
        assertThat(validator.validateAudio(audio("song.mp3", "audio/mpeg", id3())).mimeType())
                .isEqualTo("audio/mpeg");
        assertThat(validator.validateAudio(audio("song.mp3", "audio/mp3",
                new byte[]{(byte) 0xFF, (byte) 0xFB, 0x10, 0x00})).extension()).isEqualTo("mp3");
    }

    @Test
    void rejectsEmptyAudio() {
        assertCode(audio("song.mp3", "audio/mpeg", new byte[0]), "INVALID_AUDIO_FILE");
    }

    @Test
    void rejectsUnsupportedAudioExtension() {
        assertCode(audio("song.wav", "audio/wav", id3()), "UNSUPPORTED_AUDIO_FORMAT");
    }

    @Test
    void rejectsFakeMp3AndMimeMismatch() {
        assertCode(audio("song.mp3", "audio/mpeg", new byte[]{1, 2, 3, 4}), "INVALID_AUDIO_FILE");
        assertCode(audio("song.mp3", "application/octet-stream", id3()), "INVALID_AUDIO_FILE");
    }

    @Test
    void rejectsOversizedAudioBeforeReadingContent() {
        properties.setMaxAudioSize(DataSize.ofBytes(3));
        assertCode(audio("song.mp3", "audio/mpeg", id3()), "AUDIO_FILE_TOO_LARGE");
    }

    @Test
    void rejectsPathLikeAudioFilename() {
        assertCode(audio("../song.mp3", "audio/mpeg", id3()), "INVALID_AUDIO_FILE");
        assertCode(audio("folder\\song.mp3", "audio/mpeg", id3()), "INVALID_AUDIO_FILE");
    }

    @Test
    void acceptsRealJpegAndPngCoverContent() throws Exception {
        assertThat(validator.validateCover(cover("cover.jpg", "image/jpeg", image("jpg"))).extension())
                .isEqualTo("jpg");
        assertThat(validator.validateCover(cover("cover.png", "image/png", image("png"))).extension())
                .isEqualTo("png");
    }

    @Test
    void rejectsSvgAndUnsupportedCoverFormats() {
        assertCoverCode(cover("cover.svg", "image/svg+xml", "<svg/>".getBytes()), "INVALID_COVER_FILE");
        assertCoverCode(cover("cover.webp", "image/webp", new byte[]{1, 2, 3}), "INVALID_COVER_FILE");
    }

    @Test
    void rejectsFakeCoverAndMimeMismatch() throws Exception {
        assertCoverCode(cover("cover.png", "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G'}),
                "INVALID_COVER_FILE");
        assertCoverCode(cover("cover.png", "image/jpeg", image("png")), "INVALID_COVER_FILE");
    }

    @Test
    void rejectsOversizedCover() throws Exception {
        properties.setMaxCoverSize(DataSize.ofBytes(4));
        assertCoverCode(cover("cover.png", "image/png", image("png")), "COVER_FILE_TOO_LARGE");
    }

    private void assertCode(MockMultipartFile file, String code) {
        assertThatThrownBy(() -> validator.validateAudio(file))
                .isInstanceOf(BadRequestException.class).hasMessage(code);
    }

    private void assertCoverCode(MockMultipartFile file, String code) {
        assertThatThrownBy(() -> validator.validateCover(file))
                .isInstanceOf(BadRequestException.class).hasMessage(code);
    }

    private MockMultipartFile audio(String filename, String mime, byte[] bytes) {
        return new MockMultipartFile("audio", filename, mime, bytes);
    }

    private MockMultipartFile cover(String filename, String mime, byte[] bytes) {
        return new MockMultipartFile("cover", filename, mime, bytes);
    }

    private byte[] id3() {
        return new byte[]{'I', 'D', '3', 4, 0, 0, 0, 0};
    }

    private byte[] image(String format) throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }
}
