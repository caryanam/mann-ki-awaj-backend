package com.mka.service;

public interface SmsService {
    void sendOtp(String mobileNumber, String otp);
    void sendSms(String mobileNumber, String message);
}
