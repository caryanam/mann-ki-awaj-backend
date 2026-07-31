package com.mka.service.impl;

import com.mka.config.JwtService;
import com.mka.dto.request.LoginRequest;
import com.mka.dto.request.RegisterRequest;
import com.mka.dto.response.AuthResponse;
import com.mka.dto.response.ProfileResponse;
import com.mka.entity.Admin;
import com.mka.entity.Profile;
import com.mka.entity.User;
import com.mka.enums.Role;
import com.mka.exception.ResourceAlreadyExistsException;
import com.mka.exception.UnauthorizedException;
import com.mka.mapper.ProfileMapper;
import com.mka.repository.AdminRepository;
import com.mka.repository.ProfileRepository;
import com.mka.repository.UserRepository;
import com.mka.service.AuthService;
import com.mka.service.EmailVerificationService;
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
    private final ProfileMapper profileMapper;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new ResourceAlreadyExistsException("Email is already registered");
        }

        if (userRepository.existsByMobileNumber(request.getMobileNumber().trim())) {
            throw new ResourceAlreadyExistsException("Mobile number is already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .mobileNumber(request.getMobileNumber().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .active(true)
                .deleted(false)
                .emailVerified(false)
                .mobileVerified(false)
                .build();

        User savedUser = userRepository.save(user);

        // Dispatch verification OTP via email
        try {
            emailVerificationService.sendVerificationOtp(savedUser);
        } catch (Exception e) {
            // Log non-blocking email delivery failure during initial registration
        }

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
        String email = request.getEmail().trim().toLowerCase();

        // 1. Check Admin repository first
        Optional<Admin> adminOpt = adminRepository.findByEmail(email);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            if (!admin.getActive() || admin.getDeleted()) {
                throw new UnauthorizedException("Admin account is deactivated or deleted");
            }
            authenticateCredentials(email, request.getPassword());
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

        // 2. Check Standard User repository
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.getActive() || user.getDeleted()) {
            throw new UnauthorizedException("User account is inactive or deleted");
        }

        authenticateCredentials(email, request.getPassword());

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

    private void authenticateCredentials(String email, String password) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        } catch (Exception ex) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }
}
