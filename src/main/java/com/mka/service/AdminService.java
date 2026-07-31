package com.mka.service;

import com.mka.dto.request.SendWarningRequest;
import com.mka.dto.response.AdminDashboardResponse;
import com.mka.dto.response.AdminUserResponse;
import com.mka.dto.response.ReportResponse;
import com.mka.dto.response.ReviewQueueResponse;
import com.mka.dto.response.CommentResponse;
import com.mka.dto.response.PostResponse;
import com.mka.enums.CommentStatus;
import com.mka.enums.PostStatus;
import com.mka.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminService {

    AdminDashboardResponse getDashboardStats();

    Page<AdminUserResponse> getAllUsers(Pageable pageable);

    Page<AdminUserResponse> searchUsers(String query, Pageable pageable);

    AdminUserResponse getUserById(Long userId);

    void sendWarning(Long userId, SendWarningRequest request);

    void blockUser(Long userId);

    void unblockUser(Long userId);

    void deleteUser(Long userId);

    void updateUserRole(Long userId, Role role);

    Page<PostResponse> getUserPosts(Long userId, Pageable pageable);

    Page<CommentResponse> getUserComments(Long userId, Pageable pageable);

    Page<PostResponse> getAllPosts(PostStatus status, Pageable pageable);

    void updatePostStatus(Long postId, PostStatus status);

    void deletePost(Long postId);

    Page<CommentResponse> getAllComments(CommentStatus status, Pageable pageable);

    void deleteComment(Long commentId);

    Page<ReportResponse> getAllReports(Pageable pageable);

    Page<ReportResponse> getReportsByUser(Long userId, Pageable pageable);

    ReportResponse getReportById(Long reportId);

    void resolveReport(Long reportId);

    void rejectReport(Long reportId);

    Page<ReviewQueueResponse> getReviewQueue(Pageable pageable);

    void approveQueueItem(Long id);

    void rejectQueueItem(Long id);
}
