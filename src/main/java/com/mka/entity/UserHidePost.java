package com.mka.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "user_hidden_posts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_hidden_post", columnNames = {"user_id", "post_id"})
}, indexes = {
        @Index(name = "idx_user_hidden_user_id", columnList = "user_id"),
        @Index(name = "idx_user_hidden_post_id", columnList = "post_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserHidePost extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "post_title", length = 255)
    private String postTitle;

    @Column(name = "author_username", length = 50)
    private String authorUsername;
}
