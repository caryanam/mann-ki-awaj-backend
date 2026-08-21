package com.mka.repository;

import com.mka.entity.Post;
import com.mka.enums.PostStatus;
import com.mka.enums.PostTopic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByStatus(PostStatus status, Pageable pageable);

    Page<Post> findByStatusAndTopicIgnoreCase(PostStatus status, String topic, Pageable pageable);

    Page<Post> findByStatusAndSubtopicIgnoreCase(PostStatus status, String subtopic, Pageable pageable);


    Page<Post> findByUserIdAndStatus(Long userId, PostStatus status, Pageable pageable);

    Optional<Post> findByIdAndStatus(Long id, PostStatus status);

    Page<Post> findByUserId(Long userId, Pageable pageable);

    long countByUserId(Long userId);

    long countByStatus(PostStatus status);

    long countByCreatedAtAfter(java.time.LocalDateTime dateTime);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    int deleteByStatusAndUpdatedAtBefore(PostStatus status, java.time.LocalDateTime dateTime);
}
