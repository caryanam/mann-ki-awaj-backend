package com.mka.controller;

import com.mka.dto.response.MusicTrackResponse;
import com.mka.enums.LanguageCode;
import com.mka.enums.MusicMood;
import com.mka.exception.GlobalExceptionHandler;
import com.mka.exception.ResourceNotFoundException;
import com.mka.service.MusicCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MusicCatalogControllerTest {

    private MusicCatalogService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(MusicCatalogService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MusicCatalogController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void publicListReturnsWrappedPageAndForwardsFilters() throws Exception {
        when(service.getPublishedTracks(nullable(String.class), nullable(LanguageCode.class),
                nullable(MusicMood.class), nullable(String.class), nullable(Boolean.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/music/tracks")
                        .param("query", "love")
                        .param("language", "MR")
                        .param("mood", "CALM")
                        .param("genre", "Lo-fi")
                        .param("featured", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(7))
                .andExpect(jsonPath("$.data.content[0].artist").value("Artist"))
                .andExpect(jsonPath("$.data.content[0].audioUrl").value("https://api.awaazmanki.com/media/music/audio/abc.mp3"));

        verify(service).getPublishedTracks(eq("love"), eq(LanguageCode.MR), eq(MusicMood.CALM),
                eq("Lo-fi"), eq(true), any(Pageable.class));
    }

    @Test
    void excessivePageSizeIsCappedAtFiftyWithDeterministicSort() throws Exception {
        when(service.getPublishedTracks(nullable(String.class), nullable(LanguageCode.class),
                nullable(MusicMood.class), nullable(String.class), nullable(Boolean.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        mockMvc.perform(get("/api/music/tracks").param("page", "2").param("size", "1000000"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(service).getPublishedTracks(isNull(), isNull(), isNull(), isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
        assertThat(pageable.getValue().getSort().getOrderFor("featured").isDescending()).isTrue();
    }

    @Test
    void publishedDetailReturnsPlayerCompatibleContract() throws Exception {
        when(service.getPublishedTrack(7L)).thenReturn(response());
        mockMvc.perform(get("/api/music/tracks/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Track"))
                .andExpect(jsonPath("$.data.coverUrl").value("https://api.awaazmanki.com/media/music/covers/abc.webp"));
    }

    @Test
    void unavailableDetailReturnsNotFound() throws Exception {
        when(service.getPublishedTrack(9L)).thenThrow(new ResourceNotFoundException("Music track not found"));
        mockMvc.perform(get("/api/music/tracks/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    private MusicTrackResponse response() {
        return MusicTrackResponse.builder()
                .id(7L).title("Track").artist("Artist")
                .language(LanguageCode.MR).mood(MusicMood.CALM).genre("Lo-fi")
                .audioUrl("https://api.awaazmanki.com/media/music/audio/abc.mp3")
                .coverUrl("https://api.awaazmanki.com/media/music/covers/abc.webp")
                .durationSeconds(120).featured(true)
                .build();
    }
}
