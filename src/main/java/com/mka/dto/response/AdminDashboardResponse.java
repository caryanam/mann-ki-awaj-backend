package com.mka.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {

    private long totalUsers;
    private long activeUsers;
    private long totalBlockedUsers;
    private long totalPosts;
    private long todayPostsCount;
    private long totalComments;
    private long totalPendingReports;
    private long totalResolvedReports;
    private long totalRejectedReports;
    private long totalPendingReviewQueue;
}
