package de.jivz.ai_challenge.configuration;

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

@Slf4j
@Configuration
public class McpServerConfig {

    @Bean
    public WebClient mcpWebClient(
            @Value("${service.mcp.url}") final String mcpServerUrl,
            final WebClient.Builder webClientBuilder) {
        final ConnectionProvider connectionProvider = ConnectionProvider.builder("mcp-server-connection-provider")
                .maxIdleTime(Duration.ofSeconds(20)).lifo()
                .build();

        final HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .doOnConnected(connection ->
                        connection.addHandlerLast(new ReadTimeoutHandler(30))
                                .addHandlerLast(new WriteTimeoutHandler(30)));

        return webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(mcpServerUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public WebClient mcpPerplexityWebClient(
            @Value("${perplexity.mcp.url}") final String mcpServerUrl,
            final WebClient.Builder webClientBuilder) {
        final ConnectionProvider connectionProvider = ConnectionProvider.builder("mcp-perplexisty-server-connection-provider")
                .maxIdleTime(Duration.ofSeconds(20)).lifo()
                .build();

        final HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .doOnConnected(connection ->
                        connection.addHandlerLast(new ReadTimeoutHandler(30))
                                .addHandlerLast(new WriteTimeoutHandler(30)));

        return webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(mcpServerUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public WebClient mcpDockerMonitorWebClient(
            @Value("${docker.monitor.mcp.url}") final String mcpServerUrl,
            final WebClient.Builder webClientBuilder) {
        final ConnectionProvider connectionProvider = ConnectionProvider.builder("docker-monitor-mcp-server-connection-provider")
                .maxIdleTime(Duration.ofSeconds(20)).lifo()
                .build();

        final HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .doOnConnected(connection ->
                        connection.addHandlerLast(new ReadTimeoutHandler(30))
                                .addHandlerLast(new WriteTimeoutHandler(30)));

        return webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(mcpServerUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public WebClient ragMcpWebClient(
            @Value("${rag.mcp.url}") final String mcpServerUrl,
            final WebClient.Builder webClientBuilder) {
        final ConnectionProvider connectionProvider = ConnectionProvider.builder("rag-mcp-server-connection-provider")
                .maxIdleTime(Duration.ofSeconds(20)).lifo()
                .build();

        final HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .doOnConnected(connection ->
                        connection.addHandlerLast(new ReadTimeoutHandler(60))
                                .addHandlerLast(new WriteTimeoutHandler(60)));

        return webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(mcpServerUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

}
