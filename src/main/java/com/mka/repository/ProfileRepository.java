package com.mka.repository;

import com.mka.entity.Profile;
import com.mka.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByUserId(Long userId);

    Optional<Profile> findByUser(User user);

    Optional<Profile> findByUsername(String username);

    boolean existsByUsername(String username);
}
