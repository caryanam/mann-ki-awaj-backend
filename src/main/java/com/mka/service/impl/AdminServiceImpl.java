package com.mka.service.impl;

import com.mka.dto.request.SendWarningRequest;
import com.mka.dto.response.*;
import com.mka.entity.*;
import com.mka.enums.*;
import com.mka.exception.ResourceNotFoundException;
import com.mka.repository.*;
import com.mka.service.AdminService;
import com.mka.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReportRepository reportRepository;
    private final ProfileRepository profileRepository;
    private final ContentReviewQueueRepository reviewQueueRepository;
    private final NotificationService notificationService;
    private final BlockedContentRepository blockedContentRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboardStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByActiveTrue();
        long blockedUsers = userRepository.countByActiveFalse();

        long totalPosts = postRepository.countByStatus(PostStatus.ACTIVE);
        long todayPostsCount = postRepository.countByCreatedAtAfter(LocalDateTime.now().withHour(0).withMinute(0).withSecond(0));
        long totalComments = commentRepository.count();

        long pendingReports = reportRepository.countByStatus(ReportStatus.PENDING);
        long resolvedReports = reportRepository.countByStatus(ReportStatus.RESOLVED);
        long rejectedReports = reportRepository.countByStatus(ReportStatus.REJECTED);

        long pendingQueue = reviewQueueRepository.countByStatus(ReviewStatus.PENDING_REVIEW);

        return AdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalBlockedUsers(blockedUsers)
                .totalPosts(totalPosts)
                .todayPostsCount(todayPostsCount)
                .totalComments(totalComments)
                .totalPendingReports(pendingReports)
                .totalResolvedReports(resolvedReports)
                .totalRejectedReports(rejectedReports)
                .totalPendingReviewQueue(pendingQueue)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::mapToUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> searchUsers(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return getAllUsers(pageable);
        }
        return userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrMobileNumberContainingIgnoreCase(query, query, query, pageable)
                .map(this::mapToUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(Long userId) {
        User user = getUser(userId);
        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public void sendWarning(Long userId, SendWarningRequest request) {
        User user = getUser(userId);
        notificationService.createNotification(
                user,
                null,
                "system_avatar",
                NotificationType.WARNING,
                "WARNING (" + request.getWarningLevel() + "): " + request.getMessage(),
                null
        );
    }

    @Override
    @Transactional
    public void blockUser(Long userId) {
        User user = getUser(userId);
        user.setActive(false);
        userRepository.save(user);

        notificationService.createNotification(
                user,
                null,
                "system_avatar",
                NotificationType.ACCOUNT_BLOCKED,
                "Your account has been suspended due to violations of community guidelines.",
                null
        );
    }

    @Override
    @Transactional
    public void unblockUser(Long userId) {
        User user = getUser(userId);
        user.setActive(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = getUser(userId);
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateUserRole(Long userId, Role role) {
        User user = getUser(userId);
        user.setRole(role);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getUserPosts(Long userId, Pageable pageable) {
        User user = getUser(userId);
        return postRepository.findByUserId(user.getId(), pageable).map(this::mapToPostResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getUserComments(Long userId, Pageable pageable) {
        User user = getUser(userId);
        return commentRepository.findByUserId(user.getId(), pageable).map(this::mapToCommentResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getAllPosts(PostStatus status, Pageable pageable) {
        if (status != null) {
            return postRepository.findByStatus(status, pageable).map(this::mapToPostResponse);
        }
        return postRepository.findAll(pageable).map(this::mapToPostResponse);
    }

    @Override
    @Transactional
    public void updatePostStatus(Long postId, PostStatus status) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with ID: " + postId));
        post.setStatus(status);
        postRepository.save(post);
    }

    @Override
    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with ID: " + postId));
        post.setStatus(PostStatus.DELETED);
        postRepository.save(post);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getAllComments(CommentStatus status, Pageable pageable) {
        if (status != null) {
            return commentRepository.findByStatus(status, pageable).map(this::mapToCommentResponse);
        }
        return commentRepository.findAll(pageable).map(this::mapToCommentResponse);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with ID: " + commentId));
        comment.setStatus(CommentStatus.DELETED);
        commentRepository.save(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponse> getAllReports(Pageable pageable) {
        return reportRepository.findAll(pageable).map(this::mapToReportResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponse> getReportsByUser(Long userId, Pageable pageable) {
        User user = getUser(userId);
        return reportRepository.findByReporterIdOrderByCreatedAtDesc(user.getId(), pageable).map(this::mapToReportResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse getReportById(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with ID: " + reportId));
        return mapToReportResponse(report);
    }

    @Override
    @Transactional
    public void resolveReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with ID: " + reportId));
        report.setStatus(ReportStatus.RESOLVED);
        reportRepository.save(report);
    }

    @Override
    @Transactional
    public void rejectReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with ID: " + reportId));
        report.setStatus(ReportStatus.REJECTED);
        reportRepository.save(report);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewQueueResponse> getReviewQueue(Pageable pageable) {
        return reviewQueueRepository.findByStatus(ReviewStatus.PENDING_REVIEW, pageable)
                .map(this::mapToQueueResponse);
    }

    @Override
    @Transactional
    public void approveQueueItem(Long id) {
        ContentReviewQueue item = reviewQueueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Queue item not found with ID: " + id));
        item.setStatus(ReviewStatus.APPROVED);
        item.setReviewedAt(LocalDateTime.now());
        reviewQueueRepository.save(item);

        if ("POST".equalsIgnoreCase(item.getContentType())) {
            postRepository.findById(item.getContentId()).ifPresent(post -> {
                post.setStatus(PostStatus.ACTIVE);
                postRepository.save(post);
            });
        } else if ("COMMENT".equalsIgnoreCase(item.getContentType())) {
            commentRepository.findById(item.getContentId()).ifPresent(comment -> {
                comment.setStatus(CommentStatus.ACTIVE);
                commentRepository.save(comment);
            });
        }
    }

    @Override
    @Transactional
    public void rejectQueueItem(Long id) {
        ContentReviewQueue item = reviewQueueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Queue item not found with ID: " + id));
        item.setStatus(ReviewStatus.REJECTED);
        item.setReviewedAt(LocalDateTime.now());
        reviewQueueRepository.save(item);

        if ("POST".equalsIgnoreCase(item.getContentType())) {
            postRepository.findById(item.getContentId()).ifPresent(post -> {
                post.setStatus(PostStatus.DELETED);
                postRepository.save(post);
            });
        } else if ("COMMENT".equalsIgnoreCase(item.getContentType())) {
            commentRepository.findById(item.getContentId()).ifPresent(comment -> {
                comment.setStatus(CommentStatus.DELETED);
                commentRepository.save(comment);
            });
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    private AdminUserResponse mapToUserResponse(User user) {
        Profile profile = profileRepository.findByUser(user).orElse(null);
        return AdminUserResponse.builder()
                .id(user.getId())
                .userId(user.getFormattedUserId())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .fullName(user.getFullName())
                .username(profile != null ? profile.getUsername() : user.getEmail())
                .avatar(profile != null ? profile.getAvatar() : null)
                .preferredLanguage(profile != null ? profile.getPreferredLanguage() : "EN")
                .role(user.getRole())
                .active(user.getActive())
                .emailVerified(user.getEmailVerified())
                .mobileVerified(user.getMobileVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private PostResponse mapToPostResponse(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .postId(post.getFormattedPostId())
                .authorId(post.getUser() != null ? post.getUser().getId() : null)
                .username(post.getUsername())
                .title(post.getTitle())
                .summary(post.getSummary())
                .caption(post.getCaption())
                .description(post.getDescription())
                .authorAvatar(post.getAuthorAvatar())
                .originalContent(post.getOriginalContent())
                .translatedContent(post.getOriginalContent())
                .originalLanguage(post.getOriginalLanguage())
                .displayLanguage(post.getOriginalLanguage())
                .topic(post.getTopic())
                .type(post.getType())
                .imageUrl(post.getImageUrl())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .reactionCounts(Collections.emptyMap())
                .isLikedByCurrentUser(false)
                .isSavedByCurrentUser(false)
                .createdAt(post.getCreatedAt())
                .build();
    }

    private CommentResponse mapToCommentResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost() != null ? comment.getPost().getId() : null)
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .authorId(comment.getUser() != null ? comment.getUser().getId() : null)
                .authorAvatar(comment.getAuthorAvatar())
                .originalContent(comment.getOriginalContent())
                .translatedContent(comment.getOriginalContent())
                .originalLanguage(comment.getOriginalLanguage())
                .displayLanguage(comment.getOriginalLanguage())
                .likeCount(comment.getLikeCount())
                .reactionCounts(Collections.emptyMap())
                .isLikedByCurrentUser(false)
                .createdAt(comment.getCreatedAt())
                .replies(Collections.emptyList())
                .build();
    }

    private ReportResponse mapToReportResponse(Report report) {
        User reporter = report.getReporter();
        String formattedPostId = null;
        if ("POST".equalsIgnoreCase(report.getContentType()) && report.getContentId() != null) {
            formattedPostId = String.format("POST_%02d", report.getContentId());
        }

        return ReportResponse.builder()
                .id(report.getId())
                .reportId(report.getFormattedReportId())
                .reporterId(reporter.getId())
                .reporterUsername(reporter.getEmail())
                .postId(formattedPostId)
                .contentType(report.getContentType())
                .contentId(report.getContentId())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }

    private ReviewQueueResponse mapToQueueResponse(ContentReviewQueue item) {
        return ReviewQueueResponse.builder()
                .id(item.getId())
                .contentType(item.getContentType())
                .contentId(item.getContentId())
                .contentSnippet(item.getContentSnippet())
                .flaggedReason(item.getFlaggedReason())
                .aiConfidenceScore(item.getAiConfidenceScore())
                .status(item.getStatus())
                .createdAt(item.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BlockedContentResponse> getBlockedContent(String contentType, Pageable pageable) {
        Page<BlockedContent> list;
        if (contentType != null && !contentType.isBlank() && !contentType.equalsIgnoreCase("ALL")) {
            list = blockedContentRepository.findByContentType(contentType.trim().toUpperCase(), pageable);
        } else {
            list = blockedContentRepository.findAll(pageable);
        }

        return list.map(item -> BlockedContentResponse.builder()
                .id(item.getId())
                .userId(item.getUser() != null ? item.getUser().getId() : null)
                .contentType(item.getContentType())
                .authorUsername(item.getAuthorUsername())
                .authorEmail(item.getAuthorEmail())
                .originalContent(item.getOriginalContent())
                .flaggedReason(item.getFlaggedReason())
                .status(item.getStatus())
                .blockedAt(item.getBlockedAt())
                .build());
    }

    @Override
    @Transactional
    public void sendWarningForBlockedContent(Long id, SendWarningRequest request) {
        BlockedContent blocked = blockedContentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blocked content log not found with id: " + id));

        User user = blocked.getUser();
        if (user != null) {
            notificationService.createNotification(
                    user,
                    null,
                    "system_avatar",
                    NotificationType.WARNING,
                    "WARNING (" + request.getWarningLevel() + "): " + request.getMessage(),
                    null
            );
        }

        blocked.setStatus("WARNING_ISSUED");
        blockedContentRepository.save(blocked);
    }
}
