package com.mka.service;

import com.mka.dto.request.CreateProfileRequest;
import com.mka.dto.request.UpdateProfileRequest;
import com.mka.dto.response.ProfileResponse;

public interface ProfileService {

    ProfileResponse createProfile(Long userId, CreateProfileRequest request);

    ProfileResponse getMyProfile(Long userId);

    ProfileResponse getProfileByUsername(String username);

    ProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    ProfileResponse updateAvatar(Long userId, String avatar);

    ProfileResponse updateLanguage(Long userId, String language);

    void deleteProfile(Long userId);
}
