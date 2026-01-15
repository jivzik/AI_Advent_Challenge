package de.jivz.teamassistantservice.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient-basierte Konfiguration für OpenRouter API
 * Verwendet den bewährten Ansatz aus dem perplexity-service
 */
@Slf4j
@Configuration
public class OpenRouterWebClientConfig {

    private static final int CONNECT_TIMEOUT_MS = 30000;
    private static final int TIMEOUT_SECONDS = 120;
    private static final int MAX_IN_MEMORY_SIZE = 16777216; // 16MB

    @Bean
    @Primary
    @Qualifier("openRouterWebClient")
    public WebClient openRouterWebClient(OpenRouterProperties properties) {
        // Configure HTTP client with timeouts
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .responseTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(TIMEOUT_SECONDS, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(TIMEOUT_SECONDS, TimeUnit.SECONDS))
                );

        // Configure exchange strategies to handle large responses
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE);
                    configurer.defaultCodecs().enableLoggingRequestDetails(true);
                })
                .build();

        WebClient webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(exchangeStrategies)
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();

        log.info("OpenRouter WebClient configured with base URL: {}", properties.getBaseUrl());
        log.info("Timeout: {}s, Max memory size: {} bytes", TIMEOUT_SECONDS, MAX_IN_MEMORY_SIZE);

        return webClient;
    }

}

