package com.mka.service;

import com.mka.dto.request.ResendMobileOtpRequest;
import com.mka.dto.request.VerifyMobileRequest;
import com.mka.entity.User;

public interface MobileVerificationService {
    void sendOtp(User user);

    void verifyOtp(VerifyMobileRequest request);

    void resendOtp(ResendMobileOtpRequest request);
}
