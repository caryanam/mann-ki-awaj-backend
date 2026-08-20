package com.mka.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "profiles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_profiles_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_profiles_user_id", columnNames = "user_id")
}, indexes = {
        @Index(name = "idx_profiles_username", columnList = "username")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Profile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "username", nullable = false, unique = true, length = 30)
    private String username;

    @Column(name = "avatar", nullable = false, length = 100)
    @Builder.Default
    private String avatar = "#6F405F";

    @Column(name = "preferred_language", nullable = false, length = 10)
    @Builder.Default
    private String preferredLanguage = "EN";

    @Column(name = "bio", length = 250)
    private String bio;

    @Column(name = "last_seen")
    private java.time.LocalDateTime lastSeen;

    @Column(name = "username_change_count")
    @Builder.Default
    private Integer usernameChangeCount = 0;

    @Column(name = "username_last_changed_at")
    private java.time.LocalDateTime usernameLastChangedAt;
}
