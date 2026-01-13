package de.jivz.mcp.configuration;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class GoogleServiceWebclient {

    @Bean
    public WebClient gsWebClient(
            @Value("${google.service.url:http://localhost:8082}") String googleServiceUrl,
            @Value("${mcp.service.timeout.connect:10}") final int connectTimeout,
            @Value("${mcp.service.timeout.read:30}") final int readTimeout,
            @Value("${mcp.service.timeout.write:30}") final int writeTimeout,
            final WebClient.Builder webClientBuilder) {
        final ConnectionProvider connectionProvider = ConnectionProvider.builder("mcp-server-connection-provider")
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofMinutes(5))
                .lifo()
                .build();

        final HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout * 1000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .responseTimeout(Duration.ofSeconds(readTimeout))
                .doOnConnected(connection ->
                        connection.addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(writeTimeout, TimeUnit.SECONDS)));

        return webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(googleServiceUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
