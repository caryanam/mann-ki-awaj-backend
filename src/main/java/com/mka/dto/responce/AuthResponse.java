package com.mka.dto.responce;



import com.mka.enums.Role;

import lombok.*;



@Getter

@Setter

@NoArgsConstructor

@AllArgsConstructor

@Builder

public class AuthResponse {



    private Long id;



    private String fullName;



    private String email;



    private String mobileNumber;



    private Role role;



    private String token;

}


