package com.mka.dto.request;



import jakarta.validation.constraints.Email;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Pattern;

import jakarta.validation.constraints.Size;

import lombok.*;



@Getter

@Setter

@NoArgsConstructor

@AllArgsConstructor

@Builder

public class RegisterRequest {



    @NotBlank(message = "Full name is required")

    @Size(min = 2, max = 80, message = "Full name must be between 2 and 80 characters")
    @Pattern(regexp = "^[A-Za-z ]+$", message = "Full name must contain only letters and spaces")
    private String fullName;



    @Email(message = "Invalid email address")

    @NotBlank(message = "Email is required")

    private String email;



    @NotBlank(message = "Mobile number is required")
    @Pattern(

            regexp = "^[6-9][0-9]{9}$",

            message = "Invalid mobile number"

    )

    private String mobileNumber;



    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 30, message = "Password must be between 8 and 30 characters")

    private String password;



}


