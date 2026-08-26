package com.mka.service.impl;

import com.mka.config.MusicStorageProperties;
import com.mka.controller.MusicMediaController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MusicMediaDeliveryTest {

    private static final byte[] AUDIO = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            .getBytes(StandardCharsets.US_ASCII);

    @TempDir
    Path tempDir;

    private LocalMusicStorageService storage;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        MusicStorageProperties properties = new MusicStorageProperties();
        properties.setRoot(tempDir.resolve("music").toString());
        storage = new LocalMusicStorageService(properties);
        storage.initialize();

        Files.write(tempDir.resolve("music/audio/test.mp3"), AUDIO);
        Files.write(tempDir.resolve("music/covers/cover.png"), new byte[]{1, 2, 3, 4});
        mockMvc = MockMvcBuilders.standaloneSetup(new MusicMediaController(storage)).build();
    }

    @Test
    void returnsCompleteAudioWithoutLoadingItAsControllerByteArray() throws Exception {
        performAudio(get("/media/music/audio/test.mp3"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("max-age=31536000")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("immutable")))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, AUDIO.length))
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(content().bytes(AUDIO));
    }

    @Test
    void returnsInitialByteRange() throws Exception {
        assertRange("bytes=0-9", 0, 9);
    }

    @Test
    void returnsMiddleByteRangeWithExactBytes() throws Exception {
        assertRange("bytes=5-9", 5, 9);
        assertRange("bytes=10-19", 10, 19);
    }

    @Test
    void returnsOpenEndedByteRange() throws Exception {
        assertRange("bytes=10-", 10, AUDIO.length - 1);
    }

    @Test
    void returnsSuffixByteRange() throws Exception {
        assertRange("bytes=-10", AUDIO.length - 10, AUDIO.length - 1);
    }

    @Test
    void rejectsUnsatisfiableRange() throws Exception {
        mockMvc.perform(get("/media/music/audio/test.mp3").header(HttpHeaders.RANGE, "bytes=999999-"))
                .andExpect(status().isRequestedRangeNotSatisfiable())
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes */" + AUDIO.length))
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"));
    }

    @Test
    void rejectsMultipleRangesPredictably() throws Exception {
        mockMvc.perform(get("/media/music/audio/test.mp3").header(HttpHeaders.RANGE, "bytes=0-2,5-7"))
                .andExpect(status().isRequestedRangeNotSatisfiable())
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes */" + AUDIO.length));
    }

    @Test
    void returnsNotFoundForMissingMedia() throws Exception {
        mockMvc.perform(get("/media/music/audio/missing.mp3"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsUnsupportedExtensions() throws Exception {
        Files.write(tempDir.resolve("music/audio/secret.txt"), AUDIO);
        mockMvc.perform(get("/media/music/audio/secret.txt"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void rejectsPlainEncodedAndDoubleEncodedTraversal() {
        for (String candidate : new String[]{
                "../application.properties",
                "%2e%2e%2fapplication.properties",
                "..\\application.properties",
                "%252e%252e%252fapplication.properties"
        }) {
            assertThatThrownBy(() -> storage.getAudio(candidate))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        }
    }

    @Test
    void returnsCoverWithApprovedMimeType() throws Exception {
        mockMvc.perform(get("/media/music/covers/cover.png"))
                .andExpect(status().isOk())
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, 4))
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(new byte[]{1, 2, 3, 4}));
    }

    @Test
    void rejectsSvgCover() throws Exception {
        Files.writeString(tempDir.resolve("music/covers/cover.svg"), "<svg/>");
        mockMvc.perform(get("/media/music/covers/cover.svg"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void headAudioReturnsHeadersAndNoBody() throws Exception {
        mockMvc.perform(head("/media/music/audio/test.mp3"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, AUDIO.length))
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(content().bytes(new byte[0]));
    }

    @Test
    void deliversEveryAudioFormatAcceptedByTheUploadValidator() throws Exception {
        var formats = java.util.Map.of(
                "test.mp3", "audio/mpeg",
                "test.m4a", "audio/mp4",
                "test.aac", "audio/aac",
                "test.wav", "audio/wav",
                "test.flac", "audio/flac",
                "test.ogg", "audio/ogg",
                "test.opus", "audio/opus"
        );
        for (var format : formats.entrySet()) {
            Files.write(tempDir.resolve("music/audio/" + format.getKey()), AUDIO);
            performAudio(get("/media/music/audio/" + format.getKey()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(format.getValue()));
        }
    }

    private void assertRange(String range, int start, int end) throws Exception {
        byte[] expected = java.util.Arrays.copyOfRange(AUDIO, start, end + 1);
        performAudio(get("/media/music/audio/test.mp3").header(HttpHeaders.RANGE, range))
                .andExpect(status().isPartialContent())
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE,
                        "bytes " + start + "-" + end + "/" + AUDIO.length))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, expected.length))
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(content().bytes(expected));
    }

    private ResultActions performAudio(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(request)
                .andExpect(request().asyncStarted())
                .andReturn();
        return mockMvc.perform(asyncDispatch(result));
    }
}
