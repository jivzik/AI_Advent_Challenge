package de.jivz.ai_challenge.openrouterservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties für OpenRouter Integration mit Spring AI
 * und Google Gemini Flash für Audio-Transkription
 */
@Component
@ConfigurationProperties(prefix = "spring.ai.openrouter")
@Data
public class OpenRouterProperties {
    private String apiKey;
    private String baseUrl = "https://openrouter.ai/api/v1";
    private String defaultModel = "openrouter/auto";
    private Double defaultTemperature = 0.7;
    private Integer defaultMaxTokens = 1000;
    private Double defaultTopP = 0.9;

    // Audio Transcription Configuration (Google Gemini Flash 2.0)
    private String transcriptionModel = "google/gemini-2.0-flash-exp:free";
    private String transcriptionApiUrl = "https://openrouter.ai/api/v1/chat/completions";
}



