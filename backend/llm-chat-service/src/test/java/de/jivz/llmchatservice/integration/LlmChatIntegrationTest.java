package de.jivz.llmchatservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import de.jivz.llmchatservice.dto.ChatRequest;
import de.jivz.llmchatservice.dto.ChatResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for LLM Chat Service.
 * Uses WireMock to mock Ollama API and tests the full request/response flow.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("LlmChatService Integration Tests")
class LlmChatIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static WireMockServer wireMockServer;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (wireMockServer == null) {
            // Use dynamic port (0) to avoid port conflicts
            wireMockServer = new WireMockServer(0);
            wireMockServer.start();
        }
        int port = wireMockServer.port();
        WireMock.configureFor("localhost", port);
        registry.add("llm.ollama.base-url", () -> "http://localhost:" + port);
    }

    @BeforeEach
    void setUp() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.resetAll();
        }
    }

    @AfterEach
    void tearDown() {
        // Do not stop WireMock server here to avoid connection refused errors
        // It will be stopped after all tests complete
    }

    @Test
    @DisplayName("Full chat flow should work end-to-end with successful Ollama response")
    void testFullChatFlow_Success() {
        // Given - Mock Ollama API response
        String ollamaResponseJson = """
                {
                    "model": "llama2",
                    "response": "Hello! I'm doing well, thank you for asking. How can I help you today?",
                    "done": true,
                    "total_duration": 1500000000,
                    "load_duration": 50000000,
                    "prompt_eval_count": 10,
                    "prompt_eval_duration": 200000000,
                    "eval_count": 20,
                    "eval_duration": 800000000
                }
                """;

        stubFor(post(urlEqualTo("/api/generate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ollamaResponseJson)));

        ChatRequest request = ChatRequest.builder()
                .message("Hello, how are you?")
                .build();

        // When
        ResponseEntity<ChatResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/chat",
                request,
                ChatResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getResponse()).contains("Hello!");
        assertThat(response.getBody().getModel()).isEqualTo("llama2");
        assertThat(response.getBody().getDone()).isTrue();
        assertThat(response.getBody().getTokensGenerated()).isEqualTo(20);
        assertThat(response.getBody().getError()).isNull();
        assertThat(response.getBody().getProcessingTimeMs()).isGreaterThan(0);
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getMetadata()).isNotNull();

        // Verify the request sent to Ollama
        verify(postRequestedFor(urlEqualTo("/api/generate"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(matchingJsonPath("$.model", equalTo("test-model")))
                .withRequestBody(matchingJsonPath("$.prompt", equalTo("Hello, how are you?")))
                .withRequestBody(matchingJsonPath("$.stream", equalTo("false"))));
    }

    @Test
    @DisplayName("Should handle custom model parameter correctly")
    void testChatFlow_CustomModel() {
        // Given
        String ollamaResponseJson = """
                {
                    "model": "codellama",
                    "response": "def hello():\\n    print('Hello, World!')",
                    "done": true,
                    "eval_count": 15
                }
                """;

        stubFor(post(urlEqualTo("/api/generate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ollamaResponseJson)));

        ChatRequest request = ChatRequest.builder()
                .message("Write a hello world function")
                .model("codellama")
                .build();

        // When
        ResponseEntity<ChatResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/chat",
                request,
                ChatResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getModel()).isEqualTo("codellama");
        assertThat(response.getBody().getResponse()).contains("def hello()");

        // Verify model parameter was sent correctly
        verify(postRequestedFor(urlEqualTo("/api/generate"))
                .withRequestBody(matchingJsonPath("$.model", equalTo("codellama"))));
    }

    @Test
    @DisplayName("Should handle temperature and maxTokens parameters")
    void testChatFlow_CustomParameters() {
        // Given
        String ollamaResponseJson = """
                {
                    "model": "llama2",
                    "response": "Creative response",
                    "done": true,
                    "eval_count": 10
                }
                """;

        stubFor(post(urlEqualTo("/api/generate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ollamaResponseJson)));

        ChatRequest request = ChatRequest.builder()
                .message("Be creative")
                .temperature(1.2)
                .maxTokens(500)
                .build();

        // When
        ResponseEntity<ChatResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/chat",
                request,
                ChatResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify parameters were sent correctly
        verify(postRequestedFor(urlEqualTo("/api/generate"))
                .withRequestBody(matchingJsonPath("$.options.temperature", equalTo("1.2")))
                .withRequestBody(matchingJsonPath("$.options.num_predict", equalTo("500"))));
    }

    @Test
    @DisplayName("Should handle system prompt parameter")
    void testChatFlow_SystemPrompt() {
        // Given
        String ollamaResponseJson = """
                {
                    "model": "llama2",
                    "response": "Sure, I can help you with Python!",
                    "done": true,
                    "eval_count": 12
                }
                """;

        stubFor(post(urlEqualTo("/api/generate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ollamaResponseJson)));

        ChatRequest request = ChatRequest.builder()
                .message("How do I write a function?")
                .systemPrompt("You are a helpful Python programming assistant")
                .build();

        // When
        ResponseEntity<ChatResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/chat",
                request,
                ChatResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify system prompt was sent
        verify(postRequestedFor(urlEqualTo("/api/generate"))
                .withRequestBody(matchingJsonPath("$.system",
                        equalTo("You are a helpful Python programming assistant"))));
    }

    @Test
    @DisplayName("Should return 400 for empty message")
    void testChatFlow_EmptyMessage() {
        // Given
        ChatRequest request = ChatRequest.builder()
                .message("")
                .build();

        // When
        ResponseEntity<ChatResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/chat",
                request,
                ChatResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError()).isEqualTo("Message cannot be empty");
        assertThat(response.getBody().getDone()).isFalse();

        // Verify no request was made to Ollama
        verify(0, postRequestedFor(urlEqualTo("/api/generate")));
    }

    @Test
    @DisplayName("Should handle 500 error from Ollama API")
    void testChatFlow_OllamaServerError() {
        // Given
        stubFor(post(urlEqualTo("/api/generate"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        ChatRequest request = ChatRequest.builder()
                .message("Hello")
                .build();

        // When
        ResponseEntity<ChatResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/chat",
                request,
                ChatResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getError()).isNotNull();
        assertThat(response.getBody().getDone()).isFalse();
        assertThat(response.getBody().getResponse()).isNull();
    }

    @Test
    @DisplayName("Should handle connection timeout")
    void testChatFlow_ConnectionTimeout() {
        // Given
        stubFor(post(urlEqualTo("/api/generate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(35000)
                        .withBody("{}")));

        ChatRequest request = ChatRequest.builder()
                .message("This will timeout")
                .build();

        // When
        ResponseEntity<ChatResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/chat",
                request,
                ChatResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getError()).isNotNull();
        assertThat(response.getBody().getDone()).isFalse();
    }

    @Test
    @DisplayName("Should handle 404 error when model not found")
    void testChatFlow_ModelNotFound() {
        // Given
        stubFor(post(urlEqualTo("/api/generate"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("Model not found")));

        ChatRequest request = ChatRequest.builder()
                .message("Hello")
                .model("nonexistent-model")
                .build();

        // When
        ResponseEntity<ChatResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/chat",
                request,
                ChatResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getError()).isNotNull();
        assertThat(response.getBody().getDone()).isFalse();
    }

    @Test
    @DisplayName("Should handle malformed JSON from Ollama")
    void testChatFlow_MalformedResponse() {
        // Given
        stubFor(post(urlEqualTo("/api/generate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ invalid json }")));

        ChatRequest request = ChatRequest.builder()
                .message("Hello")
                .build();

        // When
        ResponseEntity<ChatResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/chat",
                request,
                ChatResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getError()).isNotNull();
    }

    @Test
    @DisplayName("GET /api/status should return service status")
    void testStatusEndpoint() {
        // When
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/status",
                Map.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("service")).isEqualTo("llm-chat-service");
        assertThat(response.getBody().get("status")).isEqualTo("UP");
        assertThat(response.getBody().get("ollamaBaseUrl")).asString().startsWith("http://localhost:");
        assertThat(response.getBody().get("defaultModel")).isEqualTo("test-model");
        assertThat(response.getBody().get("defaultTemperature")).isEqualTo(0.5);
        assertThat(response.getBody().get("defaultMaxTokens")).isEqualTo(1000);
        assertThat(response.getBody().get("timeoutSeconds")).isEqualTo(30);
    }

    @Test
    @DisplayName("GET /api/models should return model information")
    void testModelsEndpoint() {
        // When
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/models",
                Map.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("currentModel")).isEqualTo("test-model");
        assertThat(response.getBody().get("baseUrl")).asString().startsWith("http://localhost:");
        assertThat(response.getBody().get("note")).isNotNull();
    }

    @Test
    @DisplayName("Should handle incomplete response with done=false")
    void testChatFlow_IncompleteResponse() {
        // Given
        String ollamaResponseJson = """
                {
                    "model": "llama2",
                    "response": "This is a partial response...",
                    "done": false,
                    "eval_count": 10
                }
                """;

        stubFor(post(urlEqualTo("/api/generate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ollamaResponseJson)));

        ChatRequest request = ChatRequest.builder()
                .message("Tell me a long story")
                .build();

        // When
        ResponseEntity<ChatResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/chat",
                request,
                ChatResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getDone()).isFalse();
        assertThat(response.getBody().getResponse()).contains("partial response");
        assertThat(response.getBody().getError()).isNull();
    }

    @Test
    @DisplayName("Should handle very long messages")
    void testChatFlow_LongMessage() {
        // Given
        String ollamaResponseJson = """
                {
                    "model": "llama2",
                    "response": "I understand your long message.",
                    "done": true,
                    "eval_count": 8
                }
                """;

        stubFor(post(urlEqualTo("/api/generate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ollamaResponseJson)));

        String longMessage = "A".repeat(5000);
        ChatRequest request = ChatRequest.builder()
                .message(longMessage)
                .build();

        // When
        ResponseEntity<ChatResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/chat",
                request,
                ChatResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getDone()).isTrue();

        // Verify long message was sent
        verify(postRequestedFor(urlEqualTo("/api/generate"))
                .withRequestBody(matchingJsonPath("$.prompt")));
    }

    @Test
    @DisplayName("Should handle all parameters together")
    void testChatFlow_AllParameters() {
        // Given
        String ollamaResponseJson = """
                {
                    "model": "mistral",
                    "response": "Comprehensive response with all parameters",
                    "done": true,
                    "eval_count": 18,
                    "total_duration": 2000000000,
                    "load_duration": 100000000,
                    "prompt_eval_count": 15,
                    "prompt_eval_duration": 300000000,
                    "eval_duration": 1200000000
                }
                """;

        stubFor(post(urlEqualTo("/api/generate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ollamaResponseJson)));

        ChatRequest request = ChatRequest.builder()
                .message("Complex query")
                .model("mistral")
                .temperature(0.8)
                .maxTokens(1500)
                .systemPrompt("You are an expert")
                .stream(false)
                .build();

        // When
        ResponseEntity<ChatResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/chat",
                request,
                ChatResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getModel()).isEqualTo("mistral");
        assertThat(response.getBody().getTokensGenerated()).isEqualTo(18);

        // Verify all parameters were sent
        verify(postRequestedFor(urlEqualTo("/api/generate"))
                .withRequestBody(matchingJsonPath("$.model", equalTo("mistral")))
                .withRequestBody(matchingJsonPath("$.prompt", equalTo("Complex query")))
                .withRequestBody(matchingJsonPath("$.system", equalTo("You are an expert")))
                .withRequestBody(matchingJsonPath("$.options.temperature", equalTo("0.8")))
                .withRequestBody(matchingJsonPath("$.options.num_predict", equalTo("1500")))
                .withRequestBody(matchingJsonPath("$.stream", equalTo("false"))));
    }
}
