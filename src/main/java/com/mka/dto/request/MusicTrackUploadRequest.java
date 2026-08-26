package com.mka.dto.request;

import com.mka.enums.LanguageCode;
import com.mka.enums.MusicMood;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Set;

@Data
public class MusicTrackUploadRequest {
    @NotBlank @Size(max = 150)
    private String title;

    @NotBlank @Size(max = 150)
    private String artistName;

    @NotNull
    private LanguageCode language;

    @NotEmpty
    private Set<@NotNull MusicMood> moods;

    @Size(max = 80)
    private String genre;

    @Size(max = 1000)
    private String description;

    private Boolean featured = false;

    @PositiveOrZero
    private Integer sortOrder = 0;
}
