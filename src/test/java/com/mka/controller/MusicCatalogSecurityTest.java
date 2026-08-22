package com.mka.controller;

import com.mka.config.JwtFilter;
import com.mka.config.JwtService;
import com.mka.config.SecurityConfig;
import com.mka.config.CustomUserDetailsService;
import com.mka.enums.LanguageCode;
import com.mka.enums.MusicMood;
import com.mka.service.MusicCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MusicCatalogController.class)
@Import({SecurityConfig.class, JwtFilter.class})
class MusicCatalogSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MusicCatalogService musicCatalogService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void anonymousGetCatalogIsPublic() throws Exception {
        when(musicCatalogService.getPublishedTracks(nullable(String.class), nullable(LanguageCode.class),
                nullable(MusicMood.class),
                nullable(String.class), nullable(Boolean.class), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/music/tracks"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousMutationUnderMusicApiIsNotPublic() throws Exception {
        mockMvc.perform(post("/api/music/tracks"))
                .andExpect(status().isUnauthorized());
    }
}
