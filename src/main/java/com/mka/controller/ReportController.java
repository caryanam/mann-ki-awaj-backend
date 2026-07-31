package com.mka.controller;

import com.mka.config.UserPrincipal;
import com.mka.dto.request.CreateReportRequest;
import com.mka.dto.response.ApiResponse;
import com.mka.dto.response.ReportResponse;
import com.mka.enums.ReportReason;
import com.mka.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Reports", description = "Content Reporting & History APIs")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/reports/post/{postId}")
    @Operation(summary = "Report a post for hate speech, abuse or violation")
    public ResponseEntity<ApiResponse<ReportResponse>> reportPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId,
            @Valid @RequestBody CreateReportRequest request) {

        ReportResponse report = reportService.reportPost(principal.getUsername(), postId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<ReportResponse>builder()
                        .success(true)
                        .message("Post reported successfully")
                        .data(report)
                        .build()
        );
    }

    @PostMapping("/reports/comment/{commentId}")
    @Operation(summary = "Report a comment for hate speech, abuse or violation")
    public ResponseEntity<ApiResponse<ReportResponse>> reportComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long commentId,
            @Valid @RequestBody CreateReportRequest request) {

        ReportResponse report = reportService.reportComment(principal.getUsername(), commentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<ReportResponse>builder()
                        .success(true)
                        .message("Comment reported successfully")
                        .data(report)
                        .build()
        );
    }

    @GetMapping("/reports/my-reports")
    @Operation(summary = "Get current user's submitted report history")
    public ResponseEntity<ApiResponse<Page<ReportResponse>>> getMyReports(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ReportResponse> reports = reportService.getMyReports(principal.getUsername(), PageRequest.of(page, size));
        return ResponseEntity.ok(
                ApiResponse.<Page<ReportResponse>>builder()
                        .success(true)
                        .message("Report history retrieved successfully")
                        .data(reports)
                        .build()
        );
    }

    @GetMapping("/report-reasons")
    @Operation(summary = "Get list of available report reasons")
    public ResponseEntity<ApiResponse<List<ReportReason>>> getReportReasons() {
        return ResponseEntity.ok(
                ApiResponse.<List<ReportReason>>builder()
                        .success(true)
                        .message("Report reasons retrieved successfully")
                        .data(reportService.getReportReasons())
                        .build()
        );
    }
}
