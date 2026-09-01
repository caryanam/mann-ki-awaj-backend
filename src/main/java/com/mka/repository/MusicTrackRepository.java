package com.mka.repository;

import com.mka.entity.MusicTrack;
import com.mka.enums.MusicTrackStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.mka.enums.MusicTrackSource;

public interface MusicTrackRepository extends JpaRepository<MusicTrack, Long>, JpaSpecificationExecutor<MusicTrack> {

    Optional<MusicTrack> findByIdAndStatus(Long id, MusicTrackStatus status);
    Optional<MusicTrack> findByIdAndStatusNot(Long id, MusicTrackStatus status);
    Page<MusicTrack> findByUploadedByIdAndSourceAndStatusNot(Long uploaderId, MusicTrackSource source,
                                                             MusicTrackStatus excludedStatus, Pageable pageable);
    Page<MusicTrack> findByUploadedByIdAndSourceAndStatus(Long uploaderId, MusicTrackSource source,
                                                          MusicTrackStatus status, Pageable pageable);
    long countByUploadedByIdAndSourceAndStatus(Long uploaderId, MusicTrackSource source, MusicTrackStatus status);

    @Query("SELECT COUNT(t) FROM MusicTrack t WHERE t.uploadedBy.id = :uploadedBy AND t.source = :source AND t.status <> com.mka.enums.MusicTrackStatus.DELETED")
    long countByUploadedByIdAndSourceAndStatusNotDeleted(@Param("uploadedBy") Long uploadedBy, @Param("source") MusicTrackSource source);

    default long countByUploadedByAndSource(Long uploadedBy, String source) {
        if (uploadedBy == null || source == null) return 0L;
        try {
            MusicTrackSource trackSource = MusicTrackSource.valueOf(source.trim().toUpperCase());
            return countByUploadedByIdAndSourceAndStatusNotDeleted(uploadedBy, trackSource);
        } catch (IllegalArgumentException e) {
            return 0L;
        }
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from MusicTrack t where t.id = :id and t.status <> com.mka.enums.MusicTrackStatus.DELETED")
    Optional<MusicTrack> findActiveByIdForUpdate(@Param("id") Long id);
}
