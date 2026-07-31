package com.mka.repository;

import com.mka.entity.EmailVerification;
import com.mka.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByUser(User user);

    Optional<EmailVerification> findByUserAndOtp(User user, String otp);

    Optional<EmailVerification> findTopByUserOrderByCreatedAtDesc(User user);

    Optional<EmailVerification> findByUserAndOtpAndUsedFalse(User user, String otp);

    boolean existsByUser(User user);
}
