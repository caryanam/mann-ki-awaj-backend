package com.mka.controller;

import com.mka.config.CustomUserDetailsService;
import com.mka.config.JwtFilter;
import com.mka.config.JwtService;
import com.mka.config.SecurityConfig;
import com.mka.dto.response.AdminMusicTrackResponse;
import com.mka.enums.LanguageCode;
import com.mka.enums.MusicMood;
import com.mka.enums.MusicTrackStatus;
import com.mka.service.AdminMusicManagementService;
import com.mka.service.MusicStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminMusicManagementController.class)
@Import({SecurityConfig.class, JwtFilter.class})
class AdminMusicManagementControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AdminMusicManagementService service;
    @MockitoBean JwtService jwtService;
    @MockitoBean CustomUserDetailsService customUserDetailsService;
    @MockitoBean JpaMetamodelMappingContext jpaMetamodelMappingContext;
    @TempDir Path tempDir;

    @Test
    void anonymousIsRejectedFromEveryAdminManagementRoute() throws Exception {
        for (RequestBuilder request : allRoutes()) mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void normalUserIsRejectedFromEveryAdminManagementRoute() throws Exception {
        for (RequestBuilder request : allRoutes()) mockMvc.perform(request).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adminCanUseEveryManagementMutation() throws Exception {
        AdminMusicTrackResponse response = response(MusicTrackStatus.DRAFT);
        when(service.update(eq(7L), any())).thenReturn(response);
        when(service.publish(7L)).thenReturn(response);
        when(service.unpublish(7L)).thenReturn(response);
        when(service.approve(7L)).thenReturn(response);
        when(service.reject(eq(7L), anyString())).thenReturn(response);
        mockMvc.perform(validPut()).andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/music/tracks/7/publish")).andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/music/tracks/7/unpublish")).andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/music/tracks/7/approve")).andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/music/tracks/7/reject").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Rights could not be confirmed\"}")).andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/music/tracks/7")).andExpect(status().isNoContent());
        verify(service).delete(7L);
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adminListForwardsSearchFiltersAndBoundsPagination() throws Exception {
        var pageable = PageRequest.of(0, 50);
        when(service.list(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(response(MusicTrackStatus.UNPUBLISHED)), pageable, 1));
        mockMvc.perform(get("/api/admin/music/tracks")
                        .param("query", "song").param("status", "UNPUBLISHED")
                        .param("language", "HI").param("mood", "CALM")
                        .param("genre", "Folk").param("featured", "true")
                        .param("page", "-2").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("UNPUBLISHED"));
        var pageableCaptor = org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(service).list(eq("song"), eq(MusicTrackStatus.UNPUBLISHED), eq(LanguageCode.HI),
                eq(MusicMood.CALM), eq("Folk"), eq(true), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void privateAudioPreviewSupportsExactRangesAndNoStore() throws Exception {
        byte[] audio = "0123456789ABCDEFGHIJ".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        Path file = tempDir.resolve("private.mp3");
        Files.write(file, audio);
        when(service.getPrivateAudio(7L)).thenReturn(new MusicStorageService.StoredMusicResource(
                new FileSystemResource(file), "audio/mpeg", audio.length));
        MvcResult initial = mockMvc.perform(get("/api/admin/music/tracks/7/audio")
                        .header("Range", "bytes=5-9"))
                .andExpect(request().asyncStarted()).andReturn();
        mockMvc.perform(asyncDispatch(initial))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", "bytes 5-9/20"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(content().bytes("56789".getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adminDetailAndPrivateCoverPreviewWorkWithoutExposingStoragePath() throws Exception {
        Path file = tempDir.resolve("private-cover.png");
        Files.write(file, new byte[]{1, 2, 3, 4});
        when(service.get(7L)).thenReturn(response(MusicTrackStatus.DRAFT));
        when(service.getPrivateCover(7L)).thenReturn(new MusicStorageService.StoredMusicResource(
                new FileSystemResource(file), "image/png", 4));
        mockMvc.perform(get("/api/admin/music/tracks/7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(tempDir.toString()))));
        mockMvc.perform(get("/api/admin/music/tracks/7/cover"))
                .andExpect(status().isOk()).andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(new byte[]{1, 2, 3, 4}));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void invalidStatusFilterUsesStable400Response() throws Exception {
        mockMvc.perform(get("/api/admin/music/tracks").param("status", "BROKEN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("INVALID_REQUEST_PARAMETER"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void metadataUpdateRejectsNegativeSortOrderBeforeServiceCall() throws Exception {
        mockMvc.perform(put("/api/admin/music/tracks/7").contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"Song","artistName":"Artist","language":"HI","mood":"CALM",
                 "featured":false,"sortOrder":-1}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sortOrder").exists());
        org.mockito.Mockito.verify(service, org.mockito.Mockito.never()).update(eq(7L), any());
    }

    private List<RequestBuilder> allRoutes() {
        return List.of(
                get("/api/admin/music/tracks"),
                get("/api/admin/music/tracks/7"),
                validPut(),
                post("/api/admin/music/tracks/7/publish"),
                post("/api/admin/music/tracks/7/unpublish"),
                post("/api/admin/music/tracks/7/approve"),
                post("/api/admin/music/tracks/7/reject").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Reason\"}"),
                delete("/api/admin/music/tracks/7"),
                get("/api/admin/music/tracks/7/audio"),
                get("/api/admin/music/tracks/7/cover"));
    }

    private RequestBuilder validPut() {
        return put("/api/admin/music/tracks/7").contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"Song","artistName":"Artist","language":"HI","mood":"CALM",
                 "genre":"Folk","description":"Description","featured":false,"sortOrder":0}
                """);
    }

    private AdminMusicTrackResponse response(MusicTrackStatus status) {
        return AdminMusicTrackResponse.builder().id(7L).title("Song").artist("Artist")
                .language(LanguageCode.HI).mood(MusicMood.CALM).status(status)
                .featured(false).sortOrder(0).build();
    }
}
