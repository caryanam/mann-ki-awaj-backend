package com.mka.repository;

import com.mka.entity.ContentReviewQueue;
import com.mka.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentReviewQueueRepository extends JpaRepository<ContentReviewQueue, Long> {

    Page<ContentReviewQueue> findByStatus(ReviewStatus status, Pageable pageable);

    long countByStatus(ReviewStatus status);
}
