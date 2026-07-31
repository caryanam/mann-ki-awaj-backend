package com.mka.service;

public interface EmailService {
    void sendVerificationOtp(String toEmail, String otp);
    void sendEmail(String toEmail, String subject, String body);
}
