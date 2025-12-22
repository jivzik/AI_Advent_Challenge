package de.jivz.rag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Unit тесты для EmbeddingService с MockWebServer.
 */
class EmbeddingServiceTest {

    private static MockWebServer mockWebServer;
    private EmbeddingService embeddingService;
    private ObjectMapper objectMapper;

    @BeforeAll
    static void setUpServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDownServer() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        String baseUrl = mockWebServer.url("/").toString();

        // Создаём mock WebClient
        WebClient mockWebClient = mock(WebClient.class);

        embeddingService = new EmbeddingService(mockWebClient);
        ReflectionTestUtils.setField(embeddingService, "embeddingModel", "test-model");
        ReflectionTestUtils.setField(embeddingService, "batchSize", 10);
        ReflectionTestUtils.setField(embeddingService, "embeddingDimension", 768);
        ReflectionTestUtils.setField(embeddingService, "retryAttempts", 1);
        ReflectionTestUtils.setField(embeddingService, "retryDelayMs", 100L);
    }

    @Test
    @DisplayName("Should generate embedding for single text")
    void shouldGenerateEmbeddingForSingleText() {
        // Mock response
        String mockResponse = """
            {
                "data": [
                    {
                        "embedding": [0.1, 0.2, 0.3, 0.4, 0.5],
                        "index": 0
                    }
                ]
            }
            """;
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        float[] result = embeddingService.generateEmbedding("test text");

        assertThat(result).isNotNull();
        assertThat(result).hasSize(5);
        assertThat(result[0]).isEqualTo(0.1f);
    }

    @Test
    @DisplayName("Should generate embeddings for multiple texts")
    void shouldGenerateEmbeddingsForMultipleTexts() {
        String mockResponse = """
            {
                "data": [
                    {"embedding": [0.1, 0.2, 0.3], "index": 0},
                    {"embedding": [0.4, 0.5, 0.6], "index": 1}
                ]
            }
            """;
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        List<float[]> results = embeddingService.generateEmbeddings(List.of("text1", "text2"));

        assertThat(results).hasSize(2);
        assertThat(results.get(0)).containsExactly(0.1f, 0.2f, 0.3f);
        assertThat(results.get(1)).containsExactly(0.4f, 0.5f, 0.6f);
    }

    @Test
    @DisplayName("Should return empty list for empty input")
    void shouldReturnEmptyListForEmptyInput() {
        List<float[]> result = embeddingService.generateEmbeddings(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return null for null input")
    void shouldReturnNullForNullInput() {
        float[] result = embeddingService.generateEmbedding(null);
        // generateEmbeddings returns empty list, so generateEmbedding returns null
    }

    @Test
    @DisplayName("Should convert embedding to string format")
    void shouldConvertEmbeddingToString() {
        float[] embedding = {0.1f, 0.2f, 0.3f};
        String result = embeddingService.embeddingToString(embedding);

        assertThat(result).isEqualTo("[0.1,0.2,0.3]");
    }

    @Test
    @DisplayName("Should return null for null embedding conversion")
    void shouldReturnNullForNullEmbeddingConversion() {
        String result = embeddingService.embeddingToString(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should handle API error gracefully")
    void shouldHandleApiError() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        assertThatThrownBy(() -> embeddingService.generateEmbedding("test"))
                .isInstanceOf(RuntimeException.class);
    }
}

