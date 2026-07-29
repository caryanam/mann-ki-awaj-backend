package com.mka.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

@Getter
@Service
@NoArgsConstructor
@AllArgsConstructor
public class ResendMobileOtpRequest {

    @NotBlank(message = "Mobile number is required")
    private String mobileNumber;


}
