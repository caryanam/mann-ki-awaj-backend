package com.mka.controller;



import com.mka.dto.request.*;

import com.mka.dto.responce.ApiResponse;

import com.mka.dto.responce.AuthResponse;

import com.mka.dto.responce.LoginResponseDTO;

import com.mka.service.AuthService;

import com.mka.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;
import com.mka.service.MobileVerificationService;



@RestController

@RequestMapping("/api/auth")

@Tag(name = "Authentication", description = "Authentication APIs")

@RequiredArgsConstructor

public class AuthController {


    private final EmailVerificationService emailVerificationService;
    private final MobileVerificationService mobileVerificationService;

    private final AuthService authService;



    @PostMapping("/register")

    @Operation(summary = "User Registration")

    public ResponseEntity<ApiResponse<LoginResponseDTO>> register(

            @Valid @RequestBody RegisterRequest request) {



        return ResponseEntity.ok(

                ApiResponse.<LoginResponseDTO>builder()

                        .success(true)

                        .message("User Registered Successfully")

                        .data(authService.register(request))

                        .build()

        );

    }



    @PostMapping("/login")

    @Operation(summary = "All Login")

    public ResponseEntity<ApiResponse<AuthResponse>> login(

            @Valid @RequestBody LoginRequest request) {



        return ResponseEntity.ok(

                ApiResponse.<AuthResponse>builder()

                        .success(true)

                        .message("Login Successfully")

                        .data(authService.login(request))

                        .build()

        );

    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {

        emailVerificationService.verifyEmail(request);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Email verified successfully.")
                        .build()
        );
    }
    @PostMapping("/verify-mobile")
    public ResponseEntity<ApiResponse<Void>> verifyMobile(
            @Valid @RequestBody VerifyMobileRequest request) {

        mobileVerificationService.verifyOtp(request);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Mobile number verified successfully.")
                        .build()
        );
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerificationOtp(
            @Valid @RequestBody ResendVerificationRequest request) {

        emailVerificationService.resendVerificationOtp(request);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Verification OTP sent successfully.")
                        .build()
        );
    }
    @PostMapping("/resend-mobile-otp")
    public ResponseEntity<ApiResponse<Void>> resendMobileOtp(
            @Valid @RequestBody ResendMobileOtpRequest request) {

        mobileVerificationService.resendOtp(request);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Mobile OTP sent successfully.")
                        .build()
        );
    }

}


