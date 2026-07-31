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

    Page<Comment> findByPostIdAndParentCommentIsNullAndStatus(Long postId, CommentStatus status, Pageable pageable);

    List<Comment> findByParentCommentIdAndStatus(Long parentCommentId, CommentStatus status);

    Optional<Comment> findByIdAndStatus(Long id, CommentStatus status);

    long countByPostIdAndStatus(Long postId, CommentStatus status);

    Page<Comment> findByUserId(Long userId, Pageable pageable);

    Page<Comment> findByStatus(CommentStatus status, Pageable pageable);
}
