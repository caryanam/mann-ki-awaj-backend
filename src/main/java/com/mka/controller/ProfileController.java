package com.mka.controller;

import com.mka.config.UserPrincipal;
import com.mka.dto.request.CreateProfileRequest;
import com.mka.dto.request.UpdateProfileRequest;
import com.mka.dto.response.ApiResponse;
import com.mka.dto.response.ProfileResponse;
import com.mka.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@Tag(name = "Profile", description = "Anonymous Public Profile Management APIs")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping
    @Operation(summary = "Create Anonymous Profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> createProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateProfileRequest request) {
        Long userId = principal.getUser().getId();
        ProfileResponse response = profileService.createProfile(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Profile created successfully", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get My Profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.getUser().getId();
        ProfileResponse response = profileService.getMyProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile fetched successfully", response));
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get Public Profile by Username Handle")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfileByUsername(
            @PathVariable String username) {
        ProfileResponse response = profileService.getProfileByUsername(username);
        return ResponseEntity.ok(ApiResponse.success("Public profile fetched successfully", response));
    }

    @PutMapping
    @Operation(summary = "Update My Profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = principal.getUser().getId();
        ProfileResponse response = profileService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    @DeleteMapping
    @Operation(summary = "Delete My Profile")
    public ResponseEntity<ApiResponse<Void>> deleteProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.getUser().getId();
        profileService.deleteProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile deleted successfully"));
    }
}