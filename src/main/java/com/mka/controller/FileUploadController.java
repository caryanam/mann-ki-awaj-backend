package com.mka.controller;

import com.mka.dto.response.ApiResponse;
import com.mka.entity.User;
import com.mka.repository.UserRepository;
import com.mka.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@Tag(name = "File Upload", description = "Image Upload & AI Moderation Endpoint")
@RequiredArgsConstructor
public class FileUploadController {

    private final AiService aiService;
    private final UserRepository userRepository;

    @PostMapping({"", "/image"})
    @Operation(summary = "Upload and moderate post image file before publication")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file,
            Principal principal) {

        User currentUser = null;
        if (principal != null && principal.getName() != null) {
            currentUser = userRepository.findByEmail(principal.getName()).orElse(null);
        }

        String imageUrl = aiService.moderateAndSaveImage(file, currentUser);

        Map<String, String> data = Map.of("imageUrl", imageUrl);

        return ResponseEntity.ok(
                ApiResponse.<Map<String, String>>builder()
                        .success(true)
                        .message("Image successfully moderated and uploaded")
                        .data(data)
                        .build()
        );
    }
}
