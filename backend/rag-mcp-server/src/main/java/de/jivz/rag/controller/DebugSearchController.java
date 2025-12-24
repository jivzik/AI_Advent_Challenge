package de.jivz.rag.controller;

import de.jivz.rag.dto.SearchResultDto;
import de.jivz.rag.service.EmbeddingService;
import de.jivz.rag.service.RagFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Диагностический контроллер для отладки semantic search.
 * УДАЛИТЬ В PRODUCTION!
 */
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DebugSearchController {

    private final EmbeddingService embeddingService;
    private final RagFacade ragFacade;

    /**
     * Проверка генерации эмбеддинга для запроса.
     *
     * GET /api/debug/embedding?text=кто такой Раббат
     */
    @GetMapping("/embedding")
    public ResponseEntity<?> testEmbedding(@RequestParam String text) {
        log.info("Testing embedding for: '{}'", text);

        try {
            long start = System.currentTimeMillis();
            float[] embedding = embeddingService.generateEmbedding(text);
            long time = System.currentTimeMillis() - start;

            if (embedding == null) {
                return ResponseEntity.ok(Map.of(
                        "status", "ERROR",
                        "message", "Embedding is NULL - API не вернул эмбеддинг!",
                        "text", text
                ));
            }

            // Статистика по эмбеддингу
            double sum = 0, min = Float.MAX_VALUE, max = Float.MIN_VALUE;
            for (float v : embedding) {
                sum += v;
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
            double mean = sum / embedding.length;

            // Первые и последние 5 значений
            float[] first5 = Arrays.copyOfRange(embedding, 0, Math.min(5, embedding.length));
            float[] last5 = Arrays.copyOfRange(embedding,
                    Math.max(0, embedding.length - 5), embedding.length);

            return ResponseEntity.ok(Map.of(
                    "status", "OK",
                    "text", text,
                    "dimension", embedding.length,
                    "timeMs", time,
                    "stats", Map.of(
                            "min", min,
                            "max", max,
                            "mean", mean
                    ),
                    "sample", Map.of(
                            "first5", first5,
                            "last5", last5
                    )
            ));

        } catch (Exception e) {
            log.error("Embedding error", e);
            return ResponseEntity.ok(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage(),
                    "text", text
            ));
        }
    }

    /**
     * Сравнение двух текстов по косинусному сходству.
     *
     * GET /api/debug/similarity?text1=кто такой Раббат&text2=Раббат это магический лис
     */
    @GetMapping("/similarity")
    public ResponseEntity<?> testSimilarity(
            @RequestParam String text1,
            @RequestParam String text2) {

        log.info("Testing similarity: '{}' vs '{}'", text1, text2);

        try {
            float[] emb1 = embeddingService.generateEmbedding(text1);
            float[] emb2 = embeddingService.generateEmbedding(text2);

            if (emb1 == null || emb2 == null) {
                return ResponseEntity.ok(Map.of(
                        "status", "ERROR",
                        "message", "One or both embeddings are NULL"
                ));
            }

            double similarity = cosineSimilarity(emb1, emb2);

            return ResponseEntity.ok(Map.of(
                    "status", "OK",
                    "text1", text1,
                    "text2", text2,
                    "similarity", similarity,
                    "interpretation", interpretSimilarity(similarity)
            ));

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Полная диагностика semantic search.
     *
     * GET /api/debug/semantic-search?query=кто такой Раббат&topK=5&threshold=0.0
     */
    @GetMapping("/semantic-search")
    public ResponseEntity<?> debugSemanticSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK,
            @RequestParam(defaultValue = "0.0") double threshold) {

        log.info("Debug semantic search: query='{}', topK={}, threshold={}", query, topK, threshold);

        try {
            // 1. Генерируем эмбеддинг запроса
            long embStart = System.currentTimeMillis();
            float[] queryEmbedding = embeddingService.generateEmbedding(query);
            long embTime = System.currentTimeMillis() - embStart;

            if (queryEmbedding == null) {
                return ResponseEntity.ok(Map.of(
                        "status", "ERROR",
                        "step", "embedding",
                        "message", "Query embedding is NULL!"
                ));
            }

            // 2. Выполняем поиск с threshold=0 чтобы увидеть все результаты
            long searchStart = System.currentTimeMillis();
            List<SearchResultDto> results = ragFacade.search(query, topK, threshold, null);
            long searchTime = System.currentTimeMillis() - searchStart;

            // 3. Форматируем результаты с подробностями
            var detailedResults = results.stream()
                    .map(r -> Map.of(
                            "chunkId", r.getChunkId(),
                            "documentName", r.getDocumentName() != null ? r.getDocumentName() : "",
                            "similarity", r.getSimilarity() != null ? r.getSimilarity() : 0.0,
                            "chunkIndex", r.getChunkIndex() != null ? r.getChunkIndex() : 0,
                            "textPreview", truncate(r.getChunkText(), 200)
                    ))
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "status", "OK",
                    "query", query,
                    "queryEmbeddingDimension", queryEmbedding.length,
                    "embeddingTimeMs", embTime,
                    "searchTimeMs", searchTime,
                    "resultsCount", results.size(),
                    "threshold", threshold,
                    "results", detailedResults,
                    "diagnosis", diagnose(results, threshold)
            ));

        } catch (Exception e) {
            log.error("Debug search error", e);
            return ResponseEntity.ok(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()
            ));
        }
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private String interpretSimilarity(double sim) {
        if (sim >= 0.8) return "VERY_HIGH - тексты очень похожи по смыслу";
        if (sim >= 0.6) return "HIGH - тексты связаны по смыслу";
        if (sim >= 0.4) return "MEDIUM - есть некоторая связь";
        if (sim >= 0.2) return "LOW - слабая связь";
        return "VERY_LOW - тексты не связаны";
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    private Map<String, Object> diagnose(List<SearchResultDto> results, double threshold) {
        if (results.isEmpty()) {
            return Map.of(
                    "problem", "NO_RESULTS",
                    "suggestion", "Попробуй threshold=0.0, проверь что эмбеддинги сохранены в БД"
            );
        }

        double maxSim = results.stream()
                .mapToDouble(r -> r.getSimilarity() != null ? r.getSimilarity() : 0)
                .max().orElse(0);

        if (maxSim < 0.3) {
            return Map.of(
                    "problem", "LOW_SIMILARITY",
                    "maxSimilarity", maxSim,
                    "suggestion", "Модель эмбеддингов плохо понимает русский язык или контекст. " +
                            "Попробуй модель multilingual-e5-large или text-embedding-3-small"
            );
        }

        if (maxSim < 0.5) {
            return Map.of(
                    "problem", "MEDIUM_SIMILARITY",
                    "maxSimilarity", maxSim,
                    "suggestion", "Результаты есть, но сходство среднее. Возможно нужна лучшая модель"
            );
        }

        return Map.of(
                "problem", "NONE",
                "maxSimilarity", maxSim,
                "suggestion", "Semantic search работает нормально"
        );
    }
}