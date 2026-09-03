package com.mka.dto.response;

import com.mka.enums.ReportReason;
import com.mka.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {

    private Long id;
    private String reportId;
    private Long reporterId;
    private String reporterUsername;
    private String authorUsername;
    private String reportedContent;
    private String postId;
    private String contentType;
    private Long contentId;
    private ReportReason reason;
    private String description;
    private ReportStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
