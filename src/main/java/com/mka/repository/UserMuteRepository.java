package com.mka.repository;

import com.mka.entity.UserMute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserMuteRepository extends JpaRepository<UserMute, Long> {
    List<UserMute> findByMuterId(Long muterId);

    Optional<UserMute> findByMuterIdAndMutedUsernameIgnoreCase(Long muterId, String mutedUsername);

    boolean existsByMuterIdAndMutedUsernameIgnoreCase(Long muterId, String mutedUsername);

    @Modifying
    @Query("DELETE FROM UserMute m WHERE m.muter.id = :muterId AND LOWER(m.mutedUsername) = LOWER(:mutedUsername)")
    void deleteByMuterIdAndMutedUsernameIgnoreCase(@Param("muterId") Long muterId, @Param("mutedUsername") String mutedUsername);
}
