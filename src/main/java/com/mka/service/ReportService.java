package com.mka.service;

import com.mka.dto.request.CreateReportRequest;
import com.mka.dto.response.ReportResponse;
import com.mka.enums.ReportReason;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReportService {

    ReportResponse reportPost(String email, Long postId, CreateReportRequest request);

    ReportResponse reportComment(String email, Long commentId, CreateReportRequest request);

    List<ReportReason> getReportReasons();

    Page<ReportResponse> getMyReports(String email, Pageable pageable);
}
