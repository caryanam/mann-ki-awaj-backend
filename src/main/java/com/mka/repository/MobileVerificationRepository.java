package com.mka.repository;

import com.mka.entity.MobileVerification;
import com.mka.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MobileVerificationRepository extends JpaRepository<MobileVerification, Long> {

    Optional<MobileVerification> findByUser(User user);

    Optional<MobileVerification> findTopByUserOrderByCreatedAtDesc(User user);

    Optional<MobileVerification> findByUserAndOtpAndUsedFalse(User user, String otp);

    boolean existsByUser(User user);

    void deleteByUser(User user);
}