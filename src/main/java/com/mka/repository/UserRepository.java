package com.mka.repository;

import com.mka.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByMobileNumber(String mobileNumber);

    Optional<User> findByMobileNumber(String mobileNumber);

    long countByActiveFalse();

    long countByActiveTrue();

    org.springframework.data.domain.Page<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrMobileNumberContainingIgnoreCase(String fullName, String email, String mobileNumber, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    int deleteByDeletedTrueAndUpdatedAtBefore(java.time.LocalDateTime dateTime);
}
