package de.jivz.supportservice.config;

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
 * WebClient configuration für Ollama LLM Integration
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class OllamaWebClientConfig {

    private final OllamaProperties ollamaProperties;

    /**
     * Erstellt einen konfigurierten WebClient für Ollama API-Aufrufe.
     */
    @Bean(name = "ollamaWebClient")
    public WebClient ollamaWebClient() {
        // Connection Pool konfigurieren
        ConnectionProvider connectionProvider = ConnectionProvider.builder("ollama-pool")
                .maxConnections(20)
                .maxIdleTime(Duration.ofSeconds(60))
                .maxLifeTime(Duration.ofSeconds(120))
                .pendingAcquireTimeout(Duration.ofSeconds(5))
                .evictInBackground(Duration.ofSeconds(30))
                .build();

        // Netty HTTP Client mit optimierten Timeouts
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .responseTimeout(Duration.ofSeconds(ollamaProperties.getTimeoutSeconds()))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(ollamaProperties.getTimeoutSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(ollamaProperties.getTimeoutSeconds(), TimeUnit.SECONDS))
                );

        // Exchange Strategies für große Responses
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024); // 16MB buffer
                    configurer.defaultCodecs().enableLoggingRequestDetails(true);
                })
                .build();

        // Base URL optimieren (localhost -> 127.0.0.1)
        String baseUrl = ollamaProperties.getBaseUrl().replace("localhost", "127.0.0.1");
        log.info("🤖 Configuring Ollama WebClient with baseUrl: {}, model: {}",
                baseUrl, ollamaProperties.getModel());

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

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.debug("🔵 Ollama Request: {} {}", request.method(), request.url());
            return Mono.just(request);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.debug("🟢 Ollama Response: {}", response.statusCode());
            return Mono.just(response);
        });
    }
}

