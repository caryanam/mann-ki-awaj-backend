package com.mka.serviceImpl;

import com.mka.dto.request.ResendVerificationRequest;
import com.mka.dto.request.VerifyEmailRequest;
import com.mka.entity.EmailVerification;
import com.mka.entity.User;
import com.mka.exception.BadRequestException;
import com.mka.exception.ResourceNotFoundException;
import com.mka.repository.EmailVerificationRepository;
import com.mka.repository.UserRepository;
import com.mka.service.EmailService;
import com.mka.service.EmailVerificationService;
import com.mka.util.OtpGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailService emailService;

    @Override
    public void sendVerificationOtp(User user) {
        System.out.println("1. OTP Generation Started");
        String otp = OtpGenerator.generateOtp();
        System.out.println("2. OTP Generated: " + otp);
        EmailVerification verification = emailVerificationRepository
                .findByUser(user)
                .orElse(
                        EmailVerification.builder()
                                .user(user)
                                .build()
                );

        verification.setOtp(otp);
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        verification.setVerified(false);
        verification.setUsed(false);

        emailVerificationRepository.save(verification);
        System.out.println("3. Verification Saved");

        emailService.sendVerificationOtp(user.getEmail(), otp);
        System.out.println("4. Email Sent Successfully");
    }

    @Override
    public void verifyEmail(VerifyEmailRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with email: " + request.getEmail()));

        EmailVerification verification = emailVerificationRepository
                .findByUser(user)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Verification record not found."));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Email is already verified.");
        }

        if (!verification.getOtp().equals(request.getOtp())) {
            throw new BadRequestException("Invalid OTP.");
        }

        if (verification.getUsed()) {
            throw new BadRequestException("OTP has already been used.");
        }

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired.");
        }

        user.setEmailVerified(true);

        verification.setVerified(true);
        verification.setUsed(true);

        userRepository.save(user);
        emailVerificationRepository.save(verification);
    }

    @Override
    public void resendVerificationOtp(ResendVerificationRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with email: " + request.getEmail()));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Email is already verified.");
        }

        EmailVerification verification = emailVerificationRepository
                .findByUser(user)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Verification record not found."));

        String otp = OtpGenerator.generateOtp();

        verification.setOtp(otp);
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        verification.setUsed(false);
        verification.setVerified(false);

        emailVerificationRepository.save(verification);

        emailService.sendVerificationOtp(user.getEmail(), otp);
    }
}