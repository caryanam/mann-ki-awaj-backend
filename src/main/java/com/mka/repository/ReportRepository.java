package com.mka.repository;

import com.mka.entity.Report;
import com.mka.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

    Page<Report> findByReporterIdOrderByCreatedAtDesc(Long reporterId, Pageable pageable);

    boolean existsByReporterIdAndContentTypeAndContentId(Long reporterId, String contentType, Long contentId);

    long countByStatus(ReportStatus status);
}
