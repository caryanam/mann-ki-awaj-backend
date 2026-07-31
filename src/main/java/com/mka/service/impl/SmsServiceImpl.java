package com.mka.service.impl;

import com.mka.service.SmsService;
import org.springframework.stereotype.Service;

@Service
public class SmsServiceImpl implements SmsService {

    @Override
    public void sendOtp(String mobileNumber, String otp) {
        sendSms(mobileNumber, "Your Mann Ki Aavaj OTP is: " + otp);
    }

    @Override
    public void sendSms(String mobileNumber, String message) {
        // SMS Gateway integration placeholder (Twilio / AWS SNS / MSG91)
    }
}
