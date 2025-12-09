package de.jivz.ai_challenge.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OpenRouterClientConfig {

    @Value("${openrouter.api.key}")
    private String apiKey;
    @Value("${openrouter.api.base-url}")
    private String baseUrl;
    @Value("${openrouter.api.model}")
    private String model;

    @Bean
    @Qualifier("openRouterWebClient")
    public WebClient openRouterWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean
    @Qualifier("openRouterModel")
    public String openRouterModel() {
        return model;
    }
}
