package com.mka.controller;

import com.mka.config.UserPrincipal;
import com.mka.dto.request.CreatePostRequest;
import com.mka.dto.request.UpdatePostRequest;
import com.mka.dto.response.ApiResponse;
import com.mka.dto.response.PostResponse;
import com.mka.enums.PostTopic;
import com.mka.service.PostService;
import com.mka.service.SavedPostService;
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
@RequestMapping("/api/posts")
@Tag(name = "Posts", description = "Post, Feed & Saved Posts APIs")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final SavedPostService savedPostService;

    @PostMapping
    @Operation(summary = "Create a new post with optional image, topic, type and AI moderation")
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreatePostRequest request) {

        PostResponse post = postService.createPost(principal.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<PostResponse>builder()
                        .success(true)
                        .message("Post created successfully")
                        .data(post)
                        .build()
        );
    }

    @GetMapping
    @Operation(summary = "Get post feed with optional topic filter and automatic AI translation")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getFeed(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) PostTopic topic,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        String email = principal != null ? principal.getUsername() : null;
        Page<PostResponse> feed = postService.getFeed(email, topic, PageRequest.of(page, size, sort));

        return ResponseEntity.ok(
                ApiResponse.<Page<PostResponse>>builder()
                        .success(true)
                        .message("Post feed retrieved successfully")
                        .data(feed)
                        .build()
        );
    }

    @GetMapping("/saved")
    @Operation(summary = "Get list of saved/bookmarked posts for current user")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getSavedPosts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<PostResponse> savedPosts = savedPostService.getSavedPosts(principal.getUsername(), PageRequest.of(page, size));
        return ResponseEntity.ok(
                ApiResponse.<Page<PostResponse>>builder()
                        .success(true)
                        .message("Saved posts retrieved successfully")
                        .data(savedPosts)
                        .build()
        );
    }

    @PostMapping("/{id}/save")
    @Operation(summary = "Save/Bookmark a post")
    public ResponseEntity<ApiResponse<Void>> savePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {

        savedPostService.savePost(principal.getUsername(), id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Post saved successfully")
                        .build()
        );
    }

    @DeleteMapping("/{id}/save")
    @Operation(summary = "Remove post from saved/bookmarked list")
    public ResponseEntity<ApiResponse<Void>> unsavePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {

        savedPostService.unsavePost(principal.getUsername(), id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Post removed from saved list")
                        .build()
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get post details by ID with AI translation")
    public ResponseEntity<ApiResponse<PostResponse>> getPostById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {

        String email = principal != null ? principal.getUsername() : null;
        PostResponse post = postService.getPostById(email, id);
        return ResponseEntity.ok(
                ApiResponse.<PostResponse>builder()
                        .success(true)
                        .message("Post retrieved successfully")
                        .data(post)
                        .build()
        );
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update post content by ID")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request) {

        PostResponse updated = postService.updatePost(principal.getUsername(), id, request);
        return ResponseEntity.ok(
                ApiResponse.<PostResponse>builder()
                        .success(true)
                        .message("Post updated successfully")
                        .data(updated)
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete post by ID")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {

        postService.deletePost(principal.getUsername(), id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Post deleted successfully")
                        .build()
        );
    }
}
