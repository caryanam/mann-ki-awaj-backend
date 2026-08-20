package com.mka.repository;

import com.mka.entity.UserMoodVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserMoodVoteRepository extends JpaRepository<UserMoodVote, Long> {

    Optional<UserMoodVote> findByUserId(Long userId);

    @Query("SELECT v.mood, COUNT(v) FROM UserMoodVote v GROUP BY v.mood")
    List<Object[]> countVotesByMood();
}
