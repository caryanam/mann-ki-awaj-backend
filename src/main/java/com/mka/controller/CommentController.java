package com.mka.controller;

import com.mka.config.UserPrincipal;
import com.mka.dto.request.CreateCommentRequest;
import com.mka.dto.response.ApiResponse;
import com.mka.dto.response.CommentResponse;
import com.mka.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Comments", description = "Comment & Reply APIs")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/posts/{postId}/comments")
    @Operation(summary = "Add comment to a post with AI moderation")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request) {

        CommentResponse comment = commentService.createComment(principal.getUsername(), postId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<CommentResponse>builder()
                        .success(true)
                        .message("Comment added successfully")
                        .data(comment)
                        .build()
        );
    }

    @PostMapping("/topics/{topicId}/comments")
    @Operation(summary = "Add an opinion directly to a user-created subtopic")
    public ResponseEntity<ApiResponse<CommentResponse>> createTopicComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long topicId,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse comment = commentService.createTopicComment(principal.getUsername(), topicId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<CommentResponse>builder().success(true)
                        .message("Opinion added successfully").data(comment).build());
    }

    @GetMapping("/topics/{topicId}/comments")
    @Operation(summary = "Get opinions for a user-created subtopic")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getTopicComments(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long topicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String email = principal != null ? principal.getUsername() : null;
        Page<CommentResponse> comments = commentService.getCommentsByTopicId(
                email, topicId, PageRequest.of(page, size, Sort.by("createdAt").ascending()));
        return ResponseEntity.ok(ApiResponse.<Page<CommentResponse>>builder().success(true)
                .message("Opinions retrieved successfully").data(comments).build());
    }

    @PostMapping({"/comments/{commentId}/reply", "/comments/{commentId}/replies"})
    @Operation(summary = "Reply to an existing comment")
    public ResponseEntity<ApiResponse<CommentResponse>> replyToComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long commentId,
            @Valid @RequestBody CreateCommentRequest request) {

        CommentResponse reply = commentService.replyToComment(principal.getUsername(), commentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<CommentResponse>builder()
                        .success(true)
                        .message("Reply added successfully")
                        .data(reply)
                        .build()
        );
    }

    @GetMapping("/posts/{postId}/comments")
    @Operation(summary = "Get comments for a post with translation and nested replies")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getCommentsByPostId(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String email = principal != null ? principal.getUsername() : null;
        Page<CommentResponse> comments = commentService.getCommentsByPostId(
                email, postId, PageRequest.of(page, size, Sort.by("createdAt").ascending()));

        return ResponseEntity.ok(
                ApiResponse.<Page<CommentResponse>>builder()
                        .success(true)
                        .message("Comments retrieved successfully")
                        .data(comments)
                        .build()
        );
    }

    @PutMapping("/comments/{id}")
    @Operation(summary = "Update comment by ID")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request) {

        CommentResponse updated = commentService.updateComment(principal.getUsername(), id, request);
        return ResponseEntity.ok(
                ApiResponse.<CommentResponse>builder()
                        .success(true)
                        .message("Comment updated successfully")
                        .data(updated)
                        .build()
        );
    }

    @DeleteMapping("/comments/{id}")
    @Operation(summary = "Delete comment by ID")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {

        commentService.deleteComment(principal.getUsername(), id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Comment deleted successfully")
                        .build()
        );
    }
}
