package com.mka.mapper;

import com.mka.dto.request.CreateProfileRequest;
import com.mka.dto.response.ProfileResponse;
import com.mka.entity.Profile;
import com.mka.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ProfileMapper {

    public Profile toEntity(CreateProfileRequest request, User user) {
        if (request == null) {
            return null;
        }
        return Profile.builder()
                .user(user)
                .username(request.getUsername().trim())
                .avatar(request.getAvatar() != null ? request.getAvatar() : "#6F405F")
                .preferredLanguage(request.getPreferredLanguage() != null ? request.getPreferredLanguage() : "EN")
                .bio(request.getBio() != null ? request.getBio().trim() : null)
                .build();
    }

    public ProfileResponse toResponse(Profile entity) {
        if (entity == null) {
            return null;
        }
        return ProfileResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .username(entity.getUsername())
                .avatar(entity.getAvatar())
                .preferredLanguage(entity.getPreferredLanguage())
                .bio(entity.getBio())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
