package com.mka.repository;

import com.mka.entity.Comment;
import com.mka.enums.CommentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Comment c where c.id = :id and c.status = :status")
    Optional<Comment> findActiveForUpdate(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("status") CommentStatus status);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Comment c set c.likeCount = coalesce(c.likeCount, 0) + 1 where c.id = :id")
    void incrementLikeCount(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Comment c set c.likeCount = case when coalesce(c.likeCount, 0) > 0 then c.likeCount - 1 else 0 end where c.id = :id")
    void decrementLikeCount(@org.springframework.data.repository.query.Param("id") Long id);

    long countByCustomTopicIdAndStatus(Long customTopicId, CommentStatus status);

    Page<Comment> findByPostIdAndParentCommentIsNullAndStatus(Long postId, CommentStatus status, Pageable pageable);

    Page<Comment> findByCustomTopicIdAndParentCommentIsNullAndStatus(Long customTopicId, CommentStatus status, Pageable pageable);

    Page<Comment> findByCustomTopicIsNotNullAndParentCommentIsNullAndStatus(CommentStatus status, Pageable pageable);

    Page<Comment> findByCustomTopicNameIgnoreCaseAndParentCommentIsNullAndStatus(String topicName, CommentStatus status, Pageable pageable);

    List<Comment> findByParentCommentIdAndStatus(Long parentCommentId, CommentStatus status);

    List<Comment> findByParentCommentIdInAndStatus(java.util.Collection<Long> parentCommentIds, CommentStatus status);

    Optional<Comment> findByIdAndStatus(Long id, CommentStatus status);

    long countByPostIdAndStatus(Long postId, CommentStatus status);

    long countByParentCommentIdAndStatus(Long parentCommentId, CommentStatus status);

    Page<Comment> findByUserId(Long userId, Pageable pageable);

    Page<Comment> findByStatus(CommentStatus status, Pageable pageable);
}
