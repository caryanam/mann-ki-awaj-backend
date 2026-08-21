package com.mka.controller;

import com.mka.dto.request.*;
import com.mka.dto.response.ApiResponse;
import com.mka.dto.response.AuthResponse;
import com.mka.service.AuthService;
import com.mka.service.EmailVerificationService;
import com.mka.service.MobileVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication & OTP Verification Endpoints")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final MobileVerificationService mobileVerificationService;
    private final com.mka.service.EnquiryService enquiryService;

    @PostMapping("/register")
    @Operation(summary = "Register User Account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    @Operation(summary = "User / Admin Login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Logged in successfully", response));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Send Forgot Password OTP")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Verification OTP sent successfully"));
    }

    @PostMapping("/verify-forgot-password-otp")
    @Operation(summary = "Verify Forgot Password OTP")
    public ResponseEntity<ApiResponse<Void>> verifyForgotPasswordOtp(@Valid @RequestBody VerifyForgotPasswordOtpRequest request) {
        authService.verifyForgotPasswordOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset Account Password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully. You can now login with your new password."));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify Email OTP")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully"));
    }

    @PostMapping("/verify-mobile")
    @Operation(summary = "Verify Mobile SMS OTP")
    public ResponseEntity<ApiResponse<Void>> verifyMobile(@Valid @RequestBody VerifyMobileRequest request) {
        mobileVerificationService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Mobile number verified successfully"));
    }

    @PostMapping("/resend-email-otp")
    @Operation(summary = "Resend Email OTP")
    public ResponseEntity<ApiResponse<Void>> resendEmailOtp(@Valid @RequestBody ResendVerificationRequest request) {
        emailVerificationService.resendVerificationOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Verification OTP sent to email"));
    }

    @PostMapping("/resend-mobile-otp")
    @Operation(summary = "Resend Mobile OTP")
    public ResponseEntity<ApiResponse<Void>> resendMobileOtp(@Valid @RequestBody ResendMobileOtpRequest request) {
        mobileVerificationService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Verification OTP sent to mobile number"));
    }

    @PostMapping("/inquiry")
    @Operation(summary = "Submit Contact & Support Inquiry to Admin")
    public ResponseEntity<ApiResponse<String>> submitInquiry(@Valid @RequestBody CreateInquiryRequest request) {
        String ticketId = enquiryService.createEnquiry(request);
        return ResponseEntity.ok(ApiResponse.success("Support inquiry submitted successfully. Ticket ID: #" + ticketId, ticketId));
    }

}
