package com.mka.controller;

import com.mka.config.UserPrincipal;
import com.mka.dto.request.UpdateAvatarRequest;
import com.mka.dto.request.UpdateLanguageRequest;
import com.mka.dto.request.UpdatePasswordRequest;
import com.mka.dto.response.ApiResponse;
import com.mka.dto.response.ProfileResponse;
import com.mka.dto.response.UserResponse;
import com.mka.service.ProfileService;
import com.mka.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User Profile, Credentials & Account Management APIs")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ProfileService profileService;

    @GetMapping("/me")
    @Operation(summary = "Get current logged-in user details")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse user = userService.getUserByEmail(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Current user details retrieved successfully", user));
    }

    @GetMapping("/me/profile")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse user = userService.getUserByEmail(principal.getUsername());
        ProfileResponse profile = profileService.getMyProfile(user.getId());
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", profile));
    }

    @PutMapping("/avatar")
    @Operation(summary = "Update profile avatar")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateAvatar(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateAvatarRequest request) {
        UserResponse user = userService.getUserByEmail(principal.getUsername());
        ProfileResponse profile = profileService.updateAvatar(user.getId(), request.getAvatar());
        return ResponseEntity.ok(ApiResponse.success("Avatar updated successfully", profile));
    }

    @PutMapping("/language")
    @Operation(summary = "Update preferred translation language")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateLanguage(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateLanguageRequest request) {
        UserResponse user = userService.getUserByEmail(principal.getUsername());
        ProfileResponse profile = profileService.updateLanguage(user.getId(), request.getLanguage());
        return ResponseEntity.ok(ApiResponse.success("Preferred language updated successfully", profile));
    }

    @PutMapping("/password")
    @Operation(summary = "Update user password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(principal.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Password updated successfully"));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Deactivate account")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse user = userService.getUserByEmail(principal.getUsername());
        userService.deactivateUser(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Account deactivated successfully"));
    }
}
