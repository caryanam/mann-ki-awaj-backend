package com.mka.controller;

import com.mka.config.CustomUserDetailsService;
import com.mka.config.JwtFilter;
import com.mka.config.JwtService;
import com.mka.config.SecurityConfig;
import com.mka.dto.response.AdminMusicTrackResponse;
import com.mka.enums.MusicTrackStatus;
import com.mka.exception.MusicStorageException;
import com.mka.service.MusicUploadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminMusicUploadController.class)
@Import({SecurityConfig.class, JwtFilter.class})
class AdminMusicUploadSecurityTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean MusicUploadService musicUploadService;
    @MockitoBean JwtService jwtService;
    @MockitoBean CustomUserDetailsService customUserDetailsService;
    @MockitoBean JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void anonymousUploadReturns401() throws Exception {
        mockMvc.perform(validRequest()).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void normalUserUploadReturns403() throws Exception {
        mockMvc.perform(validRequest()).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adminUploadReturns201AndUsesAuthenticationIdentity() throws Exception {
        when(musicUploadService.upload(eq("admin@example.com"), any(), any(), isNull()))
                .thenReturn(AdminMusicTrackResponse.builder().id(8L).title("Song")
                        .status(MusicTrackStatus.DRAFT).build());

        mockMvc.perform(validRequest())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(8))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
        verify(musicUploadService).upload(eq("admin@example.com"), any(), any(), isNull());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void invalidMetadataReturns400WithoutCallingUploadService() throws Exception {
        MockMultipartFile audio = new MockMultipartFile("audio", "song.mp3", "audio/mpeg",
                new byte[]{'I', 'D', '3', 4});
        mockMvc.perform(multipart("/api/admin/music/tracks")
                        .file(audio)
                        .param("artistName", "Artist")
                        .param("language", "HI")
                        .param("moods", "CALM"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
        verify(musicUploadService, org.mockito.Mockito.never()).upload(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void storageFailureUsesStableErrorWithoutLeakingFilesystemPath() throws Exception {
        when(musicUploadService.upload(eq("admin@example.com"), any(), any(), isNull()))
                .thenThrow(new MusicStorageException(new RuntimeException("C:\\private\\music.mp3")));
        mockMvc.perform(validRequest())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("MUSIC_STORAGE_ERROR"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("C:\\private"))));
    }

    private org.springframework.test.web.servlet.RequestBuilder validRequest() {
        MockMultipartFile audio = new MockMultipartFile("audio", "song.mp3", "audio/mpeg",
                new byte[]{'I', 'D', '3', 4});
        return multipart("/api/admin/music/tracks")
                .file(audio)
                .param("title", "Song")
                .param("artistName", "Artist")
                .param("language", "HI")
                .param("moods", "CALM")
                .param("featured", "false")
                .param("sortOrder", "0");
    }
}
