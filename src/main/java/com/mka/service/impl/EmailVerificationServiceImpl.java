package com.mka.service.impl;

import com.mka.dto.request.ResendVerificationRequest;
import com.mka.dto.request.VerifyEmailRequest;
import com.mka.entity.EmailVerification;
import com.mka.entity.User;
import com.mka.exception.ResourceNotFoundException;
import com.mka.exception.ValidationException;
import com.mka.repository.EmailVerificationRepository;
import com.mka.repository.UserRepository;
import com.mka.service.EmailService;
import com.mka.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public void sendVerificationOtp(User user) {
        String otp = String.format("%06d", secureRandom.nextInt(1000000));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(15);

        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .otp(otp)
                .expiryTime(expiry)
                .used(false)
                .build();

        emailVerificationRepository.save(verification);

        try {
            emailService.sendEmail(
                    user.getEmail(),
                    "Mann Ki Aavaj - Email Verification OTP",
                    "Your email verification OTP code is: " + otp + ". Valid for 15 minutes."
            );
        } catch (Exception e) {
            // Log email sending error
        }
    }

    @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        EmailVerification verification = emailVerificationRepository.findByUserAndOtpAndUsedFalse(user, request.getOtp().trim())
                .orElseThrow(() -> new ValidationException("Invalid or expired OTP"));

        if (verification.isExpired()) {
            throw new ValidationException("OTP has expired. Please request a new one.");
        }

        verification.setUsed(true);
        emailVerificationRepository.save(verification);

        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void resendVerificationOtp(ResendVerificationRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        sendVerificationOtp(user);
    }
}
