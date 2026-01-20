package de.jivz.llmchatservice.service;

import de.jivz.llmchatservice.config.LlmProperties;
import de.jivz.llmchatservice.dto.ChatRequest;
import de.jivz.llmchatservice.dto.ChatResponse;
import de.jivz.llmchatservice.dto.OllamaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LlmChatService.
 * Tests WebClient interactions and response mapping with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LlmChatService Unit Tests")
class LlmChatServiceTest {

    @Mock
    private WebClient ollamaWebClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private LlmProperties llmProperties;

    private LlmChatService llmChatService;

    @BeforeEach
    void setUp() {
        // Setup default LlmProperties
        lenient().when(llmProperties.getModel()).thenReturn("llama2");
        lenient().when(llmProperties.getTemperature()).thenReturn(0.7);
        lenient().when(llmProperties.getMaxTokens()).thenReturn(2000);
        lenient().when(llmProperties.getBaseUrl()).thenReturn("http://localhost:11434");

        llmChatService = new LlmChatService(ollamaWebClient, llmProperties);
    }

    private void setupWebClientMockChain() {
        when(ollamaWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @DisplayName("chat should return successful response when Ollama API responds correctly")
    void testChat_Success() {
        // Given
        setupWebClientMockChain();

        ChatRequest request = ChatRequest.builder()
                .message("Hello, how are you?")
                .build();

        OllamaResponse ollamaResponse = OllamaResponse.builder()
                .model("llama2")
                .response("I'm doing well, thank you for asking!")
                .done(true)
                .evalCount(15)
                .totalDuration(1500000000L)
                .loadDuration(50000000L)
                .promptEvalCount(5)
                .promptEvalDuration(100000000L)
                .evalDuration(800000000L)
                .build();

        when(responseSpec.bodyToMono(OllamaResponse.class))
                .thenReturn(Mono.just(ollamaResponse));

        // When
        Mono<ChatResponse> result = llmChatService.chat(request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getResponse()).isEqualTo("I'm doing well, thank you for asking!");
                    assertThat(response.getModel()).isEqualTo("llama2");
                    assertThat(response.getDone()).isTrue();
                    assertThat(response.getTokensGenerated()).isEqualTo(15);
                    assertThat(response.getError()).isNull();
                    assertThat(response.getProcessingTimeMs()).isGreaterThanOrEqualTo(0);
                    assertThat(response.getTimestamp()).isNotNull();
                    assertThat(response.getMetadata()).isNotNull();
                })
                .verifyComplete();

        verify(ollamaWebClient).post();
        verify(requestBodyUriSpec).uri("/api/generate");
    }

    @Test
    @DisplayName("chat should use custom model when provided in request")
    void testChat_CustomModel() {
        // Given
        setupWebClientMockChain();

        ChatRequest request = ChatRequest.builder()
                .message("Write a function")
                .model("codellama")
                .build();

        OllamaResponse ollamaResponse = OllamaResponse.builder()
                .model("codellama")
                .response("def example():\n    pass")
                .done(true)
                .evalCount(10)
                .build();

        when(responseSpec.bodyToMono(OllamaResponse.class))
                .thenReturn(Mono.just(ollamaResponse));

        // When
        Mono<ChatResponse> result = llmChatService.chat(request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getModel()).isEqualTo("codellama");
                    assertThat(response.getResponse()).contains("def example()");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("chat should use custom temperature when provided in request")
    void testChat_CustomTemperature() {
        // Given
        setupWebClientMockChain();

        ChatRequest request = ChatRequest.builder()
                .message("Be creative")
                .temperature(1.5)
                .build();

        OllamaResponse ollamaResponse = OllamaResponse.builder()
                .model("llama2")
                .response("Creative response")
                .done(true)
                .evalCount(5)
                .build();

        when(responseSpec.bodyToMono(OllamaResponse.class))
                .thenReturn(Mono.just(ollamaResponse));

        // When
        Mono<ChatResponse> result = llmChatService.chat(request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getResponse()).isEqualTo("Creative response");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("chat should use custom maxTokens when provided in request")
    void testChat_CustomMaxTokens() {
        // Given
        setupWebClientMockChain();

        ChatRequest request = ChatRequest.builder()
                .message("Short answer please")
                .maxTokens(100)
                .build();

        OllamaResponse ollamaResponse = OllamaResponse.builder()
                .model("llama2")
                .response("Short response")
                .done(true)
                .evalCount(3)
                .build();

        when(responseSpec.bodyToMono(OllamaResponse.class))
                .thenReturn(Mono.just(ollamaResponse));

        // When
        Mono<ChatResponse> result = llmChatService.chat(request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getTokensGenerated()).isEqualTo(3);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("chat should include system prompt when provided")
    void testChat_WithSystemPrompt() {
        // Given
        setupWebClientMockChain();

        ChatRequest request = ChatRequest.builder()
                .message("Help me code")
                .systemPrompt("You are a helpful coding assistant")
                .build();

        OllamaResponse ollamaResponse = OllamaResponse.builder()
                .model("llama2")
                .response("Sure, I can help you with coding!")
                .done(true)
                .evalCount(12)
                .build();

        when(responseSpec.bodyToMono(OllamaResponse.class))
                .thenReturn(Mono.just(ollamaResponse));

        // When
        Mono<ChatResponse> result = llmChatService.chat(request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getResponse()).contains("coding");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("chat should handle connection timeout error")
    void testChat_ConnectionTimeout() {
        // Given
        setupWebClientMockChain();

        ChatRequest request = ChatRequest.builder()
                .message("Hello")
                .build();

        // Use a generic RuntimeException to simulate connection issues
        when(responseSpec.bodyToMono(OllamaResponse.class))
                .thenReturn(Mono.error(new RuntimeException("Connection timed out")));

        // When
        Mono<ChatResponse> result = llmChatService.chat(request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getError()).isNotNull();
                    assertThat(response.getError()).contains("Failed to get response from Ollama");
                    assertThat(response.getDone()).isFalse();
                    assertThat(response.getTokensGenerated()).isZero();
                    assertThat(response.getResponse()).isNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("chat should handle 500 error from Ollama API")
    void testChat_ServerError() {
        // Given
        setupWebClientMockChain();

        ChatRequest request = ChatRequest.builder()
                .message("Hello")
                .build();

        when(responseSpec.bodyToMono(OllamaResponse.class))
                .thenReturn(Mono.error(WebClientResponseException.create(
                        500,
                        "Internal Server Error",
                        null, null, null)));

        // When
        Mono<ChatResponse> result = llmChatService.chat(request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getError()).isNotNull();
                    assertThat(response.getDone()).isFalse();
                    assertThat(response.getResponse()).isNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("chat should handle 404 error when model not found")
    void testChat_ModelNotFound() {
        // Given
        setupWebClientMockChain();

        ChatRequest request = ChatRequest.builder()
                .message("Hello")
                .model("nonexistent-model")
                .build();

        when(responseSpec.bodyToMono(OllamaResponse.class))
                .thenReturn(Mono.error(WebClientResponseException.create(
                        404,
                        "Not Found",
                        null, null, null)));

        // When
        Mono<ChatResponse> result = llmChatService.chat(request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getError()).isNotNull();
                    assertThat(response.getDone()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("chat should handle generic runtime exception")
    void testChat_RuntimeException() {
        // Given
        setupWebClientMockChain();

        ChatRequest request = ChatRequest.builder()
                .message("Hello")
                .build();

        when(responseSpec.bodyToMono(OllamaResponse.class))
                .thenReturn(Mono.error(new RuntimeException("Unexpected error occurred")));

        // When
        Mono<ChatResponse> result = llmChatService.chat(request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getError()).contains("Unexpected error occurred");
                    assertThat(response.getDone()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("chat should handle null response from Ollama")
    void testChat_NullResponse() {
        // Given
        setupWebClientMockChain();

        ChatRequest request = ChatRequest.builder()
                .message("Hello")
                .build();

        OllamaResponse ollamaResponse = OllamaResponse.builder()
                .model("llama2")
                .response(null)
                .done(true)
                .evalCount(0)
                .build();

        when(responseSpec.bodyToMono(OllamaResponse.class))
                .thenReturn(Mono.just(ollamaResponse));

        // When
        Mono<ChatResponse> result = llmChatService.chat(request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getResponse()).isNull();
                    assertThat(response.getDone()).isTrue();
                    assertThat(response.getError()).isNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("chat should handle incomplete response from Ollama")
    void testChat_IncompleteResponse() {
        // Given
        setupWebClientMockChain();

        ChatRequest request = ChatRequest.builder()
                .message("Long story")
                .build();

        OllamaResponse ollamaResponse = OllamaResponse.builder()
                .model("llama2")
                .response("Once upon a time...")
                .done(false)
                .evalCount(5)
                .build();

        when(responseSpec.bodyToMono(OllamaResponse.class))
                .thenReturn(Mono.just(ollamaResponse));

        // When
        Mono<ChatResponse> result = llmChatService.chat(request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getResponse()).isEqualTo("Once upon a time...");
                    assertThat(response.getDone()).isFalse();
                    assertThat(response.getError()).isNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("chat should properly map metadata from Ollama response")
    void testChat_MetadataMapping() {
        // Given
        setupWebClientMockChain();

        ChatRequest request = ChatRequest.builder()
                .message("Test")
                .build();

        OllamaResponse ollamaResponse = OllamaResponse.builder()
                .model("llama2")
                .response("Test response")
                .done(true)
                .evalCount(10)
                .totalDuration(2000000000L)
                .loadDuration(100000000L)
                .promptEvalCount(8)
                .promptEvalDuration(500000000L)
                .evalDuration(1200000000L)
                .build();

        when(responseSpec.bodyToMono(OllamaResponse.class))
                .thenReturn(Mono.just(ollamaResponse));

        // When
        Mono<ChatResponse> result = llmChatService.chat(request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getMetadata()).isNotNull();
                    assertThat(response.getMetadata()).isInstanceOf(java.util.Map.class);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("chat should track processing time correctly")
    void testChat_ProcessingTime() {
        // Given
        setupWebClientMockChain();

        ChatRequest request = ChatRequest.builder()
                .message("Quick question")
                .build();

        OllamaResponse ollamaResponse = OllamaResponse.builder()
                .model("llama2")
                .response("Quick answer")
                .done(true)
                .evalCount(3)
                .build();

        when(responseSpec.bodyToMono(OllamaResponse.class))
                .thenReturn(Mono.just(ollamaResponse).delayElement(Duration.ofMillis(100)));

        // When
        Mono<ChatResponse> result = llmChatService.chat(request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getProcessingTimeMs()).isGreaterThanOrEqualTo(100);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("chat should use default values when request parameters are null")
    void testChat_DefaultValues() {
        // Given
        setupWebClientMockChain();

        ChatRequest request = ChatRequest.builder()
                .message("Test with defaults")
                .model(null)
                .temperature(null)
                .maxTokens(null)
                .systemPrompt(null)
                .stream(null)
                .build();

        OllamaResponse ollamaResponse = OllamaResponse.builder()
                .model("llama2")
                .response("Using defaults")
                .done(true)
                .evalCount(5)
                .build();

        when(responseSpec.bodyToMono(OllamaResponse.class))
                .thenReturn(Mono.just(ollamaResponse));

        // When
        Mono<ChatResponse> result = llmChatService.chat(request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getModel()).isEqualTo("llama2");
                    assertThat(response.getResponse()).isEqualTo("Using defaults");
                })
                .verifyComplete();

        verify(llmProperties, atLeastOnce()).getModel();
        verify(llmProperties, atLeastOnce()).getTemperature();
        verify(llmProperties, atLeastOnce()).getMaxTokens();
    }
}
