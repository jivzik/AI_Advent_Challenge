package de.jivz.rag.service;

import de.jivz.rag.dto.EmbeddingRequest;
import de.jivz.rag.dto.EmbeddingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для генерации эмбеддингов через OpenRouter API.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddingService {

    private final WebClient openRouterEmbeddingWebClient;

    @Value("${openrouter.api.embedding-model}")
    private String embeddingModel;

    @Value("${rag.embedding.batch-size:20}")
    private int batchSize;

    @Value("${rag.embedding.dimension:768}")
    private int embeddingDimension;

    @Value("${rag.embedding.retry-attempts:3}")
    private int retryAttempts;

    @Value("${rag.embedding.retry-delay-ms:1000}")
    private long retryDelayMs;

    /**
     * Включить instruction prefix для query эмбеддингов.
     * Улучшает качество поиска для instruction-based моделей (Qwen3, E5).
     */
    @Value("${rag.embedding.use-instruction-prefix:false}")
    private boolean useInstructionPrefix;

    /**
     * Instruction prefix для поисковых запросов.
     */
    @Value("${rag.embedding.query-instruction:Instruct: Given a question, retrieve relevant passages that answer the question.\\nQuery: }")
    private String queryInstruction;

    /**
     * Instruction prefix для документов (при индексации).
     */
    @Value("${rag.embedding.document-instruction:}")
    private String documentInstruction;

    /**
     * Генерирует эмбеддинг для поискового запроса.
     * Добавляет instruction prefix если включено.
     */
    public float[] generateEmbedding(String text) {
        String processedText = useInstructionPrefix ? queryInstruction + text : text;
        List<float[]> embeddings = generateEmbeddingsRaw(List.of(processedText));
        return embeddings.isEmpty() ? null : embeddings.getFirst();
    }

    /**
     * Генерирует эмбеддинги для документов (без instruction prefix для query).
     * Используется при индексации документов.
     */
    public List<float[]> generateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        // Для документов добавляем document instruction (обычно пустой)
        List<String> processedTexts = texts;
        if (useInstructionPrefix && !documentInstruction.isEmpty()) {
            processedTexts = texts.stream()
                    .map(t -> documentInstruction + t)
                    .toList();
        }

        return generateEmbeddingsRaw(processedTexts);
    }



    /**
     * Генерирует эмбеддинги для списка текстов (batch processing).
     */
    public List<float[]> generateEmbeddingsRaw(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        List<float[]> allEmbeddings = new ArrayList<>();

        // Разбиваем на батчи
        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);

            log.debug("📤 Processing batch {}/{} ({} texts)",
                    (i / batchSize) + 1,
                    (texts.size() + batchSize - 1) / batchSize,
                    batch.size());

            List<float[]> batchEmbeddings = callEmbeddingApi(batch);
            allEmbeddings.addAll(batchEmbeddings);
        }

        return allEmbeddings;
    }

    /**
     * Вызывает OpenRouter Embeddings API.
     */
    private List<float[]> callEmbeddingApi(List<String> texts) {
        EmbeddingRequest request = EmbeddingRequest.builder()
                .input(texts)
                .model(embeddingModel)
                .build();

        log.debug("📤 Calling embedding API with model: {}, texts count: {}",
                embeddingModel, texts.size());

        try {
            EmbeddingResponse response = openRouterEmbeddingWebClient.post()
                    .uri("/embeddings")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(EmbeddingResponse.class)
                    .retryWhen(Retry.backoff(retryAttempts, Duration.ofMillis(retryDelayMs))
                            .doBeforeRetry(signal ->
                                    log.warn("⚠️ Retrying embedding request, attempt: {}",
                                            signal.totalRetries() + 1)))
                    .block(Duration.ofSeconds(60));

            return parseEmbeddingsResponse(response);

        } catch (Exception e) {
            log.error("❌ Error calling embedding API: {}", e.getMessage());
            throw new RuntimeException("Failed to generate embeddings", e);
        }
    }

    /**
     * Парсит типизированный ответ и извлекает эмбеддинги.
     */
    private List<float[]> parseEmbeddingsResponse(EmbeddingResponse response) {
        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            log.error("❌ Invalid embedding response: null or empty data");
            throw new RuntimeException("Invalid embedding response format");
        }

        List<float[]> embeddings = new ArrayList<>();

        for (EmbeddingResponse.EmbeddingData data : response.getData()) {
            List<Double> embeddingList = data.getEmbedding();
            if (embeddingList != null && !embeddingList.isEmpty()) {
                float[] embedding = new float[embeddingList.size()];
                for (int i = 0; i < embeddingList.size(); i++) {
                    embedding[i] = embeddingList.get(i).floatValue();
                }
                embeddings.add(embedding);
            }
        }

        log.debug("✅ Parsed {} embeddings (dimension: {})",
                embeddings.size(),
                embeddings.isEmpty() ? 0 : embeddings.get(0).length);

        if (response.getUsage() != null) {
            log.debug("📊 Token usage: prompt={}, total={}, cost=${}",
                    response.getUsage().getPromptTokens(),
                    response.getUsage().getTotalTokens(),
                    response.getUsage().getCost());
        }

        if (response.getId() != null) {
            log.debug("📋 Embedding ID: {}", response.getId());
        }

        return embeddings;
    }

    /**
     * Конвертирует float[] в строку для pgvector.
     */
    public String embeddingToString(float[] embedding) {
        if (embedding == null) return null;

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
