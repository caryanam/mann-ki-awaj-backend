package com.mka.service.impl;

import com.mka.dto.request.CreateProfileRequest;
import com.mka.dto.request.UpdateProfileRequest;
import com.mka.dto.response.ProfileResponse;
import com.mka.entity.Profile;
import com.mka.entity.User;
import com.mka.enums.LanguageCode;
import com.mka.exception.ResourceAlreadyExistsException;
import com.mka.exception.ResourceNotFoundException;
import com.mka.exception.UnauthorizedException;
import com.mka.exception.ValidationException;
import com.mka.mapper.ProfileMapper;
import com.mka.repository.ProfileRepository;
import com.mka.repository.UserRepository;
import com.mka.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mka.util.UsernameValidationUtil;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final ProfileMapper profileMapper;
    private final com.mka.client.openai.OpenAIClient openAIClient;

    private String validateAndFormatLanguage(String lang) {
        if (lang == null || lang.isBlank()) {
            return "EN";
        }
        String cleanLang = lang.trim().toUpperCase();
        try {
            LanguageCode.valueOf(cleanLang);
            return cleanLang;
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Unsupported language code: " + lang);
        }
    }

    private void validateUsernameHandle(String requestedUsername, User user) {
        // 1. Fast local dictionary & user full name token validation
        UsernameValidationUtil.validateUsername(requestedUsername, user != null ? user.getFullName() : null);

        // 2. Deep LLM AI Anonymity & Real-Name Audit
        if (openAIClient != null && openAIClient.isConfigured()) {
            com.mka.dto.response.UsernameAiCheckResult aiRes = openAIClient.validateUsernameWithAi(requestedUsername, user != null ? user.getFullName() : null);
            if (aiRes != null && !aiRes.isAllowed()) {
                throw new ValidationException(aiRes.getReason() != null ? aiRes.getReason() : "Username handle contains a real human name. Please choose a fictional or creative handle like 'captainamerica'.");
            }
        }
    }

    @Override
    @Transactional
    public ProfileResponse createProfile(Long userId, CreateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!Boolean.TRUE.equals(user.getActive()) || Boolean.TRUE.equals(user.getDeleted())) {
            throw new UnauthorizedException("User account is inactive or deleted");
        }

        String validLang = validateAndFormatLanguage(request.getPreferredLanguage());
        String requestedUsername = request.getUsername().trim();

        validateUsernameHandle(requestedUsername, user);

        // If profile already exists, update details instead of throwing 409 Conflict
        java.util.Optional<Profile> existingProfileOpt = profileRepository.findByUser(user);
        if (existingProfileOpt.isPresent()) {
            Profile profile = existingProfileOpt.get();

            if (profileRepository.existsByUsername(requestedUsername) && 
                !profile.getUsername().equalsIgnoreCase(requestedUsername)) {
                java.util.List<String> suggestions = UsernameValidationUtil.generateAvailableSuggestions(requestedUsername, profileRepository);
                String suggestionsStr = suggestions.isEmpty() ? "" : " Available suggestions: " + String.join(", ", suggestions.stream().map(s -> "@" + s).toList());
                throw new ResourceAlreadyExistsException("Username handle @" + requestedUsername + " is already taken." + suggestionsStr);
            }

            profile.setUsername(requestedUsername);
            profile.setBio(request.getBio() != null ? request.getBio().trim() : null);
            if (request.getAvatar() != null && !request.getAvatar().isBlank()) {
                profile.setAvatar(request.getAvatar().trim());
            }
            profile.setPreferredLanguage(validLang);

            Profile savedProfile = profileRepository.save(profile);
            return profileMapper.toResponse(savedProfile);
        }

        if (profileRepository.existsByUsername(requestedUsername)) {
            java.util.List<String> suggestions = UsernameValidationUtil.generateAvailableSuggestions(requestedUsername, profileRepository);
            String suggestionsStr = suggestions.isEmpty() ? "" : " Available suggestions: " + String.join(", ", suggestions.stream().map(s -> "@" + s).toList());
            throw new ResourceAlreadyExistsException("Username handle @" + requestedUsername + " is already taken." + suggestionsStr);
        }

        Profile profile = profileMapper.toEntity(request, user);
        profile.setPreferredLanguage(validLang);
        profile.setUsernameChangeCount(0);
        Profile savedProfile = profileRepository.save(profile);
        return profileMapper.toResponse(savedProfile);
    }

    @Override
    @Transactional
    public ProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!Boolean.TRUE.equals(user.getActive()) || Boolean.TRUE.equals(user.getDeleted())) {
            throw new UnauthorizedException("User account is inactive or deleted");
        }

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

        if (profile.getUser() != null && (!Boolean.TRUE.equals(profile.getUser().getActive()) || Boolean.TRUE.equals(profile.getUser().getDeleted()))) {
            throw new ResourceNotFoundException("Profile not found with username: " + username);
        }

        return profileMapper.toResponse(profile);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!Boolean.TRUE.equals(user.getActive()) || Boolean.TRUE.equals(user.getDeleted())) {
            throw new UnauthorizedException("User account is inactive or deleted");
        }

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

            if (!newUsername.equalsIgnoreCase(profile.getUsername())) {
                // 1. Validate real human names & abusive words (Local + AI Audit)
                validateUsernameHandle(newUsername, user);

                // 2. Enforce 14-Day Cooldown (1st change from profile is free, subsequent edits require 14 days wait)
                if (profile.getUsernameChangeCount() != null && profile.getUsernameChangeCount() >= 1 && profile.getUsernameLastChangedAt() != null) {
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    long daysPassed = java.time.Duration.between(profile.getUsernameLastChangedAt(), now).toDays();
                    if (daysPassed < 14) {
                        long daysLeft = 14 - daysPassed;
                        throw new ValidationException("Anonymous handle can only be updated once every 14 days. You can change it again in " + daysLeft + " days.");
                    }
                }

                // 3. Check availability & generate suggestions
                if (profileRepository.existsByUsername(newUsername)) {
                    java.util.List<String> suggestions = UsernameValidationUtil.generateAvailableSuggestions(newUsername, profileRepository);
                    String suggestionsStr = suggestions.isEmpty() ? "" : " Available suggestions: " + String.join(", ", suggestions.stream().map(s -> "@" + s).toList());
                    throw new ResourceAlreadyExistsException("Username handle @" + newUsername + " is already taken." + suggestionsStr);
                }

                profile.setUsername(newUsername);
                profile.setUsernameChangeCount((profile.getUsernameChangeCount() == null ? 0 : profile.getUsernameChangeCount()) + 1);
                profile.setUsernameLastChangedAt(java.time.LocalDateTime.now());
            }
        }

        if (request.getAvatar() != null && !request.getAvatar().isBlank()) {
            profile.setAvatar(request.getAvatar().trim());
        }

        if (request.getPreferredLanguage() != null && !request.getPreferredLanguage().isBlank()) {
            profile.setPreferredLanguage(validateAndFormatLanguage(request.getPreferredLanguage()));
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

        if (!Boolean.TRUE.equals(user.getActive()) || Boolean.TRUE.equals(user.getDeleted())) {
            throw new UnauthorizedException("User account is inactive or deleted");
        }

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

        if (!Boolean.TRUE.equals(user.getActive()) || Boolean.TRUE.equals(user.getDeleted())) {
            throw new UnauthorizedException("User account is inactive or deleted");
        }

        String validLang = validateAndFormatLanguage(language);

        Profile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> profileRepository.save(
                        Profile.builder()
                                .user(user)
                                .username("user_" + user.getId())
                                .avatar("avatar_default")
                                .preferredLanguage(validLang)
                                .build()
                ));
        profile.setPreferredLanguage(validLang);
        return profileMapper.toResponse(profileRepository.save(profile));
    }

    @Override
    @Transactional
    public void deleteProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user id: " + userId));
        profileRepository.delete(profile);
    }
}
