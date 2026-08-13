package com.mka.repository;

import com.mka.entity.BlockedContent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlockedContentRepository extends JpaRepository<BlockedContent, Long> {
    Page<BlockedContent> findByContentType(String contentType, Pageable pageable);
}
