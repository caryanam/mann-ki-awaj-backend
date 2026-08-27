package com.mka.service.impl;

import com.mka.config.JwtService;
import com.mka.dto.request.*;
import com.mka.dto.response.AuthResponse;
import com.mka.dto.response.ProfileResponse;
import com.mka.entity.Admin;
import com.mka.entity.EmailVerification;
import com.mka.entity.MobileVerification;
import com.mka.entity.Profile;
import com.mka.entity.User;
import com.mka.enums.Role;
import com.mka.exception.ResourceAlreadyExistsException;
import com.mka.exception.ResourceNotFoundException;
import com.mka.exception.UnauthorizedException;
import com.mka.exception.ValidationException;
import com.mka.mapper.ProfileMapper;
import com.mka.repository.AdminRepository;
import com.mka.repository.EmailVerificationRepository;
import com.mka.repository.MobileVerificationRepository;
import com.mka.repository.ProfileRepository;
import com.mka.repository.UserRepository;
import com.mka.service.AuthService;
import com.mka.service.EmailVerificationService;
import com.mka.service.MobileVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import com.mka.entity.PendingRegistration;
import com.mka.repository.PendingRegistrationRepository;
import com.mka.service.EmailService;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final ProfileRepository profileRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final MobileVerificationRepository mobileVerificationRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final MobileVerificationService mobileVerificationService;
    private final EmailService emailService;
    private final ProfileMapper profileMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String mobile = request.getMobileNumber().trim();

        // 1. Check if user already exists in permanent users table
        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (Boolean.TRUE.equals(existingUser.getEmailVerified())) {
                throw new ResourceAlreadyExistsException("Email is already registered by a verified account");
            }
            // Delete old unverified user record so clean pending registration flow is used
            emailVerificationRepository.deleteByUser(existingUser);
            mobileVerificationRepository.deleteByUser(existingUser);
            userRepository.delete(existingUser);
            userRepository.flush();
        }

        // 2. Check if mobile number is used by another user
        Optional<User> userWithMobileOpt = userRepository.findByMobileNumber(mobile);
        if (userWithMobileOpt.isPresent()) {
            User userWithMobile = userWithMobileOpt.get();
            if (Boolean.TRUE.equals(userWithMobile.getEmailVerified()) || Boolean.TRUE.equals(userWithMobile.getMobileVerified())) {
                throw new ResourceAlreadyExistsException("Mobile number is already registered by a verified account");
            }
            // Delete stale unverified record
            emailVerificationRepository.deleteByUser(userWithMobile);
            mobileVerificationRepository.deleteByUser(userWithMobile);
            userRepository.delete(userWithMobile);
            userRepository.flush();
        }

        // Generate 6-digit OTP code
        String otp = String.format("%06d", secureRandom.nextInt(1000000));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(15);

        // Find existing pending registration by email or update it cleanly (No SQL duplicate key errors)
        Optional<PendingRegistration> existingPendingOpt = pendingRegistrationRepository.findByEmail(email);
        PendingRegistration pending;
        if (existingPendingOpt.isPresent()) {
            pending = existingPendingOpt.get();
            pending.setFullName(request.getFullName().trim());
            pending.setMobileNumber(mobile);
            pending.setPassword(passwordEncoder.encode(request.getPassword()));
            pending.setOtp(otp);
            pending.setExpiryTime(expiry);
        } else {
            // Check if mobile number exists in another pending registration record
            Optional<PendingRegistration> pendingMobileOpt = pendingRegistrationRepository.findByMobileNumber(mobile);
            if (pendingMobileOpt.isPresent()) {
                pendingRegistrationRepository.delete(pendingMobileOpt.get());
                pendingRegistrationRepository.flush();
            }

            pending = PendingRegistration.builder()
                    .fullName(request.getFullName().trim())
                    .email(email)
                    .mobileNumber(mobile)
                    .password(passwordEncoder.encode(request.getPassword()))
                    .otp(otp)
                    .expiryTime(expiry)
                    .build();
        }

        pendingRegistrationRepository.save(pending);

        // Send OTP email
        try {
            emailService.sendEmail(
                    email,
                    "Mann Ki Aavaj - Registration Verification OTP",
                    "Your registration verification OTP code is: " + otp + ". Valid for 15 minutes."
            );
        } catch (Exception e) {}

        // DO NOT save to permanent 'users' database table until OTP is verified!
        return AuthResponse.builder()
                .email(email)
                .fullName(request.getFullName().trim())
                .emailVerified(false)
                .mobileVerified(false)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String input = request.getEmail() != null ? request.getEmail().trim() : "";

        // 1. Check Admin repository first
        Optional<Admin> adminOpt = adminRepository.findByEmail(input.toLowerCase());
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            if (!admin.getActive() || admin.getDeleted()) {
                throw new UnauthorizedException("Admin account is deactivated or deleted");
            }
            authenticateCredentials(admin.getEmail(), request.getPassword());
            String token = jwtService.generateToken(admin.getEmail(), admin.getRole().name());

            return AuthResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .userId(admin.getId())
                    .email(admin.getEmail())
                    .fullName(admin.getFullName())
                    .role(admin.getRole())
                    .emailVerified(true)
                    .mobileVerified(true)
                    .build();
        }

        // 2. Check Standard User repository (support both Email & Mobile Number)
        User user = userRepository.findByEmail(input.toLowerCase())
                .orElseGet(() -> userRepository.findByMobileNumber(input).orElse(null));

        if (user == null) {
            throw new ResourceNotFoundException("Account not found");
        }

        if (!user.getActive() || user.getDeleted()) {
            throw new UnauthorizedException("User account is inactive or deleted");
        }

        authenticateCredentials(user.getEmail(), request.getPassword());

        if (!Boolean.TRUE.equals(user.getEmailVerified()) && !Boolean.TRUE.equals(user.getMobileVerified())) {
            throw new UnauthorizedException("Please verify your email or mobile OTP before logging in.");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        Optional<Profile> profileOpt = profileRepository.findByUser(user);
        ProfileResponse profileResponse = profileOpt.map(profileMapper::toResponse).orElse(null);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .emailVerified(user.getEmailVerified())
                .mobileVerified(user.getMobileVerified())
                .profile(profileResponse)
                .build();
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String input = request.getIdentifier() != null ? request.getIdentifier().trim() : "";

        User user = userRepository.findByEmail(input.toLowerCase())
                .orElseGet(() -> userRepository.findByMobileNumber(input).orElse(null));

        if (user == null) {
            throw new ResourceNotFoundException("Account not found");
        }

        // Dispatch OTP via Email or Mobile SMS
        if (input.contains("@")) {
            emailVerificationService.sendVerificationOtp(user);
        } else {
            mobileVerificationService.sendOtp(user);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyForgotPasswordOtp(VerifyForgotPasswordOtpRequest request) {
        String input = request.getIdentifier() != null ? request.getIdentifier().trim() : "";

        User user = userRepository.findByEmail(input.toLowerCase())
                .orElseGet(() -> userRepository.findByMobileNumber(input).orElse(null));

        if (user == null) {
            throw new ResourceNotFoundException("Account not found");
        }

        if (input.contains("@")) {
            EmailVerification verification = emailVerificationRepository.findByUserAndOtpAndUsedFalse(user, request.getOtp().trim())
                    .orElseThrow(() -> new ValidationException("Invalid or expired OTP"));
            if (verification.isExpired()) {
                throw new ValidationException("OTP has expired. Please request a new one.");
            }
        } else {
            MobileVerification verification = mobileVerificationRepository.findByUserAndOtpAndUsedFalse(user, request.getOtp().trim())
                    .orElseThrow(() -> new ValidationException("Invalid or expired mobile OTP"));
            if (verification.isExpired()) {
                throw new ValidationException("Mobile OTP expired. Please request a new one.");
            }
        }
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String input = request.getIdentifier() != null ? request.getIdentifier().trim() : "";

        User user = userRepository.findByEmail(input.toLowerCase())
                .orElseGet(() -> userRepository.findByMobileNumber(input).orElse(null));

        if (user == null) {
            throw new ResourceNotFoundException("Account not found");
        }

        // 1. Verify and consume the OTP
        if (input.contains("@")) {
            emailVerificationService.verifyEmail(new VerifyEmailRequest(user.getEmail(), request.getOtp()));
        } else {
            mobileVerificationService.verifyOtp(new VerifyMobileRequest(user.getMobileNumber(), request.getOtp()));
        }

        // 2. Update password securely
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteAccount(DeleteAccountRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!user.getActive() || user.getDeleted()) {
            throw new UnauthorizedException("User account is inactive or deleted");
        }

        authenticateCredentials(user.getEmail(), request.getPassword());
        user.setActive(false);
        user.setDeleted(true);
        userRepository.save(user);
    }

    private void authenticateCredentials(String email, String password) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        } catch (Exception ex) {
            throw new BadCredentialsException("Invalid email/mobile or password");
        }
    }
}
