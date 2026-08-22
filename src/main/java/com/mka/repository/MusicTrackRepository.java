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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from MusicTrack t where t.id = :id and t.status <> com.mka.enums.MusicTrackStatus.DELETED")
    Optional<MusicTrack> findActiveByIdForUpdate(@Param("id") Long id);
}
