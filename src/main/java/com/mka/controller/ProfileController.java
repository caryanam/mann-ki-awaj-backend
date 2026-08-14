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

    private Long resolveUserId(Object principalObj) {
        if (principalObj instanceof UserPrincipal userPrincipal && userPrincipal.getUser() != null) {
            return userPrincipal.getUser().getId();
        }
        return null;
    }

    @PostMapping
    @Operation(summary = "Create Anonymous Profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> createProfile(
            @AuthenticationPrincipal Object principalObj,
            @Valid @RequestBody CreateProfileRequest request) {
        Long userId = resolveUserId(principalObj);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<ProfileResponse>builder()
                            .success(false)
                            .message("Authentication required")
                            .build());
        }
        ProfileResponse response = profileService.createProfile(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Profile created successfully", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get My Profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile(
            @AuthenticationPrincipal Object principalObj) {
        Long userId = resolveUserId(principalObj);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<ProfileResponse>builder()
                            .success(false)
                            .message("Authentication required")
                            .build());
        }
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
            @AuthenticationPrincipal Object principalObj,
            @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = resolveUserId(principalObj);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<ProfileResponse>builder()
                            .success(false)
                            .message("Authentication required")
                            .build());
        }
        ProfileResponse response = profileService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    @DeleteMapping
    @Operation(summary = "Delete My Profile")
    public ResponseEntity<ApiResponse<Void>> deleteProfile(
            @AuthenticationPrincipal Object principalObj) {
        Long userId = resolveUserId(principalObj);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Authentication required")
                            .build());
        }
        profileService.deleteProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile deleted successfully"));
    }
}