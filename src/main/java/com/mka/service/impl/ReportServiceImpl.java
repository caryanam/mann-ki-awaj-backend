package com.mka.service.impl;

import com.mka.dto.request.CreateReportRequest;
import com.mka.dto.response.ReportResponse;
import com.mka.entity.Comment;
import com.mka.entity.Post;
import com.mka.entity.Profile;
import com.mka.entity.Report;
import com.mka.entity.User;
import com.mka.enums.CommentStatus;
import com.mka.enums.PostStatus;
import com.mka.enums.ReportReason;
import com.mka.enums.ReportStatus;
import com.mka.exception.ResourceAlreadyExistsException;
import com.mka.exception.ResourceNotFoundException;
import com.mka.exception.UnauthorizedException;
import com.mka.repository.CommentRepository;
import com.mka.repository.PostRepository;
import com.mka.repository.ProfileRepository;
import com.mka.repository.ReportRepository;
import com.mka.repository.UserRepository;
import com.mka.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    @Override
    @Transactional
    public ReportResponse reportPost(String email, Long postId, CreateReportRequest request) {
        User reporter = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (!Boolean.TRUE.equals(reporter.getActive())) {
            throw new UnauthorizedException("User account is inactive or deactivated");
        }

        Post post = postRepository.findByIdAndStatus(postId, PostStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        if (reportRepository.existsByReporterIdAndContentTypeAndContentId(reporter.getId(), "POST", postId)) {
            throw new ResourceAlreadyExistsException("You have already reported this post");
        }

        Report report = Report.builder()
                .reporter(reporter)
                .contentType("POST")
                .contentId(postId)
                .reason(request.getReason())
                .description(request.getDescription())
                .status(ReportStatus.PENDING)
                .build();

        Report savedReport = reportRepository.save(report);
        return mapToResponse(savedReport);
    }

    @Override
    @Transactional
    public ReportResponse reportComment(String email, Long commentId, CreateReportRequest request) {
        User reporter = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (!Boolean.TRUE.equals(reporter.getActive())) {
            throw new UnauthorizedException("User account is inactive or deactivated");
        }

        Comment comment = commentRepository.findByIdAndStatus(commentId, CommentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        if (reportRepository.existsByReporterIdAndContentTypeAndContentId(reporter.getId(), "COMMENT", commentId)) {
            throw new ResourceAlreadyExistsException("You have already reported this comment");
        }

        Report report = Report.builder()
                .reporter(reporter)
                .contentType("COMMENT")
                .contentId(commentId)
                .reason(request.getReason())
                .description(request.getDescription())
                .status(ReportStatus.PENDING)
                .build();

        Report savedReport = reportRepository.save(report);
        return mapToResponse(savedReport);
    }

    @Override
    public List<ReportReason> getReportReasons() {
        return Arrays.asList(ReportReason.values());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponse> getMyReports(String email, Pageable pageable) {
        User reporter = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (!Boolean.TRUE.equals(reporter.getActive())) {
            throw new UnauthorizedException("User account is inactive or deactivated");
        }

        return reportRepository.findByReporterIdOrderByCreatedAtDesc(reporter.getId(), pageable)
                .map(this::mapToResponse);
    }

    private String getGeneratedUsername(User user) {
        if (user == null) return "anonymous";
        Profile profile = profileRepository.findByUser(user).orElse(null);
        if (profile != null && profile.getUsername() != null && !profile.getUsername().isBlank()) {
            String u = profile.getUsername().trim();
            return u.startsWith("@") ? u.substring(1) : u;
        }
        if (user.getEmail() != null && user.getEmail().contains("@")) {
            return user.getEmail().split("@")[0];
        }
        return "user_" + user.getId();
    }

    private ReportResponse mapToResponse(Report report) {
        String reporterHandle = getGeneratedUsername(report.getReporter());
        String authorHandle = null;
        String reportedContent = report.getDescription();

        if ("POST".equalsIgnoreCase(report.getContentType()) && report.getContentId() != null) {
            Post post = postRepository.findById(report.getContentId()).orElse(null);
            if (post != null) {
                authorHandle = post.getUsername() != null ? post.getUsername() : getGeneratedUsername(post.getUser());
                if (reportedContent == null || reportedContent.isBlank()) {
                    reportedContent = post.getOriginalContent() != null ? post.getOriginalContent() : post.getTitle();
                }
            }
        } else if ("COMMENT".equalsIgnoreCase(report.getContentType()) && report.getContentId() != null) {
            Comment comment = commentRepository.findById(report.getContentId()).orElse(null);
            if (comment != null) {
                authorHandle = getGeneratedUsername(comment.getUser());
                if (reportedContent == null || reportedContent.isBlank()) {
                    reportedContent = comment.getOriginalContent();
                }
            }
        }

        return ReportResponse.builder()
                .id(report.getId())
                .reportId(report.getFormattedReportId())
                .reporterId(report.getReporter() != null ? report.getReporter().getId() : null)
                .reporterUsername(reporterHandle)
                .authorUsername(authorHandle != null ? authorHandle : "anonymous")
                .reportedContent(reportedContent)
                .postId(report.getContentId() != null ? report.getContentId().toString() : null)
                .contentType(report.getContentType())
                .contentId(report.getContentId())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
