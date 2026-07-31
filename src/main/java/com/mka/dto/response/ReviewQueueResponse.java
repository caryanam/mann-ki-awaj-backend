package com.mka.dto.response;

import com.mka.enums.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewQueueResponse {

    private Long id;
    private String contentType;
    private Long contentId;
    private String contentSnippet;
    private String flaggedReason;
    private Double aiConfidenceScore;
    private ReviewStatus status;
    private LocalDateTime createdAt;
}
