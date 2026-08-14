package com.mka.client.openai;

import com.mka.dto.openai.OpenAIHealthResponse;

/**
 * Reusable OpenAI service client contract for authentication, connection testing,
 * and future translation, transcription, and moderation operations.
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
     *
     * @param text Original text content.
     * @param sourceLanguageName Human readable source language name (e.g. English, Marathi).
     * @param targetLanguageName Human readable target language name (e.g. Marathi, English).
     * @param model Model name to use (e.g. gpt-4o-mini).
     * @return Translated text response string.
     */
    String translateText(String text, String sourceLanguageName, String targetLanguageName, String model);

    /**
     * Transcribes spoken audio bytes into text using OpenAI Audio Transcriptions API.
     *
     * @param audioBytes Raw audio bytes.
     * @param fileName File name e.g. voice_recording.webm.
     * @param model Model name e.g. whisper-1.
     * @param language Optional target speech language code e.g. HI, MR, EN.
     * @return VoiceToTextResponse containing transcribed text and detected language.
     */
    com.mka.dto.response.VoiceToTextResponse transcribeAudio(byte[] audioBytes, String fileName, String model, String language);

    /**
     * Moderates text content across multiple languages, transliterated scripts, death/violence threats,
     * hate speech, communal/religious slurs, and abuse using OpenAI AI Moderation / Vision.
     *
     * @param text Original text content.
     * @return "SAFE" or "UNSAFE: reason".
     */
    String moderateText(String text);

    /**
     * Moderates an uploaded image file using OpenAI Vision capability.
     *
     * @param imageBytes Raw image bytes.
     * @param mimeType Image MIME type (e.g. image/jpeg, image/png).
     * @return "SAFE" or "UNSAFE: reason".
     */
    String moderateImage(byte[] imageBytes, String mimeType);
}
