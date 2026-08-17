package com.mka.service.impl;

import com.mka.config.JwtService;
import com.mka.dto.request.*;
import com.mka.dto.response.AuthResponse;
import com.mka.dto.response.ProfileResponse;
import com.mka.entity.Admin;
import com.mka.entity.Profile;
import com.mka.entity.User;
import com.mka.enums.Role;
import com.mka.exception.ResourceAlreadyExistsException;
import com.mka.exception.ResourceNotFoundException;
import com.mka.exception.UnauthorizedException;
import com.mka.mapper.ProfileMapper;
import com.mka.repository.AdminRepository;
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

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final ProfileRepository profileRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final MobileVerificationService mobileVerificationService;
    private final ProfileMapper profileMapper;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String mobile = request.getMobileNumber().trim();

        // 1. Check if user already exists by email
        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (Boolean.TRUE.equals(existingUser.getEmailVerified())) {
                throw new ResourceAlreadyExistsException("Email is already registered");
            }

            // Unverified user - check mobile number conflicts with other verified users
            Optional<User> userWithMobileOpt = userRepository.findByMobileNumber(mobile);
            if (userWithMobileOpt.isPresent() && !userWithMobileOpt.get().getEmail().equals(email)) {
                if (Boolean.TRUE.equals(userWithMobileOpt.get().getEmailVerified())) {
                    throw new ResourceAlreadyExistsException("Mobile number is already registered by another verified user");
                }
            }

            // Update registration details and password
            existingUser.setFullName(request.getFullName().trim());
            existingUser.setMobileNumber(mobile);
            existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
            existingUser.setActive(true);
            existingUser.setDeleted(false);

            User savedUser = userRepository.save(existingUser);

            try {
                emailVerificationService.sendVerificationOtp(savedUser);
            } catch (Exception e) {}

            return AuthResponse.builder()
                    .userId(savedUser.getId())
                    .email(savedUser.getEmail())
                    .fullName(savedUser.getFullName())
                    .role(savedUser.getRole())
                    .emailVerified(savedUser.getEmailVerified())
                    .mobileVerified(savedUser.getMobileVerified())
                    .build();
        }

        // 2. Check if mobile number is used by someone else
        Optional<User> userWithMobileOpt = userRepository.findByMobileNumber(mobile);
        if (userWithMobileOpt.isPresent()) {
            User userWithMobile = userWithMobileOpt.get();
            if (Boolean.TRUE.equals(userWithMobile.getEmailVerified())) {
                throw new ResourceAlreadyExistsException("Mobile number is already registered");
            }
            throw new ResourceAlreadyExistsException("Mobile number is already registered by an unverified account. Please use the original email to verify.");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .mobileNumber(mobile)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .active(true)
                .deleted(false)
                .emailVerified(false)
                .mobileVerified(false)
                .build();

        User savedUser = userRepository.save(user);

        try {
            emailVerificationService.sendVerificationOtp(savedUser);
        } catch (Exception e) {}

        return AuthResponse.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole())
                .emailVerified(savedUser.getEmailVerified())
                .mobileVerified(savedUser.getMobileVerified())
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
    @Transactional
    public void verifyForgotPasswordOtp(VerifyForgotPasswordOtpRequest request) {
        String input = request.getIdentifier() != null ? request.getIdentifier().trim() : "";

        User user = userRepository.findByEmail(input.toLowerCase())
                .orElseGet(() -> userRepository.findByMobileNumber(input).orElse(null));

        if (user == null) {
            throw new ResourceNotFoundException("Account not found");
        }

        if (input.contains("@")) {
            emailVerificationService.verifyEmail(new VerifyEmailRequest(user.getEmail(), request.getOtp()));
        } else {
            mobileVerificationService.verifyOtp(new VerifyMobileRequest(user.getMobileNumber(), request.getOtp()));
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

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
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
