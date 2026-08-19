package com.mka.repository;

import com.mka.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    boolean existsByCommentIdAndUserId(Long commentId, Long userId);

    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);

    long countByCommentId(Long commentId);

    @org.springframework.data.jpa.repository.Query("SELECT l.comment.id FROM CommentLike l WHERE l.user.id = :userId AND l.comment.id IN :commentIds")
    java.util.List<Long> findLikedCommentIdsByUserIdAndCommentIdIn(@org.springframework.data.repository.query.Param("userId") Long userId, @org.springframework.data.repository.query.Param("commentIds") java.util.List<Long> commentIds);
}
