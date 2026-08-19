package com.mka.repository;

import com.mka.entity.CommentReaction;
import com.mka.enums.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {

    Optional<CommentReaction> findByCommentIdAndUserId(Long commentId, Long userId);

    List<CommentReaction> findByCommentId(Long commentId);

    long countByCommentIdAndReactionType(Long commentId, ReactionType reactionType);

    public interface CommentReactionCountProjection {
        Long getCommentId();
        ReactionType getReactionType();
        Long getCount();
    }

    @org.springframework.data.jpa.repository.Query("SELECT r.comment.id AS commentId, r.reactionType AS reactionType, COUNT(r) AS count FROM CommentReaction r WHERE r.comment.id IN :commentIds GROUP BY r.comment.id, r.reactionType")
    List<CommentReactionCountProjection> findReactionCountsByCommentIdIn(@org.springframework.data.repository.query.Param("commentIds") List<Long> commentIds);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM CommentReaction r WHERE r.user.id = :userId AND r.comment.id IN :commentIds")
    List<CommentReaction> findByUserIdAndCommentIdIn(@org.springframework.data.repository.query.Param("userId") Long userId, @org.springframework.data.repository.query.Param("commentIds") List<Long> commentIds);
}
