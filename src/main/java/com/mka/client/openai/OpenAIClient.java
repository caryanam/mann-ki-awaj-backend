package com.mka.client.openai;

import com.mka.dto.openai.OpenAIHealthResponse;
import com.mka.dto.response.ModerationResult;

/**
 * Reusable OpenAI service client contract for authentication, connection testing,
 * translation, transcription, and multimodal moderation operations.
 */
public interface OpenAIClient {

    /**
     * Checks whether the OpenAI API key is configured in the environment.
     *
     * @return true if API key is present and non-empty.
     */
    boolean isConfigured();

    /**
     * Performs a lightweight backend connection test against OpenAI API.
     *
     * @return OpenAIHealthResponse containing connectivity details without exposing sensitive keys.
     */
    OpenAIHealthResponse checkConnection();

    /**
     * Translates text using OpenAI Chat Completions API.
     */
    String translateText(String text, String sourceLanguageName, String targetLanguageName, String model);

    /**
     * Transcribes spoken audio bytes into text using OpenAI Audio Transcriptions API.
     */
    com.mka.dto.response.VoiceToTextResponse transcribeAudio(byte[] audioBytes, String fileName, String model, String language);

    /**
     * Legacy single-text moderation check.
     */
    String moderateText(String text);

    /**
     * Legacy single-image moderation check.
     */
    String moderateImage(byte[] imageBytes, String mimeType);

    /**
     * Official Multimodal Moderation using OpenAI /v1/moderations endpoint with omni-moderation-latest.
     */
    ModerationResult moderateMultimodal(String text, byte[] imageBytes, String imageMimeType);

    /**
     * AI-Powered Username Anonymity & Safety Verification.
     * Evaluates requested handle for embedded real human names, user's real name tokens, or inappropriate terms using LLM reasoning.
     */
    com.mka.dto.response.UsernameAiCheckResult validateUsernameWithAi(String username, String userFullName);
}
