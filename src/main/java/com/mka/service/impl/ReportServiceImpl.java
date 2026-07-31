package com.mka.service.impl;

import com.mka.dto.request.CreateReportRequest;
import com.mka.dto.response.ReportResponse;
import com.mka.entity.Comment;
import com.mka.entity.Post;
import com.mka.entity.Report;
import com.mka.entity.User;
import com.mka.enums.CommentStatus;
import com.mka.enums.PostStatus;
import com.mka.enums.ReportReason;
import com.mka.enums.ReportStatus;
import com.mka.exception.ResourceAlreadyExistsException;
import com.mka.exception.ResourceNotFoundException;
import com.mka.repository.CommentRepository;
import com.mka.repository.PostRepository;
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

    @Override
    @Transactional
    public ReportResponse reportPost(String email, Long postId, CreateReportRequest request) {
        User reporter = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

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

        return reportRepository.findByReporterIdOrderByCreatedAtDesc(reporter.getId(), pageable)
                .map(this::mapToResponse);
    }

    private ReportResponse mapToResponse(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .reportId(report.getFormattedReportId())
                .reporterId(report.getReporter() != null ? report.getReporter().getId() : null)
                .reporterUsername(report.getReporter() != null ? report.getReporter().getFullName() : null)
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
