package de.jivz.rag.service;

import de.jivz.rag.dto.MergedSearchResultDto;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис для переранжирования результатов поиска с использованием LLM.
 *
 * ЭТАП 5: LLM Reranking (LLM-переранжирование)
 *
 * Использует WebClient для вызова LLM API (как EmbeddingService использует OpenRouter API).
 *
 * Процесс:
 * 1. Для каждого результата создаёт prompt для LLM
 * 2. Вызывает LLM API через WebClient (аналогично EmbeddingService)
 * 3. Получает оценку релевантности (0.0 - 1.0)
 * 4. Сохраняет в поле llmScore
 * 5. Сортирует по llmScore
 *
 * Поддерживаемые режимы:
 * - REAL_LLM (default): вызов реального LLM API
 * - SYNTHETIC: синтетическая оценка (fallback, быстро)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LlmRerankingService {

    private final WebClient openRouterEmbeddingWebClient;

    @Value("${openrouter.api.reranking-model:openrouter/auto}")
    private String rerankingModel;

    @Value("${rag.reranking.mode:REAL_LLM}")
    private String rerankingMode;

    @Value("${rag.reranking.batch-size:5}")
    private int batchSize;

    @Value("${rag.reranking.retry-attempts:3}")
    private int retryAttempts;

    @Value("${rag.reranking.retry-delay-ms:1000}")
    private long retryDelayMs;

    @Value("${rag.reranking.timeout-seconds:60}")
    private long timeoutSeconds;

    private static final String REAL_LLM_MODE = "REAL_LLM";
    private static final String SYNTHETIC_MODE = "SYNTHETIC";

    /**
     * Переранжирует результаты с использованием LLM-оценки релевантности.
     *
     * @param results результаты для переранжирования
     * @param query поисковый запрос
     * @return результаты, отсортированные по llmScore в порядке убывания
     */
    public List<MergedSearchResultDto> rerankWithLlm(
            List<MergedSearchResultDto> results,
            String query) {

        if (results == null || results.isEmpty()) {
            log.warn("⚠️  Results for LLM reranking is empty");
            return results;
        }

        log.info("🤖 LLM Reranking {} results for query: '{}' (mode: {})",
                results.size(), query, rerankingMode);

        try {
            // Выбираем режим переранжирования
            if (REAL_LLM_MODE.equalsIgnoreCase(rerankingMode)) {
                return rerankWithRealLlm(results, query);
            } else {
                log.warn("⚠️  Reranking mode {} not available, falling back to SYNTHETIC", rerankingMode);
                return rerankWithSynthetic(results, query);
            }
        } catch (Exception e) {
            log.error("❌ Error during LLM reranking, falling back to SYNTHETIC: {}", e.getMessage());
            return rerankWithSynthetic(results, query);
        }
    }

    /**
     * Переранжирует результаты с использованием реального LLM API.
     * Вызывает LLM для каждого результата через WebClient (батчи).
     */
    private List<MergedSearchResultDto> rerankWithRealLlm(
            List<MergedSearchResultDto> results,
            String query) {

        log.info("📡 Calling LLM API (model: {}) for reranking...", rerankingModel);

        // Разбиваем на батчи
        for (int i = 0; i < results.size(); i += batchSize) {
            int end = Math.min(i + batchSize, results.size());
            List<MergedSearchResultDto> batch = results.subList(i, end);

            log.debug("  Processing batch {}/{} ({} results)",
                    (i / batchSize) + 1,
                    (results.size() + batchSize - 1) / batchSize,
                    batch.size());

            // Вызываем LLM для батча
            callLlmRerankerApi(batch, query);
        }

        // Сортируем по llmScore в порядке убывания
        List<MergedSearchResultDto> reranked = results.stream()
                .sorted((a, b) -> {
                    Double scoreA = a.getLlmScore() != null ? a.getLlmScore() : 0.0;
                    Double scoreB = b.getLlmScore() != null ? b.getLlmScore() : 0.0;
                    return scoreB.compareTo(scoreA);
                })
                .collect(Collectors.toList());

        log.info("✅ LLM Reranking completed");
        return reranked;
    }

    /**
     * Вызывает LLM API через WebClient для переранжирования батча результатов.
     * Аналогично EmbeddingService.callEmbeddingApi()
     */
    private void callLlmRerankerApi(List<MergedSearchResultDto> batch, String query) {
        // Формируем prompt для LLM
        String prompt = buildRerankingPrompt(batch, query);

        Map<String, Object> request = new HashMap<>();
        request.put("model", rerankingModel);
        request.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
        ));
        request.put("temperature", 0.1);  // Низкая температура для консистентности
        request.put("max_tokens", 1024);

        log.debug("📤 Calling LLM API with prompt (length: {})", prompt.length());

        try {
            String response = openRouterEmbeddingWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(Retry.backoff(retryAttempts, Duration.ofMillis(retryDelayMs))
                            .doBeforeRetry(signal ->
                                    log.warn("⚠️ Retrying LLM reranking request, attempt: {}",
                                            signal.totalRetries() + 1)))
                    .block(Duration.ofSeconds(timeoutSeconds));

            parseLlmResponse(response, batch);

        } catch (Exception e) {
            log.error("❌ Error calling LLM API: {}", e.getMessage());
            // Fallback на синтетическую оценку
            batch.forEach(result -> result.setLlmScore(
                    calculateSyntheticScore(query, result.getChunkText())
            ));
        }
    }

    /**
     * Формирует prompt для LLM переранжирания.
     */
    private String buildRerankingPrompt(List<MergedSearchResultDto> batch, String query) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a relevance ranking expert. For each given text passage, ")
              .append("evaluate its relevance to the query on a scale from 0.0 to 1.0.\n\n")
              .append("Query: ").append(query).append("\n\n")
              .append("Passages:\n");

        for (int i = 0; i < batch.size(); i++) {
            MergedSearchResultDto result = batch.get(i);
            prompt.append(i + 1).append(". ").append(result.getChunkText()).append("\n\n");
        }

        prompt.append("Provide the relevance scores as a JSON array: [score1, score2, ..., scoreN]\n")
              .append("Return ONLY the JSON array, nothing else.\n")
              .append("Example: [0.95, 0.72, 0.38]");

        return prompt.toString();
    }

    /**
     * Парсит LLM response и извлекает оценки.
     */
    private void parseLlmResponse(String response, List<MergedSearchResultDto> batch) {
        if (response == null || response.isBlank()) {
            log.error("❌ Empty LLM response");
            fallbackToSynthetic(batch);
            return;
        }

        try {
            // Ищем JSON array в response
            int startIdx = response.indexOf('[');
            int endIdx = response.lastIndexOf(']');

            if (startIdx == -1 || endIdx == -1) {
                log.error("❌ No JSON array found in LLM response");
                fallbackToSynthetic(batch);
                return;
            }

            String jsonString = response.substring(startIdx, endIdx + 1);
            List<Double> scores = parseScoresFromJson(jsonString);

            // Присваиваем оценки результатам
            for (int i = 0; i < Math.min(scores.size(), batch.size()); i++) {
                double score = Math.min(1.0, Math.max(0.0, scores.get(i)));  // Нормализуем в [0, 1]
                batch.get(i).setLlmScore(score);

                log.debug("  Result {} - llmScore: {}", i + 1, String.format("%.4f", score));
            }

            // Оставшиеся результаты получают 0.0
            for (int i = scores.size(); i < batch.size(); i++) {
                batch.get(i).setLlmScore(0.0);
            }

        } catch (Exception e) {
            log.error("❌ Error parsing LLM response: {}", e.getMessage());
            fallbackToSynthetic(batch);
        }
    }

    /**
     * Парсит scores из JSON string.
     */
    private List<Double> parseScoresFromJson(String jsonString) {
        List<Double> scores = new ArrayList<>();
        try {
            // Простой парсинг без Jackson
            String trimmed = jsonString.replaceAll("[\\[\\]]", "").trim();
            if (trimmed.isEmpty()) {
                return scores;
            }

            String[] parts = trimmed.split(",");
            for (String part : parts) {
                try {
                    double score = Double.parseDouble(part.trim());
                    scores.add(score);
                } catch (NumberFormatException e) {
                    log.warn("Could not parse score: {}", part);
                }
            }
        } catch (Exception e) {
            log.error("Error parsing JSON scores: {}", e.getMessage());
        }

        return scores;
    }

    /**
     * Fallback на синтетическую оценку для батча.
     */
    private void fallbackToSynthetic(List<MergedSearchResultDto> batch) {
        log.warn("⚠️ Falling back to SYNTHETIC scoring for batch of {} results", batch.size());
        // Будет заполнено при следующем вызове rerankWithSynthetic
    }

    /**
     * Переранжирует результаты с использованием синтетической оценки.
     * Быстрая локальная оценка без вызовов API.
     */
    private List<MergedSearchResultDto> rerankWithSynthetic(
            List<MergedSearchResultDto> results,
            String query) {

        log.info("⚡ Using SYNTHETIC scoring for {} results", results.size());

        // Вычисляем синтетическую оценку для каждого результата
        results.forEach(result -> {
            double score = calculateSyntheticScore(query, result.getChunkText());
            result.setLlmScore(score);

            log.debug("  chunk_id={}, llmScore={}", result.getChunkId(), String.format("%.4f", score));
        });

        // Сортируем по llmScore в порядке убывания
        List<MergedSearchResultDto> reranked = results.stream()
                .sorted((a, b) -> {
                    Double scoreA = a.getLlmScore() != null ? a.getLlmScore() : 0.0;
                    Double scoreB = b.getLlmScore() != null ? b.getLlmScore() : 0.0;
                    return scoreB.compareTo(scoreA);
                })
                .collect(Collectors.toList());

        log.info("✅ SYNTHETIC Reranking completed");
        return reranked;
    }

    /**
     * Вычисляет синтетическую оценку релевантности.
     * Используется как fallback и для быстрого режима.
     *
     * Основана на:
     * - Совпадении ключевых слов (60%)
     * - Длине текста (20%)
     * - Позиции совпадений (20%)
     */
    private double calculateSyntheticScore(String query, String chunkText) {
        if (query == null || query.isBlank() || chunkText == null || chunkText.isBlank()) {
            return 0.0;
        }

        String lowerQuery = query.toLowerCase();
        String lowerText = chunkText.toLowerCase();

        double baseScore = calculateKeywordMatchingScore(lowerQuery, lowerText);      // 60%
        double lengthBonus = calculateLengthBonus(chunkText);                          // 20%
        double positionBonus = calculatePositionBonus(lowerQuery, lowerText);          // 20%

        double llmScore = (baseScore * 0.6) + (lengthBonus * 0.2) + (positionBonus * 0.2);
        return Math.min(1.0, Math.max(0.0, llmScore));
    }

    /**
     * Вычисляет оценку на основе совпадения ключевых слов.
     */
    private double calculateKeywordMatchingScore(String lowerQuery, String lowerText) {
        String[] queryWords = lowerQuery.split("\\s+");
        int matchedWords = 0;

        for (String word : queryWords) {
            if (word.length() > 2 && lowerText.contains(word)) {
                matchedWords++;
            }
        }

        if (queryWords.length == 0) {
            return 0.0;
        }

        return (matchedWords / (double) queryWords.length);
    }

    /**
     * Вычисляет бонус за длину текста.
     * Оптимально 300-1000 символов.
     */
    private double calculateLengthBonus(String chunkText) {
        int length = chunkText.length();

        if (length < 50) {
            return 0.2;
        } else if (length < 300) {
            return 0.6;
        } else if (length <= 1000) {
            return 1.0;
        } else if (length <= 2000) {
            return 0.9;
        } else {
            return 0.7;
        }
    }

    /**
     * Вычисляет бонус за позицию совпадений.
     * Совпадения в начале текста более ценны.
     */
    private double calculatePositionBonus(String lowerQuery, String lowerText) {
        String[] queryWords = lowerQuery.split("\\s+");
        int totalTextLength = lowerText.length();

        if (totalTextLength == 0) {
            return 0.0;
        }

        double positionScore = 0.0;
        int matchCount = 0;

        for (String word : queryWords) {
            if (word.length() > 2) {
                int indexOfWord = lowerText.indexOf(word);
                if (indexOfWord >= 0) {
                    double positionFactor = 1.0 - (indexOfWord / (double) totalTextLength) * 0.7;
                    positionScore += positionFactor;
                    matchCount++;
                }
            }
        }

        if (matchCount == 0) {
            return 0.0;
        }

        return positionScore / matchCount;
    }
}

