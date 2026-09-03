package com.mka.dto.response;

import com.mka.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private String mobileNumber;
    private Role role;
    private Boolean active;
    private Boolean emailVerified;
    private Boolean mobileVerified;
    private Instant createdAt;
    private Instant updatedAt;
}
