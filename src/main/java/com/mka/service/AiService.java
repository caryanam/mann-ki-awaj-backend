package com.mka.service;

import com.mka.dto.response.VoiceToTextResponse;
import com.mka.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface AiService {

    void moderateContent(User user, String text, String contentType);

    String moderateAndSaveImage(MultipartFile imageFile, User user);

    String translateText(String text, String sourceLanguage, String targetLanguage);

    VoiceToTextResponse processVoiceToText(MultipartFile audioFile);

    VoiceToTextResponse processVoiceToText(MultipartFile audioFile, String language);
}
