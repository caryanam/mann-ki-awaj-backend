package com.mka.serviceImpl;

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

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendVerificationOtp(String toEmail, String otp) {
        System.out.println("Sending email to: " + toEmail);

        try {

            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Verify Your Email");
            message.setText(
                    "Hello,\n\n" +
                            "Your verification OTP is: " + otp + "\n\n" +
                            "This OTP is valid for 10 minutes.\n\n" +
                            "Regards,\n" +
                            "Mann Ki Aavaj Team"
            );

            mailSender.send(message);

            System.out.println("MailSender finished");

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
