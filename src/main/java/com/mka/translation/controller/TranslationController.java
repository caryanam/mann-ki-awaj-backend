package com.mka.translation.controller;

import com.mka.translation.dto.TranslationRequest;
import com.mka.translation.dto.TranslationResponse;
import com.mka.translation.service.TranslationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * REST Controller exposing translation endpoints to frontend applications.
 * Includes IP-based rate limiting (30 requests per minute per IP) to prevent OpenAI API abuse.
 */
@RestController
@RequestMapping("/api/v1/translation")
public class TranslationController {

    private static final int MAX_REQUESTS_PER_MINUTE = 30;
    private final ConcurrentHashMap<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();
    private final TranslationService translationService;

    public TranslationController(TranslationService translationService) {
        this.translationService = translationService;
    }

    private static class RequestCounter {
        final AtomicInteger count = new AtomicInteger(0);
        final long resetTimeMs;

        RequestCounter(long resetTimeMs) {
            this.resetTimeMs = resetTimeMs;
        }
    }

    private boolean isRateLimited(HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = httpRequest.getRemoteAddr();
        } else {
            clientIp = clientIp.split(",")[0].trim();
        }

        long now = System.currentTimeMillis();
        RequestCounter counter = requestCounts.compute(clientIp, (ip, current) -> {
            if (current == null || now > current.resetTimeMs) {
                RequestCounter fresh = new RequestCounter(now + 60_000);
                fresh.count.set(1);
                return fresh;
            } else {
                current.count.incrementAndGet();
                return current;
            }
        });

        return counter.count.get() > MAX_REQUESTS_PER_MINUTE;
    }

    @PostMapping("/translate")
    public ResponseEntity<?> translate(@Valid @RequestBody TranslationRequest request, HttpServletRequest httpRequest) {
        if (isRateLimited(httpRequest)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "success", false,
                    "status", 429,
                    "message", "Rate limit exceeded. Maximum 30 translation requests per minute allowed."
            ));
        }

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
