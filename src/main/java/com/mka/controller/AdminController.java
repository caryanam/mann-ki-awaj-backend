package com.mka.controller;

import com.mka.dto.request.SendWarningRequest;
import com.mka.dto.response.*;
import com.mka.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Panel", description = "Admin Dashboard, Moderation Queue & User Management APIs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final com.mka.service.EnquiryService enquiryService;


    @GetMapping("/dashboard")
    @Operation(summary = "Get Admin Dashboard Overview Statistics")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(
                ApiResponse.<AdminDashboardResponse>builder()
                        .success(true)
                        .message("Admin dashboard stats retrieved")
                        .data(adminService.getDashboardStats())
                        .build()
        );
    }

    @GetMapping("/users")
    @Operation(summary = "List all registered users (Admin only)")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<AdminUserResponse> users = adminService.getAllUsers(PageRequest.of(page, size, Sort.by("id").descending()));
        return ResponseEntity.ok(
                ApiResponse.<Page<AdminUserResponse>>builder()
                        .success(true)
                        .message("Users list retrieved")
                        .data(users)
                        .build()
        );
    }

    @GetMapping("/users/search")
    @Operation(summary = "Search users by name, email, or mobile number (Admin only)")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<AdminUserResponse> users = adminService.searchUsers(query, PageRequest.of(page, size, Sort.by("id").descending()));
        return ResponseEntity.ok(
                ApiResponse.<Page<AdminUserResponse>>builder()
                        .success(true)
                        .message("Users matching search query retrieved")
                        .data(users)
                        .build()
        );
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get detailed user info by ID (Admin only)")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.<AdminUserResponse>builder()
                        .success(true)
                        .message("User details retrieved")
                        .data(adminService.getUserById(id))
                        .build()
        );
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete or soft-deactivate user account (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("User account deleted successfully")
                        .build()
        );
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "Update user role (Admin only)")
    public ResponseEntity<ApiResponse<Void>> updateUserRole(
            @PathVariable Long id,
            @RequestParam com.mka.enums.Role role) {

        adminService.updateUserRole(id, role);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("User role updated successfully")
                        .build()
        );
    }

    @GetMapping("/users/{id}/posts")
    @Operation(summary = "Get all posts created by a specific user (Admin only)")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getUserPosts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<PostResponse> posts = adminService.getUserPosts(id, PageRequest.of(page, size, Sort.by("id").descending()));
        return ResponseEntity.ok(
                ApiResponse.<Page<PostResponse>>builder()
                        .success(true)
                        .message("User post history retrieved")
                        .data(posts)
                        .build()
        );
    }

    @GetMapping("/users/{id}/comments")
    @Operation(summary = "Get all comments created by a specific user (Admin only)")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getUserComments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<CommentResponse> comments = adminService.getUserComments(id, PageRequest.of(page, size, Sort.by("id").descending()));
        return ResponseEntity.ok(
                ApiResponse.<Page<CommentResponse>>builder()
                        .success(true)
                        .message("User comment history retrieved")
                        .data(comments)
                        .build()
        );
    }

    @GetMapping("/users/{id}/reports")
    @Operation(summary = "Get all reports submitted by a specific user (Admin only)")
    public ResponseEntity<ApiResponse<Page<ReportResponse>>> getReportsByUser(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ReportResponse> reports = adminService.getReportsByUser(id, PageRequest.of(page, size));
        return ResponseEntity.ok(
                ApiResponse.<Page<ReportResponse>>builder()
                        .success(true)
                        .message("User report history retrieved")
                        .data(reports)
                        .build()
        );
    }

    @PutMapping("/users/{id}/warning")
    @Operation(summary = "Send moderation warning to user (Admin only)")
    public ResponseEntity<ApiResponse<Void>> sendWarning(
            @PathVariable Long id,
            @Valid @RequestBody SendWarningRequest request) {

        adminService.sendWarning(id, request);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Warning issued to user successfully")
                        .build()
        );
    }

    @PutMapping("/users/{id}/block")
    @Operation(summary = "Block/Suspend user account (Admin only)")
    public ResponseEntity<ApiResponse<Void>> blockUser(@PathVariable Long id) {
        adminService.blockUser(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("User blocked successfully")
                        .build()
        );
    }

    @PutMapping("/users/{id}/unblock")
    @Operation(summary = "Unblock user account (Admin only)")
    public ResponseEntity<ApiResponse<Void>> unblockUser(@PathVariable Long id) {
        adminService.unblockUser(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("User unblocked successfully")
                        .build()
        );
    }

    @GetMapping("/posts")
    @Operation(summary = "List all posts with optional status filter (Admin only)")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getAllPosts(
            @RequestParam(required = false) com.mka.enums.PostStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<PostResponse> posts = adminService.getAllPosts(status, PageRequest.of(page, size, Sort.by("id").descending()));
        return ResponseEntity.ok(
                ApiResponse.<Page<PostResponse>>builder()
                        .success(true)
                        .message("Posts list retrieved")
                        .data(posts)
                        .build()
        );
    }

    @PutMapping("/posts/{id}/status")
    @Operation(summary = "Update post status (Admin only)")
    public ResponseEntity<ApiResponse<Void>> updatePostStatus(
            @PathVariable Long id,
            @RequestParam com.mka.enums.PostStatus status) {

        adminService.updatePostStatus(id, status);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Post status updated successfully")
                        .build()
        );
    }

    @DeleteMapping("/posts/{id}")
    @Operation(summary = "Force delete post (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long id) {
        adminService.deletePost(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Post deleted successfully")
                        .build()
        );
    }

    @GetMapping("/comments")
    @Operation(summary = "List all comments with optional status filter (Admin only)")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getAllComments(
            @RequestParam(required = false) com.mka.enums.CommentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<CommentResponse> comments = adminService.getAllComments(status, PageRequest.of(page, size, Sort.by("id").descending()));
        return ResponseEntity.ok(
                ApiResponse.<Page<CommentResponse>>builder()
                        .success(true)
                        .message("Comments list retrieved")
                        .data(comments)
                        .build()
        );
    }

    @DeleteMapping("/comments/{id}")
    @Operation(summary = "Force delete comment (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long id) {
        adminService.deleteComment(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Comment deleted successfully")
                        .build()
        );
    }

    @GetMapping("/reports")
    @Operation(summary = "List all content reports (Admin only)")
    public ResponseEntity<ApiResponse<Page<ReportResponse>>> getAllReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ReportResponse> reports = adminService.getAllReports(PageRequest.of(page, size, Sort.by("id").descending()));
        return ResponseEntity.ok(
                ApiResponse.<Page<ReportResponse>>builder()
                        .success(true)
                        .message("Reports list retrieved")
                        .data(reports)
                        .build()
        );
    }

    @GetMapping("/reports/{id}")
    @Operation(summary = "Get report by ID (Admin only)")
    public ResponseEntity<ApiResponse<ReportResponse>> getReportById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.<ReportResponse>builder()
                        .success(true)
                        .message("Report details retrieved")
                        .data(adminService.getReportById(id))
                        .build()
        );
    }

    @PutMapping("/reports/{id}/resolve")
    @Operation(summary = "Mark report as RESOLVED (Admin only)")
    public ResponseEntity<ApiResponse<Void>> resolveReport(@PathVariable Long id) {
        adminService.resolveReport(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Report resolved successfully")
                        .build()
        );
    }

    @PutMapping("/reports/{id}/reject")
    @Operation(summary = "Mark report as REJECTED (Admin only)")
    public ResponseEntity<ApiResponse<Void>> rejectReport(@PathVariable Long id) {
        adminService.rejectReport(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Report rejected successfully")
                        .build()
        );
    }

    @GetMapping("/moderation/queue")
    @Operation(summary = "Get content-review queue of AI-flagged content (Admin only)")
    public ResponseEntity<ApiResponse<Page<ReviewQueueResponse>>> getReviewQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ReviewQueueResponse> queue = adminService.getReviewQueue(PageRequest.of(page, size));
        return ResponseEntity.ok(
                ApiResponse.<Page<ReviewQueueResponse>>builder()
                        .success(true)
                        .message("Moderation review queue retrieved")
                        .data(queue)
                        .build()
        );
    }

    @PutMapping("/moderation/queue/{id}/approve")
    @Operation(summary = "Approve flagged content in review queue (Admin only)")
    public ResponseEntity<ApiResponse<Void>> approveQueueItem(@PathVariable Long id) {
        adminService.approveQueueItem(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Content approved successfully")
                        .build()
        );
    }

    @PutMapping("/moderation/queue/{id}/reject")
    @Operation(summary = "Reject/remove flagged content in review queue (Admin only)")
    public ResponseEntity<ApiResponse<Void>> rejectQueueItem(@PathVariable Long id) {
        adminService.rejectQueueItem(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Content rejected successfully")
                        .build()
        );
    }

    @GetMapping("/blocked-content")
    @Operation(summary = "Get list of all AI moderated/blocked content (Admin only)")
    public ResponseEntity<ApiResponse<Page<BlockedContentResponse>>> getBlockedContent(
            @RequestParam(required = false) String contentType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<BlockedContentResponse> content = adminService.getBlockedContent(contentType, PageRequest.of(page, size, Sort.by("id").descending()));
        return ResponseEntity.ok(
                ApiResponse.<Page<BlockedContentResponse>>builder()
                        .success(true)
                        .message("AI blocked content list retrieved")
                        .data(content)
                        .build()
        );
    }

    @PutMapping("/moderation/ai-blocked/{id}/warn")
    @Operation(summary = "Issue warning for specific AI blocked content (Admin only)")
    public ResponseEntity<ApiResponse<Void>> issueWarningForAiBlocked(
            @PathVariable Long id,
            @Valid @RequestBody SendWarningRequest request) {

        adminService.sendWarningForBlockedContent(id, request);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Warning issued and content status updated")
                        .build()
        );
    }

    @GetMapping("/enquiries")
    @Operation(summary = "Get all landing page user inquiries (Admin only)")
    public ResponseEntity<ApiResponse<java.util.List<com.mka.dto.response.EnquiryResponse>>> getAllEnquiries() {
        return ResponseEntity.ok(
                ApiResponse.<java.util.List<com.mka.dto.response.EnquiryResponse>>builder()
                        .success(true)
                        .message("Enquiries list retrieved")
                        .data(enquiryService.getAllEnquiries())
                        .build()
        );
    }

    @PutMapping("/enquiries/{id}/status")
    @Operation(summary = "Update inquiry status and admin notes (Admin only)")
    public ResponseEntity<ApiResponse<com.mka.dto.response.EnquiryResponse>> updateEnquiryStatus(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String adminNotes) {

        com.mka.dto.response.EnquiryResponse updated = enquiryService.updateStatus(id, status, adminNotes);
        return ResponseEntity.ok(
                ApiResponse.<com.mka.dto.response.EnquiryResponse>builder()
                        .success(true)
                        .message("Enquiry status updated successfully")
                        .data(updated)
                        .build()
        );
    }


    @DeleteMapping("/enquiries/{id}")
    @Operation(summary = "Delete user inquiry (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteEnquiry(@PathVariable Long id) {
        enquiryService.deleteEnquiry(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Enquiry deleted successfully")
                        .build()
        );
    }

    @GetMapping("/topics")
    @Operation(summary = "List all user-created custom topics with optional search & filter (Admin only)")
    public ResponseEntity<ApiResponse<Page<java.util.Map<String, Object>>>> getTopics(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) com.mka.enums.PostTopic parentTopic,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<java.util.Map<String, Object>> topics = adminService.getTopics(
                search, parentTopic, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(
                ApiResponse.<Page<java.util.Map<String, Object>>>builder()
                        .success(true)
                        .message("Topics retrieved successfully")
                        .data(topics)
                        .build()
        );
    }

    @DeleteMapping("/topics/{id}")
    @Operation(summary = "Delete custom topic (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteTopic(@PathVariable Long id) {
        adminService.deleteTopic(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Topic deleted successfully")
                        .build()
        );
    }
}

