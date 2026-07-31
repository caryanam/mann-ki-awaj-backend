package com.mka.mapper;

import com.mka.dto.request.RegisterRequest;
import com.mka.dto.response.UserResponse;
import com.mka.entity.User;
import com.mka.enums.Role;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(RegisterRequest request) {
        if (request == null) {
            return null;
        }
        return User.builder()
                .fullName(request.getFullName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .mobileNumber(request.getMobileNumber().trim())
                .role(Role.USER)
                .active(true)
                .deleted(false)
                .emailVerified(false)
                .mobileVerified(false)
                .build();
    }

    public UserResponse toResponse(User entity) {
        if (entity == null) {
            return null;
        }
        return UserResponse.builder()
                .id(entity.getId())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .mobileNumber(entity.getMobileNumber())
                .role(entity.getRole())
                .active(entity.getActive())
                .emailVerified(entity.getEmailVerified())
                .mobileVerified(entity.getMobileVerified())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
