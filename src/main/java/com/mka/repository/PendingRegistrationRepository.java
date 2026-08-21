package com.mka.repository;

import com.mka.entity.PendingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, Long> {
    Optional<PendingRegistration> findByEmail(String email);
    Optional<PendingRegistration> findByEmailAndOtp(String email, String otp);
    Optional<PendingRegistration> findByMobileNumber(String mobileNumber);
    void deleteByEmail(String email);
}
