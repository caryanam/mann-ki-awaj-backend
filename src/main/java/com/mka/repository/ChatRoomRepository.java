package com.mka.repository;

import com.mka.entity.ChatRoom;
import com.mka.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT r FROM ChatRoom r WHERE (r.participant1 = :user1 AND r.participant2 = :user2) OR (r.participant1 = :user2 AND r.participant2 = :user1)")
    Optional<ChatRoom> findByUsers(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT r FROM ChatRoom r WHERE r.participant1 = :user OR r.participant2 = :user ORDER BY r.updatedAt DESC")
    List<ChatRoom> findByParticipant(@Param("user") User user);
}
