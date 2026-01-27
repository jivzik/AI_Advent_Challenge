package de.jivz.ai_challenge.openrouterservice.service;

import de.jivz.ai_challenge.openrouterservice.dto.ChatResponse;
import de.jivz.ai_challenge.openrouterservice.dto.VoiceAgentRequest;
import de.jivz.ai_challenge.openrouterservice.dto.VoiceAgentResponse;
import de.jivz.ai_challenge.openrouterservice.dto.WhisperResponse;
import de.jivz.ai_challenge.openrouterservice.service.client.OpenRouterApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Voice Agent Service - Pipeline: Audio → Speech-to-Text (Gemini) → LLM (OpenRouter) → Text Response
 * Verwendet OpenRouterApiClient und PromptLoaderService für saubere Architektur
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceAgentService {

    private final OpenRouterApiClient openRouterClient;
    private final OpenRouterAiChatService chatService;
    private final PromptLoaderService promptLoader;

    /**
     * Vollständiger Voice Agent Pipeline
     * 1. Audio → Text (Google Gemini Flash)
     * 2. Text → LLM Processing (OpenRouter)
     * 3. Return Response
     */
    public VoiceAgentResponse processVoiceCommand(MultipartFile audioFile, VoiceAgentRequest request) {
        log.info("Starting voice agent pipeline for user: {}", request.getUserId());
        long startTime = System.currentTimeMillis();

        try {
            // Schritt 1: Audio transkribieren mit Gemini Flash
            long transcriptionStart = System.currentTimeMillis();
            WhisperResponse transcriptionResponse = transcribeAudio(audioFile, request.getLanguage());
            long transcriptionTime = System.currentTimeMillis() - transcriptionStart;

            log.info("Transcription completed in {} ms: {}", transcriptionTime,
                    transcriptionResponse.getText().substring(0, Math.min(50, transcriptionResponse.getText().length())));

            // Schritt 2: LLM Processing mit OpenRouter
            long llmStart = System.currentTimeMillis();

            String userMessage = transcriptionResponse.getText();
            if (request.getSystemPrompt() != null) {
                userMessage = request.getSystemPrompt() + "\n\nUser: " + transcriptionResponse.getText();
            }

            ChatResponse llmResponse = chatService.chat(
                    userMessage,
                    request.getModel(),
                    request.getTemperature(),
                    null
            );
            long llmTime = System.currentTimeMillis() - llmStart;

            log.info("LLM processing completed in {} ms", llmTime);

            // Schritt 3: Response zusammenstellen
            long totalTime = System.currentTimeMillis() - startTime;

            return VoiceAgentResponse.builder()
                    .transcription(transcriptionResponse.getText())
                    .response(llmResponse.getReply())
                    .timestamp(LocalDateTime.now())
                    .language(transcriptionResponse.getLanguage())
                    .transcriptionTimeMs(transcriptionTime)
                    .llmProcessingTimeMs(llmTime)
                    .totalTimeMs(totalTime)
                    .model(llmResponse.getModel())
                    .userId(request.getUserId())
                    .build();

        } catch (Exception e) {
            log.error("Error in voice agent pipeline", e);
            throw new RuntimeException("Voice agent processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Audio-Transkription mit Google Gemini Flash (über OpenRouter)
     * Verwendet OpenRouterApiClient und PromptLoaderService
     */
    public WhisperResponse transcribeAudio(MultipartFile audioFile, String language) {
        log.info("🎤 Transcribing audio file with Gemini Flash: {} (size: {} bytes)",
                audioFile.getOriginalFilename(), audioFile.getSize());

        try {
            // Validierung
            if (audioFile.isEmpty()) {
                throw new IllegalArgumentException("Audio file is empty");
            }

            String filename = audioFile.getOriginalFilename();
            if (filename == null || !isSupportedAudioFormat(filename)) {
                throw new IllegalArgumentException(
                        "Unsupported audio format. Supported: mp3, mp4, mpeg, mpga, m4a, wav, webm");
            }

            // Audio zu Base64 konvertieren
            byte[] audioBytes = audioFile.getBytes();
            String base64Audio = java.util.Base64.getEncoder().encodeToString(audioBytes);
            String audioFormat = getAudioFormat(filename);

            // Prompt aus PromptLoaderService laden
            String languageDisplay = getLanguageDisplay(language);
            String transcriptionPrompt = promptLoader.buildAudioTranscriptionPrompt(languageDisplay);

            log.debug("📝 Using transcription prompt with language: {}", languageDisplay);
            log.debug("🎵 Audio format: {}", audioFormat);

            // API Call über OpenRouterApiClient
            String transcribedText = openRouterClient.sendAudioTranscriptionRequest(
                    transcriptionPrompt,
                    base64Audio,
                    audioFormat
            );

            if (transcribedText == null || transcribedText.isEmpty()) {
                throw new RuntimeException("No transcription found in response");
            }

            log.info("✅ Transcription successful: {} characters", transcribedText.length());

            return WhisperResponse.builder()
                    .text(transcribedText)
                    .language(language != null ? language : "auto")
                    .model("google/gemini-2.0-flash-exp")
                    .build();

        } catch (IOException e) {
            log.error("❌ Error reading audio file", e);
            throw new RuntimeException("Failed to read audio file: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Error during transcription", e);
            throw new RuntimeException("Transcription failed: " + e.getMessage(), e);
        }
    }

    /**
     * Konvertiert ISO-639-1 Code zu vollständigem Sprachnamen
     */
    private String getLanguageDisplay(String languageCode) {
        if (languageCode == null || languageCode.isEmpty()) {
            return "the original language";
        }

        return switch (languageCode.toLowerCase()) {
            case "de" -> "German";
            case "en" -> "English";
            case "es" -> "Spanish";
            case "fr" -> "French";
            case "it" -> "Italian";
            case "pt" -> "Portuguese";
            case "ru" -> "Russian";
            case "ja" -> "Japanese";
            case "zh" -> "Chinese";
            case "ar" -> "Arabic";
            default -> languageCode;
        };
    }

    /**
     * Ermittelt Audio-Format basierend auf Dateiendung (für OpenRouter API)
     */
    private String getAudioFormat(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".mp3")) return "mp3";
        if (lower.endsWith(".mp4")) return "mp4";
        if (lower.endsWith(".mpeg")) return "mpeg";
        if (lower.endsWith(".mpga")) return "mpga";
        if (lower.endsWith(".m4a")) return "m4a";
        if (lower.endsWith(".wav")) return "wav";
        if (lower.endsWith(".webm")) return "webm";
        if (lower.endsWith(".aiff")) return "aiff";
        if (lower.endsWith(".aac")) return "aac";
        if (lower.endsWith(".ogg")) return "ogg";
        if (lower.endsWith(".flac")) return "flac";
        return "mp3"; // Default
    }

    /**
     * Prüft, ob das Audio-Format unterstützt wird
     */
    private boolean isSupportedAudioFormat(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".mp3") || lower.endsWith(".mp4") ||
               lower.endsWith(".mpeg") || lower.endsWith(".mpga") ||
               lower.endsWith(".m4a") || lower.endsWith(".wav") ||
               lower.endsWith(".webm") || lower.endsWith(".aiff") ||
               lower.endsWith(".aac") || lower.endsWith(".ogg") ||
               lower.endsWith(".flac");
    }
}
