package com.mka.dto.responce;



import com.mka.enums.Role;

import lombok.AllArgsConstructor;

import lombok.Builder;

import lombok.Data;

import lombok.NoArgsConstructor;



@Data

@NoArgsConstructor

@AllArgsConstructor

@Builder

public class LoginResponseDTO {



    private Long id;



    private String fullName;



    private String email;



    private String mobileNumber;



    private Role role;

}
