package com.mka.repository;

import com.mka.entity.UserHidePost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserHidePostRepository extends JpaRepository<UserHidePost, Long> {
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    void deleteByUserIdAndPostId(Long userId, Long postId);
    List<UserHidePost> findByUserIdOrderByCreatedAtDesc(Long userId);
}
