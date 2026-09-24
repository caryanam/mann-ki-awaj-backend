package com.mka.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsernameAvailabilityResponse {
    private String username;
    private boolean available;
    private String message;
    private List<String> suggestions;
}
