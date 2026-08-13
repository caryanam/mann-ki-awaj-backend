package com.mka.translation.controller;

import com.mka.translation.dto.TranslationRequest;
import com.mka.translation.dto.TranslationResponse;
import com.mka.translation.service.TranslationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller exposing translation endpoints to frontend applications.
 */
@RestController
@RequestMapping("/api/v1/translation")
public class TranslationController {

    private final TranslationService translationService;

    public TranslationController(TranslationService translationService) {
        this.translationService = translationService;
    }

    @PostMapping("/translate")
    public ResponseEntity<TranslationResponse> translate(@Valid @RequestBody TranslationRequest request) {
        String srcLang = request.getSourceLanguage();
        if (srcLang == null || srcLang.trim().isEmpty() || "null".equalsIgnoreCase(srcLang.trim()) || "undefined".equalsIgnoreCase(srcLang.trim())) {
            srcLang = "auto";
        }
        TranslationResponse response = translationService.translate(
                request.getText(),
                srcLang,
                request.getTargetLanguage()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        boolean available = translationService.isTranslationServiceAvailable();
        return ResponseEntity.ok(Map.of(
                "status", available ? "UP" : "DOWN",
                "service", "OpenAI Translation Service"
        ));
    }
}
