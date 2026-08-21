package com.mka.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "custom_topics", indexes = {
        @Index(name = "idx_custom_topics_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CustomTopic extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "label", length = 100)
    private String label;

    @Column(name = "icon", length = 20)
    @Builder.Default
    private String icon = "💡";

    @Column(name = "created_by_username", length = 100)
    private String createdByUsername;

    @Column(name = "post_count")
    @Builder.Default
    private Long postCount = 0L;
}
