package de.jivz.ai_challenge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration class for Perplexity API client.
 * Centralizes all Perplexity-related configuration and WebClient setup.
 */
@Configuration
public class PerplexityClientConfig {

    @Value("${perplexity.api.base-url}")
    private String baseUrl;

    @Value("${perplexity.api.key}")
    private String apiKey;

    @Value("${perplexity.api.model}")
    private String model;

    /**
     * Creates and configures the WebClient for Perplexity API.
     *
     * @return Configured WebClient instance
     */
    @Bean(name = "perplexityWebClient")
    public WebClient perplexityWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Provides the Perplexity API model name.
     *
     * @return The model name to use for API calls
     */
    @Bean(name = "perplexityModel")
    public String perplexityModel() {
        return model;
    }

    /**
     * Provides the API key for debugging purposes (masked).
     *
     * @return Masked API key
     */
    public String getApiKeyPreview() {
        return apiKey != null && apiKey.length() > 10
            ? apiKey.substring(0, 10) + "..."
            : "***";
    }

    /**
     * Provides the base URL.
     *
     * @return Base URL for Perplexity API
     */
    public String getBaseUrl() {
        return baseUrl;
    }
}

