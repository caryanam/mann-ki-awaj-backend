package com.mka.repository;

import com.mka.entity.PostReaction;
import com.mka.enums.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {

    Optional<PostReaction> findByPostIdAndUserId(Long postId, Long userId);

    List<PostReaction> findByPostId(Long postId);

    long countByPostIdAndReactionType(Long postId, ReactionType reactionType);

    public interface PostReactionCountProjection {
        Long getPostId();
        ReactionType getReactionType();
        Long getCount();
    }

    @Query("SELECT r.post.id AS postId, r.reactionType AS reactionType, COUNT(r) AS count FROM PostReaction r WHERE r.post.id IN :postIds GROUP BY r.post.id, r.reactionType")
    List<PostReactionCountProjection> findReactionCountsByPostIdIn(@Param("postIds") List<Long> postIds);

    @Query("SELECT r FROM PostReaction r WHERE r.user.id = :userId AND r.post.id IN :postIds")
    List<PostReaction> findByUserIdAndPostIdIn(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}
