package com.mka.entity.translation;

import com.mka.enums.translation.EntityType;
import com.mka.enums.translation.SupportedLanguage;
import com.mka.enums.translation.TranslationProvider;
import com.mka.enums.translation.TranslationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing cached translation entries for database persistence.
 * Unique constraint on (entity_type, entity_id, target_language).
 */
@Entity
@Table(
    name = "translation_cache",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_entity_type_id_target_lang",
            columnNames = {"entity_type", "entity_id", "target_language"}
        )
    },
    indexes = {
        @Index(name = "idx_translation_lookup", columnList = "entity_type, entity_id, target_language"),
        @Index(name = "idx_translation_status", columnList = "status"),
        @Index(name = "idx_translation_expires_at", columnList = "expires_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_language", nullable = false, length = 10)
    private SupportedLanguage sourceLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_language", nullable = false, length = 10)
    private SupportedLanguage targetLanguage;

    @Column(name = "translated_text", nullable = false, columnDefinition = "TEXT")
    private String translatedText;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private TranslationProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TranslationStatus status;

    @Builder.Default
    @Column(name = "translation_version")
    private Integer translationVersion = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    public void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.translationVersion == null) {
            this.translationVersion = 1;
        }
        if (this.status == null) {
            this.status = TranslationStatus.COMPLETED;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
