package de.jivz.ai_challenge.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class OpenRouterClientConfig {

    @Value("${openrouter.api.key}")
    private String apiKey;
    @Value("${openrouter.api.base-url}")
    private String baseUrl;
    @Value("${openrouter.api.model}")
    private String model;
    @Value("${openrouter.api.timeout:120}")
    private int timeoutSeconds;

    @Value("${openrouter.api.max-in-memory-size:16777216}") // 16MB default
    private int maxInMemorySize;


    @Bean
    @Qualifier("openRouterWebClient")
    public WebClient openRouterWebClient() {
            // Configure HTTP client with timeouts
            HttpClient httpClient = HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000) // 30 seconds connection timeout
                    .responseTimeout(Duration.ofSeconds(timeoutSeconds)) // Response timeout
                    .doOnConnected(conn -> conn
                            .addHandlerLast(new ReadTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS))
                    );

            // Configure exchange strategies to handle large responses
            ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                    .codecs(configurer -> {
                        configurer.defaultCodecs().maxInMemorySize(maxInMemorySize);
                        // Enable lenient JSON parsing
                        configurer.defaultCodecs().enableLoggingRequestDetails(true);
                    })
                    .build();

            WebClient webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .exchangeStrategies(exchangeStrategies)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build();

            log.info("OpenRouter WebClient configured with base URL: {}", baseUrl);
            log.info("Timeout: {}s, Max memory size: {} bytes", timeoutSeconds, maxInMemorySize);

            return webClient;
    }

    @Bean
    @Qualifier("openRouterModel")
    public String openRouterModel() {
        return model;
    }
}
