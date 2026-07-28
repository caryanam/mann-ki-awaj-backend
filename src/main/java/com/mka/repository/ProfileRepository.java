package com.mka.repository;

import com.mka.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile,Long> {
    Optional<Profile> findByUserId(Long userId);

    Optional<Profile> findByUsername(String username);

    boolean existsByUsername(String username);
}
