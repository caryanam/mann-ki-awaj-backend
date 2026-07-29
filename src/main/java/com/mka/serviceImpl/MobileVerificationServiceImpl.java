package com.mka.serviceImpl;

import com.mka.dto.request.ResendMobileOtpRequest;
import com.mka.dto.request.VerifyMobileRequest;
import com.mka.entity.MobileVerification;
import com.mka.entity.User;
import com.mka.exception.BadRequestException;
import com.mka.exception.ResourceNotFoundException;
import com.mka.repository.MobileVerificationRepository;
import com.mka.repository.UserRepository;
import com.mka.service.MobileVerificationService;
import com.mka.service.SmsService;
import com.mka.util.OtpGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class MobileVerificationServiceImpl implements MobileVerificationService {

    private final UserRepository userRepository;
    private final MobileVerificationRepository mobileVerificationRepository;
    private final SmsService smsService;

    @Override
    public void sendOtp(User user) {

        System.out.println("1. Mobile OTP Generation Started");

        String otp = OtpGenerator.generateOtp();

        System.out.println("2. Mobile OTP Generated: " + otp);

        MobileVerification verification = mobileVerificationRepository
                .findByUser(user)
                .orElse(
                        MobileVerification.builder()
                                .user(user)
                                .build()
                );

        verification.setOtp(otp);
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        verification.setVerified(false);
        verification.setUsed(false);

        mobileVerificationRepository.save(verification);

        System.out.println("3. Mobile Verification Saved");

        smsService.sendOtp(user.getMobileNumber(), otp);

        System.out.println("4. SMS Sent Successfully");
    }

    @Override
    public void verifyOtp(VerifyMobileRequest request) {

        User user = userRepository.findByMobileNumber(request.getMobileNumber())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with mobile number: " + request.getMobileNumber()));

        MobileVerification verification = mobileVerificationRepository
                .findByUser(user)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Mobile verification record not found."));

        if (Boolean.TRUE.equals(user.getMobileVerified())) {
            throw new BadRequestException("Mobile number is already verified.");
        }

        if (!verification.getOtp().equals(request.getOtp())) {
            throw new BadRequestException("Invalid OTP.");
        }

        if (Boolean.TRUE.equals(verification.getUsed())) {
            throw new BadRequestException("OTP has already been used.");
        }

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired.");
        }

        user.setMobileVerified(true);

        verification.setVerified(true);
        verification.setUsed(true);

        userRepository.save(user);
        mobileVerificationRepository.save(verification);
    }

    @Override
    public void resendOtp(ResendMobileOtpRequest request) {

        User user = userRepository.findByMobileNumber(request.getMobileNumber())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with mobile number: " + request.getMobileNumber()));

        if (Boolean.TRUE.equals(user.getMobileVerified())) {
            throw new BadRequestException("Mobile number is already verified.");
        }

        MobileVerification verification = mobileVerificationRepository
                .findByUser(user)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Mobile verification record not found."));

        String otp = OtpGenerator.generateOtp();

        verification.setOtp(otp);
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        verification.setVerified(false);
        verification.setUsed(false);

        mobileVerificationRepository.save(verification);

        smsService.sendOtp(user.getMobileNumber(), otp);
    }
}