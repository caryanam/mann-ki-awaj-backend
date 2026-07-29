package com.mka.service;

public interface EmailService {
    void sendVerificationOtp(String toEmail, String otp);
}
