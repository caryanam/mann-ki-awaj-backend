package com.mka.service;
import com.mka.dto.request.CreateProfileRequest;
import com.mka.dto.request.UpdateProfileRequest;
import com.mka.dto.responce.ProfileResponse;

public interface ProfileService {

    ProfileResponse createProfile(Long userId, CreateProfileRequest request);

    ProfileResponse getMyProfile(Long userId);

    ProfileResponse getProfileByUsername(String username);

    ProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    void deleteProfile(Long userId);
}
