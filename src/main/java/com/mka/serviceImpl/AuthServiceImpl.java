package com.mka.serviceImpl;







import com.mka.config.JwtService;

import com.mka.dto.request.LoginRequest;

import com.mka.dto.request.RegisterRequest;



import com.mka.dto.responce.AuthResponse;

import com.mka.dto.responce.LoginResponseDTO;

import com.mka.entity.Admin;
import com.mka.entity.User;

import com.mka.enums.Role;


import com.mka.exception.ResourceAlreadyExistsException;
import com.mka.repository.AdminRepository;
import com.mka.repository.UserRepository;

import com.mka.service.AuthService;

import com.mka.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;

import org.springframework.security.authentication.BadCredentialsException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

import java.util.Optional;


@Service

@RequiredArgsConstructor

public class AuthServiceImpl implements AuthService {



    private final UserRepository userRepository;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final PasswordEncoder passwordEncoder;

    private  final AdminRepository adminRepository;

    //for Email verification
    private final EmailVerificationService emailVerificationService;



    @Override

    public LoginResponseDTO register(RegisterRequest request) {



        if (userRepository.existsByEmail(request.getEmail())) {

            throw new ResourceAlreadyExistsException("Email already exists");

        }



        if (userRepository.existsByMobileNumber(request.getMobileNumber())) {

            throw new ResourceAlreadyExistsException("Mobile number already exists");

        }



        User user = User.builder()

                .fullName(request.getFullName())

                .email(request.getEmail())

                .mobileNumber(request.getMobileNumber())

                .password(passwordEncoder.encode(request.getPassword()))

                .role(Role.USER)

                .active(true)

                .deleted(false)

                .build();



        userRepository.save(user);



      /*  return LoginResponseDTO.builder()

                .id(user.getId())

                .fullName(user.getFullName())

                .email(user.getEmail())

                .mobileNumber(user.getMobileNumber())

                .role(user.getRole())

                .build();

       */

        User savedUser = userRepository.save(user);

// Send verification OTP
        emailVerificationService.sendVerificationOtp(savedUser);

        return LoginResponseDTO.builder()
                .id(savedUser.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .mobileNumber(savedUser.getMobileNumber())
                .role(savedUser.getRole())
                .build();

    }



    @Override
    public AuthResponse login(LoginRequest request) {




        Optional<Admin> adminOptional = adminRepository.findByEmail(request.getEmail());

        if (adminOptional.isPresent()) {

            Admin admin = adminOptional.get();

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            if (!admin.getActive()) {
                throw new RuntimeException("Your account is inactive.");
            }

            String token = jwtService.generateToken(admin.getEmail(),admin.getRole().name());

            return AuthResponse.builder()
                    .id(admin.getId())
                    .fullName(admin.getFullName())
                    .email(admin.getEmail())
                    .mobileNumber(admin.getMobileNumber())
                    .role(admin.getRole())
                    .token(token)
                    .build();
        }


        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid Email or Password"));

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

       /* if (!user.getActive()) {
            throw new RuntimeException("Your account is inactive.");
        }

        String token = jwtService.generateToken(user.getEmail(),user.getRole().name()); */

        if (!user.getActive()) {
            throw new RuntimeException("Your account is inactive.");
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new RuntimeException("Please verify your email before logging in.");
        }

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole().name()
        );

        return AuthResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .role(user.getRole())
                .token(token)
                .build();
    }

}
