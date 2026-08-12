package com.mka.service;

import com.mka.dto.response.VoiceToTextResponse;
import org.springframework.web.multipart.MultipartFile;

public interface AiService {

    void moderateContent(String text);

    String translateText(String text, String sourceLanguage, String targetLanguage);

    VoiceToTextResponse processVoiceToText(MultipartFile audioFile);

    VoiceToTextResponse processVoiceToText(MultipartFile audioFile, String language);
}
