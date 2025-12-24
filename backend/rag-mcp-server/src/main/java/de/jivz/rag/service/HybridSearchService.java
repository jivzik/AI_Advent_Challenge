package de.jivz.rag.service;

import de.jivz.rag.dto.MergedSearchResultDto;
import de.jivz.rag.dto.RerankingStrategyConfig;
import de.jivz.rag.dto.SearchResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис гибридного поиска.
 *
 * Единственная ответственность (SRP):
 * Координация гибридного поиска (семантический + ключевой).
 *
 * Реализует этапы 1-4 RAG pipeline:
 * 1. Семантический поиск (делегируется SemanticSearchService)
 * 2. Ключевой поиск (делегируется KeywordSearchService)
 * 3. Объединение результатов (делегируется SearchResultMergingService)
 * 4. Переранжирование (делегируется SearchResultRerankingService)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HybridSearchService {

    private static final double DEFAULT_SEMANTIC_WEIGHT = 0.6;
    private static final double DEFAULT_KEYWORD_WEIGHT = 0.4;
    private static final double DEFAULT_THRESHOLD = 0.5;

    private final SemanticSearchService semanticSearchService;
    private final KeywordSearchService keywordSearchService;
    private final SearchResultMergingService mergingService;
    private final SearchResultRerankingService rerankingService;

    /**
     * Гибридный поиск с полной конфигурацией.
     *
     * @param query поисковый запрос
     * @param config конфигурация поиска
     * @return объединённые и переранжированные результаты
     */
    public List<MergedSearchResultDto> search(String query, HybridSearchConfig config) {
        log.debug("Hybrid search: query='{}', config={}", query, config);

        // Этап 1: Семантический поиск
        List<SearchResultDto> semanticResults = executeSemanticSearch(query, config);

        // Этап 2: Ключевой поиск
        List<SearchResultDto> keywordResults = executeKeywordSearch(query, config);

        // Этап 3: Объединение
        List<MergedSearchResultDto> mergedResults = mergeResults(
                semanticResults, keywordResults, config);

        // Этап 4: Переранжирование
        List<MergedSearchResultDto> rerankedResults = rerankResults(mergedResults, config);

        log.debug("Hybrid search completed: {} results", rerankedResults.size());
        return rerankedResults;
    }

    /**
     * Гибридный поиск с параметрами.
     */
    public List<MergedSearchResultDto> search(String query, int topK, double threshold,
                                              double semanticWeight, double keywordWeight) {
        HybridSearchConfig config = HybridSearchConfig.builder()
                .topK(topK)
                .threshold(threshold)
                .semanticWeight(semanticWeight)
                .keywordWeight(keywordWeight)
                .build();
        return search(query, config);
    }

    /**
     * Гибридный поиск с весами по умолчанию (0.6/0.4).
     */
    public List<MergedSearchResultDto> search(String query, int topK, double threshold) {
        return search(query, topK, threshold, DEFAULT_SEMANTIC_WEIGHT, DEFAULT_KEYWORD_WEIGHT);
    }

    /**
     * Гибридный поиск с параметрами по умолчанию.
     */
    public List<MergedSearchResultDto> search(String query, int topK) {
        return search(query, topK, DEFAULT_THRESHOLD);
    }

    /**
     * Гибридный поиск в конкретном документе.
     */
    public List<MergedSearchResultDto> searchInDocument(String query, Long documentId,
                                                        int topK, double threshold,
                                                        double semanticWeight,
                                                        double keywordWeight) {
        log.debug("Hybrid search in document: query='{}', docId={}", query, documentId);

        // Этап 1: Семантический поиск в документе
        List<SearchResultDto> semanticResults =
                semanticSearchService.search(query, topK, threshold, documentId);

        // Этап 2: Ключевой поиск в документе
        List<SearchResultDto> keywordResults =
                keywordSearchService.keywordSearchInDocument(query, documentId, topK);

        // Этап 3-4: Объединение и переранжирование
        List<MergedSearchResultDto> merged = mergingService.mergeResults(
                semanticResults, keywordResults, semanticWeight, keywordWeight, topK);

        return rerankingService.rerankWeightedSum(merged, semanticWeight, keywordWeight);
    }

    /**
     * Гибридный поиск в документе с параметрами по умолчанию.
     */
    public List<MergedSearchResultDto> searchInDocument(String query, Long documentId, int topK) {
        return searchInDocument(query, documentId, topK, DEFAULT_THRESHOLD,
                DEFAULT_SEMANTIC_WEIGHT, DEFAULT_KEYWORD_WEIGHT);
    }

    // === Методы переранжирования ===

    /**
     * Переранжировать с конфигурацией стратегии.
     */
    public List<MergedSearchResultDto> rerank(List<MergedSearchResultDto> results,
                                              RerankingStrategyConfig config) {
        return rerankingService.rerank(results, config);
    }

    /**
     * Переранжировать с WEIGHTED_SUM.
     */
    public List<MergedSearchResultDto> rerankWeightedSum(List<MergedSearchResultDto> results,
                                                         double semanticWeight,
                                                         double keywordWeight) {
        return rerankingService.rerankWeightedSum(results, semanticWeight, keywordWeight);
    }

    /**
     * Переранжировать с MAX_SCORE.
     */
    public List<MergedSearchResultDto> rerankMaxScore(List<MergedSearchResultDto> results) {
        return rerankingService.rerankMaxScore(results);
    }

    /**
     * Переранжировать с RRF.
     */
    public List<MergedSearchResultDto> rerankRRF(List<MergedSearchResultDto> results, int k) {
        return rerankingService.rerankRRF(results, k);
    }

    /**
     * Переранжировать с RRF (k=60 по умолчанию).
     */
    public List<MergedSearchResultDto> rerankRRF(List<MergedSearchResultDto> results) {
        return rerankingService.rerankRRF(results);
    }

    // === Приватные методы ===

    private List<SearchResultDto> executeSemanticSearch(String query, HybridSearchConfig config) {
        if (config.getSemanticWeight() < 0.01) {
            return List.of();
        }
        return semanticSearchService.search(
                query,
                config.getTopK() * 2, // Берём больше для объединения
                config.getThreshold(),
                config.getDocumentId()
        );
    }

    private List<SearchResultDto> executeKeywordSearch(String query, HybridSearchConfig config) {
        if (config.getKeywordWeight() < 0.01) {
            return List.of();
        }
        if (config.getDocumentId() != null) {
            return keywordSearchService.keywordSearchInDocument(
                    query, config.getDocumentId(), config.getTopK() * 2);
        }
        return keywordSearchService.keywordSearch(query, config.getTopK() * 2);
    }

    private List<MergedSearchResultDto> mergeResults(List<SearchResultDto> semanticResults,
                                                     List<SearchResultDto> keywordResults,
                                                     HybridSearchConfig config) {
        return mergingService.mergeResults(
                semanticResults,
                keywordResults,
                config.getSemanticWeight(),
                config.getKeywordWeight(),
                config.getTopK()
        );
    }

    private List<MergedSearchResultDto> rerankResults(List<MergedSearchResultDto> results,
                                                      HybridSearchConfig config) {
        return rerankingService.rerankWeightedSum(
                results,
                config.getSemanticWeight(),
                config.getKeywordWeight()
        );
    }

    /**
     * Конфигурация гибридного поиска.
     */
    @lombok.Builder
    @lombok.Getter
    public static class HybridSearchConfig {
        @lombok.Builder.Default
        private int topK = 10;

        @lombok.Builder.Default
        private double threshold = DEFAULT_THRESHOLD;

        @lombok.Builder.Default
        private double semanticWeight = DEFAULT_SEMANTIC_WEIGHT;

        @lombok.Builder.Default
        private double keywordWeight = DEFAULT_KEYWORD_WEIGHT;

        private Long documentId;
    }
}