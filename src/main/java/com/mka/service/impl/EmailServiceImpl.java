package com.mka.service.impl;

import com.mka.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@mannkiaavaj.com}")
    private String fromEmail;

    @Override
    public void sendVerificationOtp(String toEmail, String otp) {
        sendEmail(
                toEmail,
                "Mann Ki Aavaj - Email Verification",
                "Hello,\n\nYour verification OTP is: " + otp + "\n\nThis OTP is valid for 15 minutes.\n\nRegards,\nMann Ki Aavaj Team"
        );
    }

    @Override
    public void sendEmail(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("SMTP Error sending mail to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
