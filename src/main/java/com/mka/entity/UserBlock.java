package com.mka.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "user_blocks", uniqueConstraints = {
        @UniqueConstraint(name = "uk_blocker_blocked", columnNames = {"blocker_user_id", "blocked_username"})
}, indexes = {
        @Index(name = "idx_user_blocks_blocker_id", columnList = "blocker_user_id"),
        @Index(name = "idx_user_blocks_blocked_username", columnList = "blocked_username")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserBlock extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_user_id", nullable = false)
    private User blocker;

    @Column(name = "blocked_username", nullable = false, length = 50)
    private String blockedUsername;
}
