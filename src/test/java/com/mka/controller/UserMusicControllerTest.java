package com.mka.controller;

import com.mka.config.CustomUserDetailsService;
import com.mka.config.JwtFilter;
import com.mka.config.JwtService;
import com.mka.config.SecurityConfig;
import com.mka.dto.response.UserMusicTrackResponse;
import com.mka.enums.MusicTrackStatus;
import com.mka.service.MusicStorageService;
import com.mka.service.UserMusicService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserMusicController.class)
@Import({SecurityConfig.class, JwtFilter.class})
class UserMusicControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean UserMusicService service;
    @MockitoBean JwtService jwtService;
    @MockitoBean CustomUserDetailsService customUserDetailsService;
    @MockitoBean JpaMetamodelMappingContext jpaMetamodelMappingContext;
    @TempDir Path tempDir;

    @Test
    void anonymousCannotUseAnyOwnershipEndpoint() throws Exception {
        for (RequestBuilder request : routes()) mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void authenticatedUserUploadsWithPrincipalAndGetsPendingStatus() throws Exception {
        when(service.upload(eq("user@example.com"), any(), any(), isNull()))
                .thenReturn(response(MusicTrackStatus.PENDING_REVIEW));
        MockMultipartFile audio = new MockMultipartFile("audio", "mine.mp3", "audio/mpeg",
                new byte[]{'I','D','3',0});
        mockMvc.perform(multipart("/api/music/my-tracks").file(audio)
                        .param("title", "Mine").param("artistName", "Artist")
                        .param("language", "MR").param("mood", "CALM")
                        .param("originalWorkConfirmed", "true").param("rightsConfirmed", "true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));
        verify(service).upload(eq("user@example.com"), any(), any(), isNull());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void myTracksListDetailEditAndDeleteUseAuthenticatedIdentity() throws Exception {
        when(service.list(eq("user@example.com"), eq(MusicTrackStatus.REJECTED), any()))
                .thenReturn(new PageImpl<>(List.of(response(MusicTrackStatus.REJECTED))));
        when(service.get("user@example.com", 42L)).thenReturn(response(MusicTrackStatus.REJECTED));
        when(service.update(eq("user@example.com"), eq(42L), any()))
                .thenReturn(response(MusicTrackStatus.PENDING_REVIEW));
        mockMvc.perform(get("/api/music/my-tracks").param("status", "REJECTED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].status").value("REJECTED"));
        mockMvc.perform(get("/api/music/my-tracks/42")).andExpect(status().isOk());
        mockMvc.perform(put("/api/music/my-tracks/42").contentType("application/json").content("""
                {"title":"Mine","artistName":"Artist","language":"MR","mood":"CALM"}
                """)).andExpect(status().isOk());
        mockMvc.perform(delete("/api/music/my-tracks/42")).andExpect(status().isNoContent());
        verify(service).delete("user@example.com", 42L);
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void ownerPrivateAudioPreviewSupportsRangeAndNoStore() throws Exception {
        byte[] bytes = "0123456789".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        Path file = tempDir.resolve("private.mp3");
        Files.write(file, bytes);
        when(service.getPrivateAudio("user@example.com", 42L)).thenReturn(
                new MusicStorageService.StoredMusicResource(new FileSystemResource(file), "audio/mpeg", bytes.length));
        MvcResult initial = mockMvc.perform(get("/api/music/my-tracks/42/audio")
                        .header("Range", "bytes=2-5"))
                .andExpect(request().asyncStarted()).andReturn();
        mockMvc.perform(asyncDispatch(initial)).andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", "bytes 2-5/10"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(content().bytes("2345".getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
    }

    private List<RequestBuilder> routes() {
        return List.of(get("/api/music/my-tracks"), get("/api/music/my-tracks/42"),
                get("/api/music/my-tracks/42/audio"), get("/api/music/my-tracks/42/cover"),
                delete("/api/music/my-tracks/42"),
                put("/api/music/my-tracks/42").contentType("application/json").content("{}"));
    }

    private UserMusicTrackResponse response(MusicTrackStatus status) {
        return UserMusicTrackResponse.builder().id(42L).title("Mine").artist("Artist")
                .status(status).privateAudioUrl("/api/music/my-tracks/42/audio").build();
    }
}
