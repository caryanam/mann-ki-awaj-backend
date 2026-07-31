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
}
