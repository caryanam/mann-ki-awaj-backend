package com.mka.controller;

import com.mka.client.openai.OpenAIClient;
import com.mka.dto.openai.OpenAIHealthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for OpenAI health and connection testing.
 * Protected under /api/admin/** (requires authentication and ADMIN authority).
 */
@RestController
@RequestMapping("/api/admin/openai")
@RequiredArgsConstructor
public class OpenAIHealthController {

    private final OpenAIClient openAIClient;

    /**
     * Connection test endpoint to verify backend communication with OpenAI API.
     *
     * @return OpenAIHealthResponse
     */
    @GetMapping("/health")
    public ResponseEntity<OpenAIHealthResponse> checkOpenAIHealth() {
        OpenAIHealthResponse healthResponse = openAIClient.checkConnection();
        return ResponseEntity.ok(healthResponse);
    }
}
