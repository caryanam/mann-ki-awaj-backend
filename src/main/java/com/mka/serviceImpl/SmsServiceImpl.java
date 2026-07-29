package com.mka.serviceImpl;

import com.mka.service.SmsService;
import org.springframework.stereotype.Service;

@Service
public class SmsServiceImpl implements SmsService {
    @Override
    public void sendOtp(String mobileNumber, String otp) {
        System.out.println("--------------------------------");
        System.out.println("SMS SENT");
        System.out.println("Mobile : " + mobileNumber);
        System.out.println("OTP : " + otp);
        System.out.println("--------------------------------");
    }
}
