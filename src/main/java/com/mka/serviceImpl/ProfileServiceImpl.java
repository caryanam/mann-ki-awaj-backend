package com.mka.service.impl;

import com.mka.dto.request.CreateProfileRequest;
import com.mka.dto.request.UpdateProfileRequest;
import com.mka.dto.responce.ProfileResponse;
import com.mka.entity.Profile;
import com.mka.entity.User;
import com.mka.exception.ResourceAlreadyExistsException;
import com.mka.exception.ResourceNotFoundException;
import com.mka.repository.ProfileRepository;
import com.mka.repository.UserRepository;
import com.mka.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ProfileResponse createProfile(Long userId, CreateProfileRequest request) {
        // 1. Fetch user or throw error if user doesn't exist
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // 2. Ensure user doesn't already have a profile
        if (profileRepository.findByUserId(userId).isPresent()) {
            throw new ResourceAlreadyExistsException("Profile already exists for user id: " + userId);
        }

        // 3. Ensure username is available
        if (profileRepository.existsByUsername(request.getUsername())) {
            throw new ResourceAlreadyExistsException("Username '" + request.getUsername() + "' is already taken");
        }

        // 4. Build and save profile entity
        Profile profile = Profile.builder()
                .user(user)
                .username(request.getUsername().trim())
                .bio(request.getBio() != null ? request.getBio().trim() : null)
                .avatar(request.getAvatar() != null ? request.getAvatar().trim() : "default-avatar.png")
                .build();

        Profile savedProfile = profileRepository.save(profile);
        return mapToResponse(savedProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile(Long userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user id: " + userId));

        return mapToResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfileByUsername(String username) {
        Profile profile = profileRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found with username: " + username));

        return mapToResponse(profile);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user id: " + userId));

        // If user is trying to change their username, check for uniqueness conflicts
        if (!profile.getUsername().equals(request.getUsername())) {
            if (profileRepository.existsByUsername(request.getUsername())) {
                throw new ResourceAlreadyExistsException("Username '" + request.getUsername() + "' is already taken");
            }
            profile.setUsername(request.getUsername().trim());
        }

        // Update bio if provided
        if (request.getBio() != null) {
            profile.setBio(request.getBio().trim());
        }

        // Update avatar if provided
        if (request.getAvatar() != null && !request.getAvatar().isBlank()) {
            profile.setAvatar(request.getAvatar().trim());
        }

        Profile updatedProfile = profileRepository.save(profile);
        return mapToResponse(updatedProfile);
    }

    @Override
    @Transactional
    public void deleteProfile(Long userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user id: " + userId));

        profileRepository.delete(profile);
    }

    // Helper method to map Entity to DTO
    private ProfileResponse mapToResponse(Profile profile) {
        return ProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .username(profile.getUsername())
                .bio(profile.getBio())
                .avatar(profile.getAvatar())
                .build();
    }
}