package de.jivz.rag.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Конфигурация WebClient для OpenRouter API.
 */
@Configuration
@Slf4j
public class OpenRouterConfig {

    @Value("${openrouter.api.url}")
    private String apiUrl;

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.api.max-buffer-size:16777216}")
    private int maxBufferSize; // 16 MB default

    /**
     * WebClient для OpenRouter Embeddings API.
     */
    @Bean
    public WebClient openRouterEmbeddingWebClient() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(maxBufferSize))
                .build();

        WebClient webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .exchangeStrategies(strategies)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("HTTP-Referer", "https://rag-mcp-server.local")
                .defaultHeader("X-Title", "RAG MCP Server")
                .build();

        log.info("🔌 OpenRouter WebClient configured: url={}, bufferSize={}MB",
                apiUrl, maxBufferSize / (1024 * 1024));

        return webClient;
    }
}

