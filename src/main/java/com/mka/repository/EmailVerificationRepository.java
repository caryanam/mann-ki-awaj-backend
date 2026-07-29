package com.mka.repository;

import com.mka.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import com.mka.entity.User;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByUser(User user);

    Optional<EmailVerification> findByUserAndOtp(User user, String otp);

    boolean existsByUser(User user);
}
