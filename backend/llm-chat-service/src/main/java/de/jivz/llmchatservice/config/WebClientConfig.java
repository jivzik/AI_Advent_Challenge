package de.jivz.llmchatservice.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient configuration for Ollama LLM integration.
 * Configures reactive HTTP client with proper timeouts and error handling.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final LlmProperties llmProperties;

    /**
     * Creates a configured WebClient bean for Ollama API calls.
     * Includes:
     * - Connection pooling with keep-alive
     * - Optimized timeouts (3s connect, configurable read/write)
     * - Large buffer for LLM responses (16MB)
     * - Request/response logging
     * - Prefer IPv4 to avoid DNS resolution delays
     *
     * @return Configured WebClient instance
     */
    @Bean
    public WebClient ollamaWebClient() {
        // Configure connection pool with keep-alive
        ConnectionProvider connectionProvider = ConnectionProvider.builder("ollama-pool")
                .maxConnections(50)
                .maxIdleTime(Duration.ofSeconds(60))
                .maxLifeTime(Duration.ofSeconds(120))
                .pendingAcquireTimeout(Duration.ofSeconds(5))
                .evictInBackground(Duration.ofSeconds(30))
                .build();

        // Configure Netty HTTP client with optimized timeouts
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000) // Fast connect timeout
                .option(ChannelOption.SO_KEEPALIVE, true) // Enable TCP keep-alive
                .responseTimeout(Duration.ofSeconds(llmProperties.getTimeoutSeconds()))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(llmProperties.getTimeoutSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(llmProperties.getTimeoutSeconds(), TimeUnit.SECONDS))
                );

        // Configure exchange strategies for large responses
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024); // 16MB buffer
                    configurer.defaultCodecs().enableLoggingRequestDetails(true);
                })
                .build();

        // Resolve base URL (prefer 127.0.0.1 over localhost to avoid DNS lookup)
        String baseUrl = llmProperties.getBaseUrl().replace("localhost", "127.0.0.1");
        log.info("Configuring WebClient with baseUrl: {}", baseUrl);

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(exchangeStrategies)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    /**
     * Logs outgoing requests for debugging
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.debug("Request: {} {}", request.method(), request.url());
            request.headers().forEach((name, values) ->
                    values.forEach(value -> log.debug("  {}: {}", name, value))
            );
            return Mono.just(request);
        });
    }

    /**
     * Logs incoming responses for debugging
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.debug("Response status: {}", response.statusCode());
            return Mono.just(response);
        });
    }

}
