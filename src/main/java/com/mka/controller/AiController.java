package com.mka.controller;

import com.mka.dto.response.ApiResponse;
import com.mka.dto.response.VoiceToTextResponse;
import com.mka.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Integration", description = "AI Speech-to-Text & Voice Posting APIs")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/voice-to-text")
    @Operation(summary = "Convert voice audio file to typed text (Audio is never saved)")
    public ResponseEntity<ApiResponse<VoiceToTextResponse>> voiceToText(
            @RequestParam("file") MultipartFile file) {

        VoiceToTextResponse result = aiService.processVoiceToText(file);
        return ResponseEntity.ok(
                ApiResponse.<VoiceToTextResponse>builder()
                        .success(true)
                        .message("Voice successfully converted to text")
                        .data(result)
                        .build()
        );
    }
}
