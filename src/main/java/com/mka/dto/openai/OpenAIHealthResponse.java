package com.mka.dto.openai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Health and connectivity status response for OpenAI integration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAIHealthResponse {
    private String status;
    private boolean connected;
    private String provider;
    private String translationModel;
    private String transcriptionModel;
    private String moderationModel;
    private String message;
    private LocalDateTime timestamp;
}
