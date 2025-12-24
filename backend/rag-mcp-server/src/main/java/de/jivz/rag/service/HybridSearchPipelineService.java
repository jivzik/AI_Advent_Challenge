package de.jivz.rag.service;

import de.jivz.rag.dto.FinalRankingConfig;
import de.jivz.rag.dto.FinalSearchResultDto;
import de.jivz.rag.dto.MergedSearchResultDto;
import de.jivz.rag.service.filtering.RelevanceFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис полного pipeline гибридного поиска.
 *
 * Единственная ответственность (SRP):
 * Координация полного RAG pipeline (этапы 1-5).
 *
 * Pipeline:
 * 1. Семантический поиск
 * 2. Ключевой поиск
 * 3. Объединение результатов
 * 4. Переранжирование
 * 5. Финализация (фильтрация, дедупликация, diversification)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HybridSearchPipelineService {

    private static final double DEFAULT_THRESHOLD = 0.3;
    private static final int DEFAULT_MAX_CHUNKS_PER_DOCUMENT = 2;
    private static final double DEFAULT_SEMANTIC_WEIGHT = 0.6;
    private static final double DEFAULT_KEYWORD_WEIGHT = 0.4;

    private final HybridSearchService hybridSearchService;
    private final FinalSearchResultService finalSearchService;

    /**
     * Полный pipeline с конфигурацией.
     *
     * @param query поисковый запрос
     * @param config конфигурация pipeline
     * @return финальные результаты
     */
    public List<FinalSearchResultDto> search(String query, PipelineConfig config) {
        log.debug("Pipeline search: query='{}', config={}", query, config);

        // Этапы 1-4: Гибридный поиск
        List<MergedSearchResultDto> merged = hybridSearchService.search(
                query,
                config.getTopK() * 2, // Берём больше для финализации
                0.0, // Низкий threshold, фильтрация в финализации
                config.getSemanticWeight(),
                config.getKeywordWeight()
        );

        // Этап 5: Финализация
        FinalRankingConfig finalConfig = FinalRankingConfig.builder()
                .minScoreThreshold(config.getThreshold())
                .topK(config.getTopK())
                .maxChunksPerDocument(config.getMaxChunksPerDocument())
                .build();

        List<FinalSearchResultDto> results = finalSearchService.finalizeResults(merged, finalConfig);

        log.debug("Pipeline search completed: {} results", results.size());
        return results;
    }

    /**
     * Полный pipeline с параметрами.
     */
    public List<FinalSearchResultDto> search(String query, int topK, double threshold,
                                             int maxChunksPerDocument) {
        PipelineConfig config = PipelineConfig.builder()
                .topK(topK)
                .threshold(threshold)
                .maxChunksPerDocument(maxChunksPerDocument)
                .build();
        return search(query, config);
    }

    /**
     * Полный pipeline с параметрами по умолчанию.
     */
    public List<FinalSearchResultDto> search(String query, int topK) {
        return search(query, topK, DEFAULT_THRESHOLD, DEFAULT_MAX_CHUNKS_PER_DOCUMENT);
    }

    // === Методы финализации ===

    /**
     * Финализация с конфигурацией.
     */
    public List<FinalSearchResultDto> finalize(List<MergedSearchResultDto> results,
                                               FinalRankingConfig config) {
        return finalSearchService.finalizeResults(results, config);
    }

    /**
     * Финализация с параметрами по умолчанию.
     */
    public List<FinalSearchResultDto> finalizeDefault(List<MergedSearchResultDto> results) {
        return finalSearchService.finalizeDefault(results);
    }

    /**
     * Финализация с порогом.
     */
    public List<FinalSearchResultDto> finalizeWithThreshold(List<MergedSearchResultDto> results,
                                                            double threshold, int topK) {
        return finalSearchService.finalizeWithThreshold(results, threshold, topK);
    }

    /**
     * Финализация с diversification.
     */
    public List<FinalSearchResultDto> finalizeWithDiversification(List<MergedSearchResultDto> results,
                                                                  int topK, int maxChunksPerDocument) {
        return finalSearchService.finalizeWithDiversification(results, topK, maxChunksPerDocument);
    }

    /**
     * Финализация с дедупликацией.
     */
    public List<FinalSearchResultDto> finalizeWithDeduplication(List<MergedSearchResultDto> results,
                                                                int topK) {
        return finalSearchService.finalizeWithDeduplication(results, topK);
    }

    /**
     * Конфигурация pipeline.
     */
    @lombok.Builder
    @lombok.Getter
    @lombok.ToString
    public static class PipelineConfig {
        @lombok.Builder.Default
        private int topK = 10;

        @lombok.Builder.Default
        private double threshold = DEFAULT_THRESHOLD;

        @lombok.Builder.Default
        private int maxChunksPerDocument = DEFAULT_MAX_CHUNKS_PER_DOCUMENT;

        @lombok.Builder.Default
        private double semanticWeight = DEFAULT_SEMANTIC_WEIGHT;

        @lombok.Builder.Default
        private double keywordWeight = DEFAULT_KEYWORD_WEIGHT;

        /**
         * Фильтр релевантности для применения перед финализацией (опционально).
         */
        private RelevanceFilter relevanceFilter;
    }
}