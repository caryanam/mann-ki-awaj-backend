package com.mka.controller;

import com.mka.config.UserPrincipal;
import com.mka.dto.request.CreateProfileRequest;
import com.mka.dto.request.UpdateProfileRequest;
import com.mka.dto.responce.ApiResponse;
import com.mka.dto.responce.ProfileResponse;
import com.mka.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@Tag(name = "Profile", description = "Profile Management APIs")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping
    @Operation(summary = "Create Profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> createProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateProfileRequest request) {

        Long userId = principal.getUser().getId();

        return ResponseEntity.ok(
                ApiResponse.<ProfileResponse>builder()
                        .success(true)
                        .message("Profile Created Successfully")
                        .data(profileService.createProfile(userId, request))
                        .build()
        );
    }

    @GetMapping("/me")
    @Operation(summary = "Get My Profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {

        Long userId = principal.getUser().getId();

        return ResponseEntity.ok(
                ApiResponse.<ProfileResponse>builder()
                        .success(true)
                        .message("Profile Fetched Successfully")
                        .data(profileService.getMyProfile(userId))
                        .build()
        );
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get Public Profile By Username")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfileByUsername(
            @PathVariable String username) {

        return ResponseEntity.ok(
                ApiResponse.<ProfileResponse>builder()
                        .success(true)
                        .message("Profile Fetched Successfully")
                        .data(profileService.getProfileByUsername(username))
                        .build()
        );
    }

    @PutMapping
    @Operation(summary = "Update Profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {

        Long userId = principal.getUser().getId();

        return ResponseEntity.ok(
                ApiResponse.<ProfileResponse>builder()
                        .success(true)
                        .message("Profile Updated Successfully")
                        .data(profileService.updateProfile(userId, request))
                        .build()
        );
    }

    @DeleteMapping
    @Operation(summary = "Delete Profile")
    public ResponseEntity<ApiResponse<Void>> deleteProfile(
            @AuthenticationPrincipal UserPrincipal principal) {

        Long userId = principal.getUser().getId();
        profileService.deleteProfile(userId);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Profile Deleted Successfully")
                        .build()
        );
    }
}