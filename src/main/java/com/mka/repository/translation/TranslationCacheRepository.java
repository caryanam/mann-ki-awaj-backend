package com.mka.repository.translation;

import com.mka.entity.translation.TranslationCache;
import com.mka.enums.translation.EntityType;
import com.mka.enums.translation.SupportedLanguage;
import com.mka.enums.translation.TranslationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for TranslationCache entities.
 */
@Repository
public interface TranslationCacheRepository extends JpaRepository<TranslationCache, Long> {

    Optional<TranslationCache> findByEntityTypeAndEntityIdAndTargetLanguage(
            EntityType entityType,
            Long entityId,
            SupportedLanguage targetLanguage
    );

    Optional<TranslationCache> findByEntityTypeAndEntityIdAndTargetLanguageAndStatus(
            EntityType entityType,
            Long entityId,
            SupportedLanguage targetLanguage,
            TranslationStatus status
    );

    boolean existsByEntityTypeAndEntityIdAndTargetLanguage(
            EntityType entityType,
            Long entityId,
            SupportedLanguage targetLanguage
    );

    void deleteByEntityTypeAndEntityId(EntityType entityType, Long entityId);

    List<TranslationCache> findByExpiresAtBefore(LocalDateTime now);
}
