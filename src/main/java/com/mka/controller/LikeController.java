package com.mka.controller;

import com.mka.config.UserPrincipal;
import com.mka.dto.response.ApiResponse;
import com.mka.service.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Likes", description = "Like & Unlike APIs for Posts and Comments")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/posts/{id}/like")
    @Operation(summary = "Like a post")
    public ResponseEntity<ApiResponse<Void>> likePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {

        likeService.likePost(principal.getUsername(), id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Post liked successfully")
                        .build()
        );
    }

    @DeleteMapping("/posts/{id}/like")
    @Operation(summary = "Unlike a post")
    public ResponseEntity<ApiResponse<Void>> unlikePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {

        likeService.unlikePost(principal.getUsername(), id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Post unliked successfully")
                        .build()
        );
    }

    @PostMapping("/comments/{id}/like")
    @Operation(summary = "Like a comment")
    public ResponseEntity<ApiResponse<Void>> likeComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {

        likeService.likeComment(principal.getUsername(), id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Comment liked successfully")
                        .build()
        );
    }

    @DeleteMapping("/comments/{id}/like")
    @Operation(summary = "Unlike a comment")
    public ResponseEntity<ApiResponse<Void>> unlikeComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {

        likeService.unlikeComment(principal.getUsername(), id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Comment unliked successfully")
                        .build()
        );
    }
}
