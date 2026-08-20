package com.mka.controller;

import com.mka.config.UserPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.mka.dto.request.UpdateAvatarRequest;
import com.mka.dto.request.UpdateLanguageRequest;
import com.mka.dto.request.UpdatePasswordRequest;
import com.mka.dto.response.ApiResponse;
import com.mka.dto.response.ProfileResponse;
import com.mka.dto.response.UserResponse;
import com.mka.service.ProfileService;
import com.mka.service.UserMuteService;
import com.mka.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User Profile, Credentials & Account Management APIs")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ProfileService profileService;
    private final UserMuteService userMuteService;

    private String resolveUsername(Object principalObj) {
        if (principalObj instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        } else if (principalObj != null) {
            return principalObj.toString();
        }
        return "";
    }

    @GetMapping("/me")
    @Operation(summary = "Get current logged-in user details")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(@AuthenticationPrincipal Object principalObj) {
        String email = resolveUsername(principalObj);
        UserResponse user = userService.getUserByEmail(email);
        return ResponseEntity.ok(ApiResponse.success("Current user details retrieved successfully", user));
    }

    @GetMapping("/me/profile")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile(@AuthenticationPrincipal Object principalObj) {
        String email = resolveUsername(principalObj);
        UserResponse user = userService.getUserByEmail(email);
        ProfileResponse profile = profileService.getMyProfile(user.getId());
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", profile));
    }

    @PutMapping("/avatar")
    @Operation(summary = "Update profile avatar")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateAvatar(
            @AuthenticationPrincipal Object principalObj,
            @Valid @RequestBody UpdateAvatarRequest request) {
        String email = resolveUsername(principalObj);
        UserResponse user = userService.getUserByEmail(email);
        ProfileResponse profile = profileService.updateAvatar(user.getId(), request.getAvatar());
        return ResponseEntity.ok(ApiResponse.success("Avatar updated successfully", profile));
    }

    @PutMapping("/language")
    @Operation(summary = "Update preferred translation language")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateLanguage(
            @AuthenticationPrincipal Object principalObj,
            @Valid @RequestBody UpdateLanguageRequest request) {
        String email = resolveUsername(principalObj);
        UserResponse user = userService.getUserByEmail(email);
        ProfileResponse profile = profileService.updateLanguage(user.getId(), request.getLanguage());
        return ResponseEntity.ok(ApiResponse.success("Preferred language updated successfully", profile));
    }

    @PutMapping("/password")
    @Operation(summary = "Update user password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal Object principalObj,
            @Valid @RequestBody UpdatePasswordRequest request) {
        String email = resolveUsername(principalObj);
        userService.updatePassword(email, request);
        return ResponseEntity.ok(ApiResponse.success("Password updated successfully"));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Deactivate account")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(@AuthenticationPrincipal Object principalObj) {
        String email = resolveUsername(principalObj);
        UserResponse user = userService.getUserByEmail(email);
        userService.deactivateUser(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Account deactivated successfully"));
    }

    @PostMapping("/mute/{username}")
    @Operation(summary = "Mute a user handle so their posts are hidden from feed")
    public ResponseEntity<ApiResponse<Void>> muteUser(
            @AuthenticationPrincipal Object principalObj,
            @PathVariable String username) {
        String identifier = resolveUsername(principalObj);
        userMuteService.muteUser(identifier, username);
        return ResponseEntity.ok(ApiResponse.success("User muted successfully"));
    }

    @DeleteMapping("/unmute/{username}")
    @Operation(summary = "Unmute a user handle so their posts are visible again")
    public ResponseEntity<ApiResponse<Void>> unmuteUser(
            @AuthenticationPrincipal Object principalObj,
            @PathVariable String username) {
        String identifier = resolveUsername(principalObj);
        userMuteService.unmuteUser(identifier, username);
        return ResponseEntity.ok(ApiResponse.success("User unmuted successfully"));
    }

    @GetMapping("/muted")
    @Operation(summary = "Get list of muted handles for current user")
    public ResponseEntity<ApiResponse<List<String>>> getMutedUsers(@AuthenticationPrincipal Object principalObj) {
        String identifier = resolveUsername(principalObj);
        List<String> muted = userMuteService.getMutedUsers(identifier);
        return ResponseEntity.ok(ApiResponse.success("Muted users retrieved successfully", muted));
    }
}
