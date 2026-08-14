package com.mka.service.impl;

import com.mka.client.openai.OpenAIClient;
import com.mka.dto.response.VoiceToTextResponse;
import com.mka.exception.BadRequestException;
import com.mka.service.AiService;
import com.mka.entity.User;
import com.mka.entity.BlockedContent;
import com.mka.repository.BlockedContentRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    private final OpenAIClient openAIClient;
    private final BlockedContentRepository blockedContentRepository;

    @Value("${ai.moderation.enabled:true}")
    private boolean moderationEnabled;

    @Value("${ai.translation.enabled:true}")
    private boolean translationEnabled;

    private static final List<String> PROHIBITED_KEYWORDS = Arrays.asList(
            "fuck", "shit", "bitch", "bastard", "asshole",
            "hate", "kill", "terrorist", "nigger", "cunt",
            "chutiya", "madarchod", "bhenchod", "gaand", "harami",
            "marun takel", "marun takin", "marun takne", "maar dunga", "maar denge"
    );

    @Override
    public void moderateContent(User user, String text, String contentType) {
        if (!moderationEnabled || text == null || text.trim().isEmpty()) {
            return;
        }

        String lowerText = text.toLowerCase(Locale.ROOT);
        String flaggedReason = null;

        // 1. Fast Path: Local Keyword List Check
        for (String keyword : PROHIBITED_KEYWORDS) {
            if (lowerText.contains(keyword)) {
                flaggedReason = "Abusive keyword or threat detected: " + keyword;
                log.warn("Fast-path AI Moderation triggered for keyword: {}", keyword);
                break;
            }
        }

        // 2. AI Path: Multi-lingual AI Moderation via OpenAI LLM
        if (flaggedReason == null && openAIClient.isConfigured()) {
            try {
                String aiResult = openAIClient.moderateText(text);
                if (aiResult != null && aiResult.startsWith("UNSAFE")) {
                    flaggedReason = "AI Content Moderation flagged: " + aiResult.replace("UNSAFE:", "").trim();
                    log.warn("AI Multi-lingual Moderation flagged content for user {}: {}", user != null ? user.getEmail() : "anonymous", aiResult);
                }
            } catch (Exception e) {
                log.error("AI Text moderation call failed: {}. Continuing with safe path.", e.getMessage());
            }
        }

        if (flaggedReason != null) {
            saveBlockedAuditLog(user, contentType != null ? contentType : "POST", text, flaggedReason);
            throw new BadRequestException("Content blocked by AI moderation: " + flaggedReason);
        }
    }

    @Override
    public String moderateAndSaveImage(MultipartFile file, User user) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded image file is empty.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Only valid image files (JPEG, PNG, WEBP, GIF) are allowed.");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BadRequestException("Image file size exceeds the 5MB limit.");
        }

        try {
            byte[] bytes = file.getBytes();
            String moderationResult = openAIClient.moderateImage(bytes, contentType);

            if (moderationResult != null && moderationResult.startsWith("UNSAFE")) {
                log.warn("AI Image Moderation flagged image for user {}: {}", user != null ? user.getEmail() : "anonymous", moderationResult);
                String reason = moderationResult.replace("UNSAFE:", "").trim();
                saveBlockedAuditLog(user, "POST_IMAGE", "[Image File: " + file.getOriginalFilename() + "]", reason);
                throw new BadRequestException("Image content blocked by AI moderation: " + reason);
            }

            java.io.File uploadsDir = new java.io.File("uploads");
            if (!uploadsDir.exists()) {
                uploadsDir.mkdirs();
            }

            String ext = ".jpg";
            String originalName = file.getOriginalFilename();
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }

            String fileName = UUID.randomUUID().toString() + ext;
            java.io.File destFile = new java.io.File(uploadsDir, fileName).getAbsoluteFile();

            try (java.io.InputStream is = file.getInputStream()) {
                java.nio.file.Files.copy(is, destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Successfully moderated and saved post image: /uploads/{}", fileName);
            return "/uploads/" + fileName;

        } catch (BadRequestException bre) {
            throw bre;
        } catch (Exception ex) {
            log.error("Failed to process image upload: {}", ex.getMessage(), ex);
            throw new BadRequestException("Could not upload and process image file: " + ex.getMessage());
        }
    }

    private void saveBlockedAuditLog(User user, String contentType, String content, String flaggedReason) {
        try {
            String handle = "anonymous";
            String email = "anonymous@mka.com";
            if (user != null) {
                email = user.getEmail();
                handle = email.split("@")[0];
            }

            BlockedContent blocked = BlockedContent.builder()
                    .user(user)
                    .contentType(contentType)
                    .authorUsername(handle)
                    .authorEmail(email)
                    .originalContent(content)
                    .flaggedReason(flaggedReason)
                    .status("PENDING")
                    .blockedAt(LocalDateTime.now())
                    .build();

            blockedContentRepository.saveAndFlush(blocked);
            log.info("Successfully saved & committed AI blocked content footprint to DB for user: {}", email);
        } catch (Exception e) {
            log.error("Failed to save blocked content log to database: {}", e.getMessage(), e);
        }
    }

    @Override
    public String translateText(String text, String sourceLanguage, String targetLanguage) {
        if (!translationEnabled || text == null || sourceLanguage == null || targetLanguage == null) {
            return text;
        }

        if (sourceLanguage.equalsIgnoreCase(targetLanguage)) {
            return text;
        }

        return text;
    }

    @Override
    public VoiceToTextResponse processVoiceToText(MultipartFile audioFile) {
        return processVoiceToText(audioFile, null);
    }

    @Override
    public VoiceToTextResponse processVoiceToText(MultipartFile audioFile, String language) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BadRequestException("Audio file is required for speech-to-text processing.");
        }

        try {
            byte[] audioBytes = audioFile.getBytes();
            String fileName = audioFile.getOriginalFilename();
            if (fileName == null || fileName.trim().isEmpty()) {
                fileName = "voice_recording.webm";
            }
            return openAIClient.transcribeAudio(audioBytes, fileName, null, language);
        } catch (IOException e) {
            log.error("Failed to read audio file bytes for STT processing: {}", e.getMessage());
            throw new BadRequestException("Could not read uploaded audio file.");
        }
    }
}
