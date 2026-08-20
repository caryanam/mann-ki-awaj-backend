package com.mka.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoodOfIndiaResponse {
    private String userMood;
    private long totalVotes;
    private Map<String, Long> moodCounts;
}
