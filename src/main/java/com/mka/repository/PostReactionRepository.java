package com.mka.repository;

import com.mka.entity.PostReaction;
import com.mka.enums.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {

    Optional<PostReaction> findByPostIdAndUserId(Long postId, Long userId);

    List<PostReaction> findByPostId(Long postId);

    long countByPostIdAndReactionType(Long postId, ReactionType reactionType);
}
