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

    Page<Post> findByStatusAndIsMusicCommunityTrue(PostStatus status, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Post p WHERE p.status = :status AND (p.isMusicCommunity = true OR (p.audioUrl IS NOT NULL AND p.audioUrl != ''))")
    Page<Post> findMusicCommunityPosts(@org.springframework.data.repository.query.Param("status") PostStatus status, Pageable pageable);


    Page<Post> findByUserIdAndStatus(Long userId, PostStatus status, Pageable pageable);

    Optional<Post> findByIdAndStatus(Long id, PostStatus status);

    Page<Post> findByUserId(Long userId, Pageable pageable);

    long countByUserId(Long userId);

    long countByUserIdAndStatusNot(Long userId, PostStatus status);

    long countByStatus(PostStatus status);

    long countByCreatedAtAfter(java.time.LocalDateTime dateTime);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    int deleteByStatusAndUpdatedAtBefore(PostStatus status, java.time.LocalDateTime dateTime);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Post p SET p.likeCount = COALESCE(p.likeCount, 0) + 1 WHERE p.id = :postId")
    void incrementLikeCount(@org.springframework.data.repository.query.Param("postId") Long postId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Post p SET p.likeCount = CASE WHEN COALESCE(p.likeCount, 0) > 0 THEN p.likeCount - 1 ELSE 0 END WHERE p.id = :postId")
    void decrementLikeCount(@org.springframework.data.repository.query.Param("postId") Long postId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Post p SET p.commentCount = COALESCE(p.commentCount, 0) + 1 WHERE p.id = :postId")
    void incrementCommentCount(@org.springframework.data.repository.query.Param("postId") Long postId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Post p SET p.commentCount = CASE WHEN COALESCE(p.commentCount, 0) > 0 THEN p.commentCount - 1 ELSE 0 END WHERE p.id = :postId")
    void decrementCommentCount(@org.springframework.data.repository.query.Param("postId") Long postId);
}
