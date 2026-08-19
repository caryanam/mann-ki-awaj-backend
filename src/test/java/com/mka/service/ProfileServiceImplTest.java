package com.mka.service;

import com.mka.dto.request.CreateProfileRequest;
import com.mka.dto.request.UpdateProfileRequest;
import com.mka.dto.response.ProfileResponse;
import com.mka.entity.Profile;
import com.mka.entity.User;
import com.mka.enums.Role;
import com.mka.exception.ResourceAlreadyExistsException;
import com.mka.exception.ResourceNotFoundException;
import com.mka.exception.ValidationException;
import com.mka.mapper.ProfileMapper;
import com.mka.repository.ProfileRepository;
import com.mka.repository.UserRepository;
import com.mka.service.impl.ProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileMapper profileMapper;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private User testUser;
    private Profile testProfile;
    private ProfileResponse testProfileResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("Test User")
                .role(Role.USER)
                .active(true)
                .deleted(false)
                .build();

        testProfile = Profile.builder()
                .id(10L)
                .user(testUser)
                .username("test_handle")
                .avatar("avatar_default")
                .preferredLanguage("EN")
                .bio("Hello bio")
                .build();

        testProfileResponse = ProfileResponse.builder()
                .id(10L)
                .userId(1L)
                .username("test_handle")
                .avatar("avatar_default")
                .preferredLanguage("EN")
                .bio("Hello bio")
                .build();
    }

    @Test
    void testCreateProfile_Success() {
        CreateProfileRequest request = CreateProfileRequest.builder()
                .username("new_handle")
                .bio("My Bio")
                .avatar("avatar_1")
                .preferredLanguage("HI")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(profileRepository.existsByUsername("new_handle")).thenReturn(false);
        when(profileMapper.toEntity(any(), any())).thenReturn(testProfile);
        when(profileRepository.save(any(Profile.class))).thenReturn(testProfile);
        when(profileMapper.toResponse(any(Profile.class))).thenReturn(testProfileResponse);

        ProfileResponse response = profileService.createProfile(1L, request);

        assertNotNull(response);
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    void testCreateProfile_DuplicateUsername_ThrowsResourceAlreadyExistsException() {
        CreateProfileRequest request = CreateProfileRequest.builder()
                .username("taken_handle")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(profileRepository.existsByUsername("taken_handle")).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> profileService.createProfile(1L, request));
    }

    @Test
    void testCreateProfile_UnsupportedLanguage_ThrowsValidationException() {
        CreateProfileRequest request = CreateProfileRequest.builder()
                .username("new_handle")
                .preferredLanguage("INVALID_LANG")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(ValidationException.class, () -> profileService.createProfile(1L, request));
    }

    @Test
    void testGetMyProfile_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(profileMapper.toResponse(testProfile)).thenReturn(testProfileResponse);

        ProfileResponse response = profileService.getMyProfile(1L);

        assertNotNull(response);
        assertEquals("test_handle", response.getUsername());
    }

    @Test
    void testGetProfileByUsername_Success() {
        when(profileRepository.findByUsername("test_handle")).thenReturn(Optional.of(testProfile));
        when(profileMapper.toResponse(testProfile)).thenReturn(testProfileResponse);

        ProfileResponse response = profileService.getProfileByUsername("test_handle");

        assertNotNull(response);
        assertEquals("test_handle", response.getUsername());
    }

    @Test
    void testGetProfileByUsername_NonExistent_ThrowsResourceNotFoundException() {
        when(profileRepository.findByUsername("unknown_handle")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> profileService.getProfileByUsername("unknown_handle"));
    }

    @Test
    void testGetProfileByUsername_InactiveUser_ThrowsResourceNotFoundException() {
        User inactiveUser = User.builder().id(2L).active(false).deleted(true).build();
        Profile inactiveProfile = Profile.builder().id(11L).user(inactiveUser).username("inactive_handle").build();

        when(profileRepository.findByUsername("inactive_handle")).thenReturn(Optional.of(inactiveProfile));

        assertThrows(ResourceNotFoundException.class, () -> profileService.getProfileByUsername("inactive_handle"));
    }

    @Test
    void testUpdateProfile_Success() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .username("updated_handle")
                .bio("Updated Bio")
                .preferredLanguage("MR")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(profileRepository.existsByUsername("updated_handle")).thenReturn(false);
        when(profileRepository.save(any(Profile.class))).thenReturn(testProfile);
        when(profileMapper.toResponse(any(Profile.class))).thenReturn(testProfileResponse);

        ProfileResponse response = profileService.updateProfile(1L, request);

        assertNotNull(response);
        verify(profileRepository).save(testProfile);
    }

    @Test
    void testUpdateLanguage_ValidLanguage_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(profileRepository.save(any(Profile.class))).thenReturn(testProfile);
        when(profileMapper.toResponse(any(Profile.class))).thenReturn(testProfileResponse);

        ProfileResponse response = profileService.updateLanguage(1L, "HI");

        assertNotNull(response);
        assertEquals("HI", testProfile.getPreferredLanguage());
    }

    @Test
    void testUpdateLanguage_UnsupportedLanguage_ThrowsValidationException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(ValidationException.class, () -> profileService.updateLanguage(1L, "XYZ_LANG"));
    }

    @Test
    void testDeleteProfile_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));

        profileService.deleteProfile(1L);

        verify(profileRepository).delete(testProfile);
    }
}
