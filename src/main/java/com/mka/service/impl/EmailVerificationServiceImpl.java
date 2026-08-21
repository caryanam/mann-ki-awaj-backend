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
import com.mka.entity.PendingRegistration;
import com.mka.repository.PendingRegistrationRepository;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;
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
        String email = request.getEmail().trim().toLowerCase();
        String otp = request.getOtp().trim();

        // 1. Check PendingRegistration table first (User is ONLY created in DB after OTP verification)
        Optional<PendingRegistration> pendingOpt = pendingRegistrationRepository.findByEmailAndOtp(email, otp);
        if (pendingOpt.isPresent()) {
            PendingRegistration pending = pendingOpt.get();
            if (pending.isExpired()) {
                throw new ValidationException("OTP code has expired. Please request a new OTP.");
            }

            // Create permanent User in 'users' database table ONLY NOW AFTER OTP VERIFICATION!
            User user = User.builder()
                    .fullName(pending.getFullName())
                    .email(pending.getEmail())
                    .mobileNumber(pending.getMobileNumber())
                    .password(pending.getPassword())
                    .role(com.mka.enums.Role.USER)
                    .active(true)
                    .deleted(false)
                    .emailVerified(true)
                    .mobileVerified(true)
                    .build();

            userRepository.save(user);

            // Clean up pending registration
            pendingRegistrationRepository.delete(pending);
            return;
        }

        // 2. Fallback for existing user verification
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ValidationException("Invalid or expired OTP code"));

        EmailVerification verification = emailVerificationRepository.findByUserAndOtpAndUsedFalse(user, otp)
                .orElseThrow(() -> new ValidationException("Invalid or expired OTP code"));

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
        String email = request.getEmail().trim().toLowerCase();
        Optional<PendingRegistration> pendingOpt = pendingRegistrationRepository.findByEmail(email);

        if (pendingOpt.isPresent()) {
            PendingRegistration pending = pendingOpt.get();
            String otp = String.format("%06d", secureRandom.nextInt(1000000));
            pending.setOtp(otp);
            pending.setExpiryTime(LocalDateTime.now().plusMinutes(15));
            pendingRegistrationRepository.save(pending);

            try {
                emailService.sendEmail(
                        email,
                        "Mann Ki Aavaj - Registration Verification OTP",
                        "Your registration verification OTP code is: " + otp + ". Valid for 15 minutes."
                );
            } catch (Exception e) {}
            return;
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        sendVerificationOtp(user);
    }
}
