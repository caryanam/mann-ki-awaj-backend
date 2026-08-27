package com.mka.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteAccountRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    @Size(max = 254, message = "Email cannot exceed 254 characters")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
