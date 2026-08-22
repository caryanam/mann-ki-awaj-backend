package com.mka.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MusicTrackRejectRequest {
    @NotBlank
    @Size(max = 500)
    private String reason;
}
