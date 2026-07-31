package com.mka.service.impl;

import com.mka.dto.request.ResendMobileOtpRequest;
import com.mka.dto.request.VerifyMobileRequest;
import com.mka.entity.MobileVerification;
import com.mka.entity.User;
import com.mka.exception.ResourceNotFoundException;
import com.mka.exception.ValidationException;
import com.mka.repository.MobileVerificationRepository;
import com.mka.repository.UserRepository;
import com.mka.service.MobileVerificationService;
import com.mka.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MobileVerificationServiceImpl implements MobileVerificationService {

    private final MobileVerificationRepository mobileVerificationRepository;
    private final UserRepository userRepository;
    private final SmsService smsService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public void sendOtp(User user) {
        String otp = String.format("%06d", secureRandom.nextInt(1000000));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(10);

        MobileVerification verification = MobileVerification.builder()
                .user(user)
                .otp(otp)
                .expiryTime(expiry)
                .used(false)
                .build();

        mobileVerificationRepository.save(verification);

        try {
            smsService.sendSms(
                    user.getMobileNumber(),
                    "Mann Ki Aavaj OTP is: " + otp + ". Valid for 10 mins."
            );
        } catch (Exception e) {
            // Log SMS delivery exception
        }
    }

    @Override
    @Transactional
    public void verifyOtp(VerifyMobileRequest request) {
        User user = userRepository.findByMobileNumber(request.getMobileNumber().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with mobile: " + request.getMobileNumber()));

        MobileVerification verification = mobileVerificationRepository.findByUserAndOtpAndUsedFalse(user, request.getOtp().trim())
                .orElseThrow(() -> new ValidationException("Invalid or expired mobile OTP"));

        if (verification.isExpired()) {
            throw new ValidationException("Mobile OTP expired. Please request a new one.");
        }

        verification.setUsed(true);
        mobileVerificationRepository.save(verification);

        user.setMobileVerified(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void resendOtp(ResendMobileOtpRequest request) {
        User user = userRepository.findByMobileNumber(request.getMobileNumber().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with mobile: " + request.getMobileNumber()));

        sendOtp(user);
    }
}
