package com.mka.service.impl;

import com.mka.client.openai.OpenAIClient;
import com.mka.dto.response.VoiceToTextResponse;
import com.mka.exception.BadRequestException;
import com.mka.service.AiService;
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
public class
AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    private final OpenAIClient openAIClient;

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
    public void moderateContent(String text) {
        if (!moderationEnabled || text == null || text.trim().isEmpty()) {
            return;
        }

        String lowerText = text.toLowerCase(Locale.ROOT);
        for (String keyword : PROHIBITED_KEYWORDS) {
            if (lowerText.contains(keyword)) {
                log.warn("AI Moderation triggered for content containing prohibited keyword: {}", keyword);
                throw new BadRequestException("Content blocked by AI moderation: contains abusive language or hate speech.");
            }
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
