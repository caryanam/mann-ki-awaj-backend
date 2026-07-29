package com.mka.repository;

import com.mka.entity.MobileVerification;
import com.mka.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MobileVerificationRepository extends JpaRepository<MobileVerification, Long> {

    Optional<MobileVerification> findByUser(User user);

}