package com.mka.repository;

import com.mka.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);

    long countByPostId(Long postId);

    @Query("SELECT l.post.id FROM PostLike l WHERE l.user.id = :userId AND l.post.id IN :postIds")
    List<Long> findLikedPostIdsByUserIdAndPostIdIn(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}
