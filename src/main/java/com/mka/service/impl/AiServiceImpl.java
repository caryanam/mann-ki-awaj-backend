package com.mka.service.impl;

import com.mka.client.openai.OpenAIClient;
import com.mka.dto.response.VoiceToTextResponse;
import com.mka.exception.BadRequestException;
import com.mka.service.AiService;
import com.mka.entity.User;
import com.mka.entity.BlockedContent;
import com.mka.repository.BlockedContentRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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
            "chutiya", "madarchod", "bhenchod", "gaand", "harami"
    );

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void moderateContent(User user, String text, String contentType) {
        if (!moderationEnabled || text == null || text.trim().isEmpty()) {
            return;
        }

        String lowerText = text.toLowerCase(Locale.ROOT);
        for (String keyword : PROHIBITED_KEYWORDS) {
            if (lowerText.contains(keyword)) {
                log.warn("AI Moderation triggered for content containing prohibited keyword: {}", keyword);

                String handle = "anonymous";
                String email = "anonymous@mka.com";
                if (user != null) {
                    email = user.getEmail();
                    handle = email.split("@")[0];
                }

                BlockedContent blocked = BlockedContent.builder()
                        .user(user)
                        .contentType(contentType != null ? contentType : "POST")
                        .authorUsername(handle)
                        .authorEmail(email)
                        .originalContent(text)
                        .flaggedReason("Abusive keyword: " + keyword)
                        .status("PENDING")
                        .blockedAt(LocalDateTime.now())
                        .build();

                try {
                    blockedContentRepository.save(blocked);
                    log.info("Saved AI blocked content footprint for user: {}", email);
                } catch (Exception e) {
                    log.error("Failed to save blocked content log: {}", e.getMessage(), e);
                }

                throw new BadRequestException("Content blocked by AI moderation: contains abusive language or hate speech.");
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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

                String handle = "anonymous";
                String email = "anonymous@mka.com";
                if (user != null) {
                    email = user.getEmail();
                    handle = email.split("@")[0];
                }

                BlockedContent blocked = BlockedContent.builder()
                        .user(user)
                        .contentType("POST_IMAGE")
                        .authorUsername(handle)
                        .authorEmail(email)
                        .originalContent("[Image File: " + file.getOriginalFilename() + "]")
                        .flaggedReason(moderationResult)
                        .status("PENDING")
                        .blockedAt(LocalDateTime.now())
                        .build();

                try {
                    blockedContentRepository.save(blocked);
                } catch (Exception e) {
                    log.error("Failed to save blocked image log footprint: {}", e.getMessage());
                }

                throw new BadRequestException("Image content blocked by AI moderation: " + moderationResult.replace("UNSAFE:", "").trim());
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
