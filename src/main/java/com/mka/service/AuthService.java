package com.mka.service;

import com.mka.dto.request.*;
import com.mka.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void verifyForgotPasswordOtp(VerifyForgotPasswordOtpRequest request);

    void resetPassword(ResetPasswordRequest request);

    void deleteAccount(DeleteAccountRequest request);
}
