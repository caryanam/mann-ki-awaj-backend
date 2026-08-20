package com.mka.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "user_mutes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_muter_muted", columnNames = {"muter_user_id", "muted_username"})
}, indexes = {
        @Index(name = "idx_user_mutes_muter_id", columnList = "muter_user_id"),
        @Index(name = "idx_user_mutes_muted_username", columnList = "muted_username")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserMute extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "muter_user_id", nullable = false)
    private User muter;

    @Column(name = "muted_username", nullable = false, length = 50)
    private String mutedUsername;
}
