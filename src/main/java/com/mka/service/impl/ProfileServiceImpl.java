package com.mka.service.impl;

import com.mka.dto.request.CreateProfileRequest;
import com.mka.dto.request.UpdateProfileRequest;
import com.mka.dto.response.ProfileResponse;
import com.mka.entity.Profile;
import com.mka.entity.User;
import com.mka.exception.ResourceAlreadyExistsException;
import com.mka.exception.ResourceNotFoundException;
import com.mka.mapper.ProfileMapper;
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
    private final ProfileMapper profileMapper;

    @Override
    @Transactional
    public ProfileResponse createProfile(Long userId, CreateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (profileRepository.findByUser(user).isPresent()) {
            throw new ResourceAlreadyExistsException("Profile already exists for this user");
        }

        if (profileRepository.existsByUsername(request.getUsername().trim())) {
            throw new ResourceAlreadyExistsException("Username handle @" + request.getUsername() + " is already taken");
        }

        Profile profile = profileMapper.toEntity(request, user);
        Profile savedProfile = profileRepository.save(profile);
        return profileMapper.toResponse(savedProfile);
    }

    @Override
    @Transactional
    public ProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Profile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> profileRepository.save(
                        Profile.builder()
                                .user(user)
                                .username("user_" + user.getId())
                                .avatar("avatar_default")
                                .preferredLanguage("EN")
                                .build()
                ));
        return profileMapper.toResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfileByUsername(String username) {
        Profile profile = profileRepository.findByUsername(username.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found with username: " + username));
        return profileMapper.toResponse(profile);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Profile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> profileRepository.save(
                        Profile.builder()
                                .user(user)
                                .username("user_" + user.getId())
                                .avatar("avatar_default")
                                .preferredLanguage("EN")
                                .build()
                ));

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            String newUsername = request.getUsername().trim();
            if (!newUsername.equalsIgnoreCase(profile.getUsername()) && profileRepository.existsByUsername(newUsername)) {
                throw new ResourceAlreadyExistsException("Username handle @" + newUsername + " is already taken");
            }
            profile.setUsername(newUsername);
        }

        if (request.getAvatar() != null && !request.getAvatar().isBlank()) {
            profile.setAvatar(request.getAvatar().trim());
        }

        if (request.getPreferredLanguage() != null && !request.getPreferredLanguage().isBlank()) {
            profile.setPreferredLanguage(request.getPreferredLanguage().trim());
        }

        if (request.getBio() != null) {
            profile.setBio(request.getBio().trim());
        }

        Profile updatedProfile = profileRepository.save(profile);
        return profileMapper.toResponse(updatedProfile);
    }

    @Override
    @Transactional
    public ProfileResponse updateAvatar(Long userId, String avatar) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Profile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> profileRepository.save(
                        Profile.builder()
                                .user(user)
                                .username("user_" + user.getId())
                                .avatar("avatar_default")
                                .preferredLanguage("EN")
                                .build()
                ));
        profile.setAvatar(avatar);
        return profileMapper.toResponse(profileRepository.save(profile));
    }

    @Override
    @Transactional
    public ProfileResponse updateLanguage(Long userId, String language) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Profile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> profileRepository.save(
                        Profile.builder()
                                .user(user)
                                .username("user_" + user.getId())
                                .avatar("avatar_default")
                                .preferredLanguage(language)
                                .build()
                ));
        profile.setPreferredLanguage(language);
        return profileMapper.toResponse(profileRepository.save(profile));
    }

    @Override
    @Transactional
    public void deleteProfile(Long userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user id: " + userId));
        profileRepository.delete(profile);
    }
}
