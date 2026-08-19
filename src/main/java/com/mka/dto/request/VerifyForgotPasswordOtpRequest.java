package com.mka.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyForgotPasswordOtpRequest {

    @NotBlank(message = "Email or mobile number is required")
    @Size(min = 3, max = 254, message = "Identifier must be between 3 and 254 characters")
    private String identifier;

    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP code must be exactly 6 digits")
    private String otp;
}
