package de.jivz.llmchatservice.controller;

import de.jivz.llmchatservice.config.LlmProperties;
import de.jivz.llmchatservice.dto.ChatRequest;
import de.jivz.llmchatservice.dto.ChatResponse;
import de.jivz.llmchatservice.service.LlmChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ChatController.
 * Uses WebFluxTest to test reactive endpoints with mocked service layer.
 */
@WebFluxTest(ChatController.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("ChatController Unit Tests")
class ChatControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private LlmChatService llmChatService;

    @MockBean
    private LlmProperties llmProperties;

    private ChatResponse successResponse;
    private ChatResponse errorResponse;

    @BeforeEach
    void setUp() {
        // Setup default LlmProperties mock
        when(llmProperties.getBaseUrl()).thenReturn("http://localhost:11434");
        when(llmProperties.getModel()).thenReturn("llama2");
        when(llmProperties.getTemperature()).thenReturn(0.7);
        when(llmProperties.getMaxTokens()).thenReturn(2000);
        when(llmProperties.getTimeoutSeconds()).thenReturn(120);

        // Setup success response
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("totalDuration", 1000000L);
        metadata.put("evalDuration", 500000L);

        successResponse = ChatResponse.builder()
                .response("This is a test response from the LLM")
                .model("llama2")
                .timestamp(LocalDateTime.now())
                .processingTimeMs(150L)
                .tokensGenerated(20)
                .done(true)
                .metadata(metadata)
                .build();

        // Setup error response
        errorResponse = ChatResponse.builder()
                .response(null)
                .model("llama2")
                .timestamp(LocalDateTime.now())
                .processingTimeMs(50L)
                .tokensGenerated(0)
                .done(false)
                .error("Connection timeout")
                .build();
    }

    @Test
    @DisplayName("POST /api/chat should return successful response when valid request")
    void testChat_Success() {
        // Given
        ChatRequest request = ChatRequest.builder()
                .message("Hello, how are you?")
                .build();

        when(llmChatService.chat(any(ChatRequest.class)))
                .thenReturn(Mono.just(successResponse));

        // When & Then
        webTestClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(ChatResponse.class)
                .value(response -> {
                    assertThat(response.getResponse()).isEqualTo("This is a test response from the LLM");
                    assertThat(response.getModel()).isEqualTo("llama2");
                    assertThat(response.getDone()).isTrue();
                    assertThat(response.getTokensGenerated()).isEqualTo(20);
                    assertThat(response.getError()).isNull();
                });
    }

    @Test
    @DisplayName("POST /api/chat should return 400 when message is null")
    void testChat_NullMessage() {
        // Given
        ChatRequest request = ChatRequest.builder()
                .message(null)
                .build();

        // When & Then
        webTestClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ChatResponse.class)
                .value(response -> {
                    assertThat(response.getError()).isEqualTo("Message cannot be empty");
                    assertThat(response.getDone()).isFalse();
                });
    }

    @Test
    @DisplayName("POST /api/chat should return 400 when message is empty")
    void testChat_EmptyMessage() {
        // Given
        ChatRequest request = ChatRequest.builder()
                .message("   ")
                .build();

        // When & Then
        webTestClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ChatResponse.class)
                .value(response -> {
                    assertThat(response.getError()).isEqualTo("Message cannot be empty");
                    assertThat(response.getDone()).isFalse();
                });
    }

    @Test
    @DisplayName("POST /api/chat should return 500 when service returns error response")
    void testChat_ServiceError() {
        // Given
        ChatRequest request = ChatRequest.builder()
                .message("Hello")
                .build();

        when(llmChatService.chat(any(ChatRequest.class)))
                .thenReturn(Mono.just(errorResponse));

        // When & Then
        webTestClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(ChatResponse.class)
                .value(response -> {
                    assertThat(response.getError()).isEqualTo("Connection timeout");
                    assertThat(response.getDone()).isFalse();
                    assertThat(response.getTokensGenerated()).isZero();
                });
    }

    @Test
    @DisplayName("POST /api/chat should handle service exception")
    void testChat_ServiceException() {
        // Given
        ChatRequest request = ChatRequest.builder()
                .message("Hello")
                .build();

        when(llmChatService.chat(any(ChatRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

        // When & Then
        webTestClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(ChatResponse.class)
                .value(response -> {
                    assertThat(response.getError()).contains("Internal server error");
                    assertThat(response.getError()).contains("Unexpected error");
                    assertThat(response.getDone()).isFalse();
                });
    }

    @Test
    @DisplayName("POST /api/chat should accept request with all optional parameters")
    void testChat_WithAllParameters() {
        // Given
        ChatRequest request = ChatRequest.builder()
                .message("Write a function")
                .model("codellama")
                .temperature(0.9)
                .maxTokens(1500)
                .systemPrompt("You are a helpful coding assistant")
                .stream(false)
                .build();

        when(llmChatService.chat(any(ChatRequest.class)))
                .thenReturn(Mono.just(successResponse));

        // When & Then
        webTestClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ChatResponse.class)
                .value(response -> {
                    assertThat(response.getDone()).isTrue();
                    assertThat(response.getError()).isNull();
                });
    }

    @Test
    @DisplayName("GET /api/status should return service status and configuration")
    void testStatus_Success() {
        // When & Then
        webTestClient.get()
                .uri("/api/status")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.service").isEqualTo("llm-chat-service")
                .jsonPath("$.status").isEqualTo("UP")
                .jsonPath("$.ollamaBaseUrl").isEqualTo("http://localhost:11434")
                .jsonPath("$.defaultModel").isEqualTo("llama2")
                .jsonPath("$.defaultTemperature").isEqualTo(0.7)
                .jsonPath("$.defaultMaxTokens").isEqualTo(2000)
                .jsonPath("$.timeoutSeconds").isEqualTo(120);
    }

    @Test
    @DisplayName("GET /api/models should return model information")
    void testModels_Success() {
        // When & Then
        webTestClient.get()
                .uri("/api/models")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.currentModel").isEqualTo("llama2")
                .jsonPath("$.baseUrl").isEqualTo("http://localhost:11434")
                .jsonPath("$.note").exists();
    }

    @Test
    @DisplayName("POST /api/chat should handle long messages")
    void testChat_LongMessage() {
        // Given
        String longMessage = "A".repeat(5000);
        ChatRequest request = ChatRequest.builder()
                .message(longMessage)
                .build();

        when(llmChatService.chat(any(ChatRequest.class)))
                .thenReturn(Mono.just(successResponse));

        // When & Then
        webTestClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ChatResponse.class)
                .value(response -> {
                    assertThat(response.getDone()).isTrue();
                });
    }

    @Test
    @DisplayName("POST /api/chat should handle special characters in message")
    void testChat_SpecialCharacters() {
        // Given
        ChatRequest request = ChatRequest.builder()
                .message("Hello! @#$%^&*() <test> \"quotes\" 'apostrophes' \n\t")
                .build();

        when(llmChatService.chat(any(ChatRequest.class)))
                .thenReturn(Mono.just(successResponse));

        // When & Then
        webTestClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ChatResponse.class)
                .value(response -> {
                    assertThat(response.getDone()).isTrue();
                });
    }

    @Test
    @DisplayName("POST /api/chat should handle extreme temperature values")
    void testChat_ExtremeTemperature() {
        // Given
        ChatRequest request = ChatRequest.builder()
                .message("Test message")
                .temperature(0.0)
                .build();

        when(llmChatService.chat(any(ChatRequest.class)))
                .thenReturn(Mono.just(successResponse));

        // When & Then
        webTestClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();

        // Test high temperature
        request.setTemperature(2.0);
        webTestClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();
    }
}
