package com.mka.dto.request;

import com.mka.enums.MusicMood;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class MusicTrackApprovalRequest {
    @NotEmpty
    private Set<@NotNull MusicMood> moods;
}
