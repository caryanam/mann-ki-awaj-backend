package com.mka.repository;

import com.mka.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {
    boolean existsByBlockerIdAndBlockedUsernameIgnoreCase(Long blockerId, String blockedUsername);
    void deleteByBlockerIdAndBlockedUsernameIgnoreCase(Long blockerId, String blockedUsername);
    List<UserBlock> findByBlockerId(Long blockerId);
}
