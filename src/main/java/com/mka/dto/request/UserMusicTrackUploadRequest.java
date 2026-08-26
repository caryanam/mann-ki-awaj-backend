package com.mka.dto.request;

import com.mka.enums.LanguageCode;
import com.mka.enums.MusicMood;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class UserMusicTrackUploadRequest {
    @NotBlank @Size(max = 150)
    private String title;
    @NotBlank @Size(max = 150)
    private String artistName;
    @NotNull
    private LanguageCode language;
    @NotEmpty
    @Size(max = 3)
    private Set<@NotNull MusicMood> moods;
    @Size(max = 80)
    private String genre;
    @Size(max = 1000)
    private String description;
    private Boolean originalWorkConfirmed;
    private Boolean rightsConfirmed;
}
