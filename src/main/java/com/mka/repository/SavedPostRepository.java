package com.mka.repository;

import com.mka.entity.SavedPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedPostRepository extends JpaRepository<SavedPost, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    Optional<SavedPost> findByUserIdAndPostId(Long userId, Long postId);

    Page<SavedPost> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT s.post.id FROM SavedPost s WHERE s.user.id = :userId AND s.post.id IN :postIds")
    List<Long> findSavedPostIdsByUserIdAndPostIdIn(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}
