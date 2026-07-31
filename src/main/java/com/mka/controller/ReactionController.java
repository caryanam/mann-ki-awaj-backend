package com.mka.controller;

import com.mka.config.UserPrincipal;
import com.mka.dto.request.ReactionRequest;
import com.mka.dto.response.ApiResponse;
import com.mka.service.ReactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Reactions", description = "Rich Emoji Reaction APIs for Posts and Comments")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;

    @PostMapping("/posts/{id}/react")
    @Operation(summary = "Add or update a rich reaction on a post (LIKE, LOVE, HAHA, WOW, SAD, ANGRY)")
    public ResponseEntity<ApiResponse<Void>> reactToPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ReactionRequest request) {

        reactionService.reactToPost(principal.getUsername(), id, request);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Reaction added/updated successfully")
                        .build()
        );
    }

    @DeleteMapping("/posts/{id}/react")
    @Operation(summary = "Remove reaction from a post")
    public ResponseEntity<ApiResponse<Void>> removePostReaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {

        reactionService.removePostReaction(principal.getUsername(), id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Reaction removed successfully")
                        .build()
        );
    }

    @PostMapping("/comments/{id}/react")
    @Operation(summary = "Add or update a rich reaction on a comment")
    public ResponseEntity<ApiResponse<Void>> reactToComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ReactionRequest request) {

        reactionService.reactToComment(principal.getUsername(), id, request);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Comment reaction added/updated successfully")
                        .build()
        );
    }

    @DeleteMapping("/comments/{id}/react")
    @Operation(summary = "Remove reaction from a comment")
    public ResponseEntity<ApiResponse<Void>> removeCommentReaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {

        reactionService.removeCommentReaction(principal.getUsername(), id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Comment reaction removed successfully")
                        .build()
        );
    }
}
