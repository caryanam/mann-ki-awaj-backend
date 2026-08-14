package com.mka.controller;

import com.mka.dto.response.ApiResponse;
import com.mka.entity.User;
import com.mka.repository.UserRepository;
import com.mka.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Principal;
import java.util.Map;

@RestController
@Tag(name = "File Upload", description = "Image Upload & Static Media File Serving Endpoint")
@RequiredArgsConstructor
public class FileUploadController {

    private final AiService aiService;
    private final UserRepository userRepository;

    @PostMapping({"/api/upload", "/api/upload/image"})
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

    @GetMapping("/uploads/{filename:.+}")
    @Operation(summary = "Stream or download uploaded media file")
    public ResponseEntity<byte[]> getUploadedFile(@PathVariable String filename) {
        try {
            File uploadsDir = new File("uploads").getAbsoluteFile();
            File file = new File(uploadsDir, filename);

            if (!file.exists() || !file.isFile()) {
                String svgFallback = """
                        <svg xmlns="http://www.w3.org/2000/svg" width="400" height="250" viewBox="0 0 400 250">
                          <rect width="100%" height="100%" fill="#F8F5F3" rx="16"/>
                          <path d="M160 110 L200 70 L240 110" stroke="#9F9794" stroke-width="3" fill="none"/>
                          <circle cx="230" cy="80" r="10" fill="#9F9794"/>
                          <text x="50%" y="65%" dominant-baseline="middle" text-anchor="middle" fill="#2D1D15" font-family="sans-serif" font-size="14" font-weight="bold">
                            Flagged Media Footprint
                          </text>
                          <text x="50%" y="78%" dominant-baseline="middle" text-anchor="middle" fill="#9F9794" font-family="sans-serif" font-size="11">
                            Media archived for admin audit
                          </text>
                        </svg>
                        """;
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, "image/svg+xml")
                        .body(svgFallback.getBytes(StandardCharsets.UTF_8));
            }

            byte[] imageBytes = Files.readAllBytes(file.toPath());
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                if (filename.toLowerCase().endsWith(".png")) contentType = "image/png";
                else if (filename.toLowerCase().endsWith(".webp")) contentType = "image/webp";
                else if (filename.toLowerCase().endsWith(".gif")) contentType = "image/gif";
                else contentType = "image/jpeg";
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(imageBytes);

        } catch (Exception e) {
            String svgFallback = """
                    <svg xmlns="http://www.w3.org/2000/svg" width="400" height="250" viewBox="0 0 400 250">
                      <rect width="100%" height="100%" fill="#F8F5F3" rx="16"/>
                      <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="#2D1D15" font-family="sans-serif" font-size="14">
                        Media Archived
                      </text>
                    </svg>
                    """;
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "image/svg+xml")
                    .body(svgFallback.getBytes(StandardCharsets.UTF_8));
        }
    }
}
