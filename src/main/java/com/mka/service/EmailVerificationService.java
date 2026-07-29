package com.mka.service;

import com.mka.dto.request.ResendVerificationRequest;
import com.mka.dto.request.VerifyEmailRequest;
import com.mka.entity.User;

public interface EmailVerificationService {
    void sendVerificationOtp(User user);

    void verifyEmail(VerifyEmailRequest request);

    void resendVerificationOtp(ResendVerificationRequest request);
}
