package de.jivz.rag.service;

import de.jivz.rag.dto.MergedSearchResultDto;
import de.jivz.rag.dto.RerankingStrategyConfig;
import de.jivz.rag.dto.RerankingStrategyConfig.Strategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис для переранжирования (Reranking) результатов поиска.
 *
 * ЭТАП 4: Reranking (переранжирование)
 *
 * Цель:
 * Вычислить финальный combined score для каждого чанка, используя одну из стратегий комбинирования.
 *
 * Поддерживаемые стратегии:
 * 1. WEIGHTED_SUM (по умолчанию) - быстрая, простая в настройке
 * 2. MAX_SCORE - берет лучший результат из двух методов
 * 3. RRF (Reciprocal Rank Fusion) - более robust, менее чувствительна к масштабам scores
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SearchResultRerankingService {

    /**
     * Переранжирует результаты с использованием выбранной стратегии.
     *
     * @param results результаты для переранжирования
     * @param config конфигурация стратегии
     * @return переранжированные результаты
     */
    public List<MergedSearchResultDto> rerank(
            List<MergedSearchResultDto> results,
            RerankingStrategyConfig config) {

        if (results == null || results.isEmpty()) {
            log.warn("⚠️  Results for reranking is empty");
            return new ArrayList<>();
        }

        config.validate();

        log.info("🔄 Reranking {} results using strategy: {}",
                results.size(), config.getStrategy());

        List<MergedSearchResultDto> rerankedResults;

        switch (config.getStrategy()) {
            case WEIGHTED_SUM:
                rerankedResults = rerankWeightedSum(results, config);
                break;
            case MAX_SCORE:
                rerankedResults = rerankMaxScore(results);
                break;
            case RRF:
                rerankedResults = rerankRRF(results, config);
                break;
            default:
                throw new IllegalArgumentException("Unknown strategy: " + config.getStrategy());
        }

        log.info("✅ Reranking completed");
        return rerankedResults;
    }

    /**
     * Переранжирует результаты с использованием стратегии WEIGHTED_SUM.
     *
     * Формула:
     * combined_score = semantic_weight × semantic_score + keyword_weight × keyword_score
     *
     * Пример (weights: 0.6 / 0.4):
     * chunk1: 0.6 × 0.89 + 0.4 × 0.88 = 0.886
     * chunk2: 0.6 × 0.0 + 0.4 × 0.95 = 0.380
     * chunk3: 0.6 × 0.82 + 0.4 × 0.0 = 0.492
     *
     * @param results результаты для переранжирования
     * @param config конфигурация стратегии
     * @return переранжированные результаты
     */
    private List<MergedSearchResultDto> rerankWeightedSum(
            List<MergedSearchResultDto> results,
            RerankingStrategyConfig config) {

        log.debug("📊 Using WEIGHTED_SUM strategy");
        log.debug("  semantic_weight={}, keyword_weight={}",
                config.getSemanticWeight(), config.getKeywordWeight());

        double[] weights = config.getNormalizedWeights();
        double semanticWeight = weights[0];
        double keywordWeight = weights[1];

        log.debug("  normalized: semantic={}, keyword={}",
                semanticWeight, keywordWeight);

        // Вычисляем комбинированный score для каждого результата
        results.forEach(result -> {
            double semanticScore = result.getSemanticScore() != null ? result.getSemanticScore() : 0.0;
            double keywordScore = result.getKeywordScore() != null ? result.getKeywordScore() : 0.0;

            double combinedScore = (semanticWeight * semanticScore) + (keywordWeight * keywordScore);
            result.setMergedScore(combinedScore);

            log.debug("    chunk_id={}, semantic={}, keyword={}, combined={}",
                    result.getChunkId(),
                    String.format("%.4f", semanticScore),
                    String.format("%.4f", keywordScore),
                    String.format("%.4f", combinedScore));
        });

        // Сортируем по комбинированному score в порядке убывания
        return results.stream()
                .sorted((a, b) -> {
                    Double scoreA = a.getMergedScore() != null ? a.getMergedScore() : 0.0;
                    Double scoreB = b.getMergedScore() != null ? b.getMergedScore() : 0.0;
                    return scoreB.compareTo(scoreA);
                })
                .collect(Collectors.toList());
    }

    /**
     * Переранжирует результаты с использованием стратегии MAX_SCORE.
     *
     * Логика:
     * combined_score = max(semantic_score, keyword_score)
     *
     * Пример:
     * chunk1: max(0.89, 0.88) = 0.89
     * chunk2: max(0.0, 0.95) = 0.95
     * chunk3: max(0.82, 0.0) = 0.82
     *
     * Используется, когда нужно отдать приоритет лучшему результату из двух методов,
     * независимо от того, какой метод его дал.
     *
     * @param results результаты для переранжирования
     * @return переранжированные результаты
     */
    public List<MergedSearchResultDto> rerankMaxScore(List<MergedSearchResultDto> results) {

        log.debug("📊 Using MAX_SCORE strategy");

        results.forEach(result -> {
            double semanticScore = result.getSemanticScore() != null ? result.getSemanticScore() : 0.0;
            double keywordScore = result.getKeywordScore() != null ? result.getKeywordScore() : 0.0;

            double combinedScore = Math.max(semanticScore, keywordScore);
            result.setMergedScore(combinedScore);

            log.debug("    chunk_id={}, semantic={}, keyword={}, max={}",
                    result.getChunkId(),
                    String.format("%.4f", semanticScore),
                    String.format("%.4f", keywordScore),
                    String.format("%.4f", combinedScore));
        });

        // Сортируем по комбинированному score в порядке убывания
        return results.stream()
                .sorted((a, b) -> {
                    Double scoreA = a.getMergedScore() != null ? a.getMergedScore() : 0.0;
                    Double scoreB = b.getMergedScore() != null ? b.getMergedScore() : 0.0;
                    return scoreB.compareTo(scoreA);
                })
                .collect(Collectors.toList());
    }

    /**
     * Переранжирует результаты с использованием стратегии RRF (Reciprocal Rank Fusion).
     *
     * Концепция:
     * Не использует raw scores, а позиции в ранжированных списках.
     *
     * Формула для каждого чанка:
     * RRF_score = Σ(1 / (k + rank_i))
     *
     * где:
     * - k = 60 (константа, обычно 60)
     * - rank_i = позиция в списке i (semantic или keyword)
     *
     * Пример:
     * Semantic ranking: [chunk1(1), chunk3(2), chunk5(3)]
     * Keyword ranking:  [chunk2(1), chunk1(2), chunk4(3)]
     *
     * chunk1: 1/(60+1) + 1/(60+2) = 0.0164 + 0.0161 = 0.0325
     * chunk2: 0 + 1/(60+1) = 0.0164
     * chunk3: 1/(60+2) + 0 = 0.0161
     * chunk4: 0 + 1/(60+3) = 0.0154
     * chunk5: 1/(60+3) + 0 = 0.0154
     *
     * RRF более robust к различным масштабам scores и менее чувствительна к
     * экстремальным значениям.
     *
     * Преимущества RRF:
     * - Не зависит от масштаба scores
     * - Менее чувствительна к outliers
     * - Хорошо работает для комбинирования разнородных результатов
     *
     * @param results результаты для переранжирования
     * @param config конфигурация стратегии (используется config.getRrfK())
     * @return переранжированные результаты
     */
    private List<MergedSearchResultDto> rerankRRF(
            List<MergedSearchResultDto> results,
            RerankingStrategyConfig config) {

        int k = config.getRrfK() != null ? config.getRrfK() : 60;
        log.debug("📊 Using RRF (Reciprocal Rank Fusion) strategy");
        log.debug("  k={}", k);

        // Получаем исходные результаты (отсортированные списки)
        // Semantic ranking (результаты отсортированы по semanticScore)
        List<MergedSearchResultDto> semanticRanking = results.stream()
                .filter(r -> r.getSemanticScore() != null && r.getSemanticScore() > 0)
                .sorted((a, b) -> b.getSemanticScore().compareTo(a.getSemanticScore()))
                .toList();

        // Keyword ranking (результаты отсортированы по keywordScore)
        List<MergedSearchResultDto> keywordRanking = results.stream()
                .filter(r -> r.getKeywordScore() != null && r.getKeywordScore() > 0)
                .sorted((a, b) -> b.getKeywordScore().compareTo(a.getKeywordScore()))
                .toList();

        log.debug("  semantic_ranking_size={}, keyword_ranking_size={}",
                semanticRanking.size(), keywordRanking.size());

        // Создаём Map для хранения RRF scores
        Map<Long, Double> rrfScores = new HashMap<>();

        // Вычисляем RRF score для semantic ranking
        for (int i = 0; i < semanticRanking.size(); i++) {
            MergedSearchResultDto result = semanticRanking.get(i);
            int rank = i + 1; // Ранги начинаются с 1
            double rrfScore = 1.0 / (k + rank);

            rrfScores.put(result.getChunkId(),
                    rrfScores.getOrDefault(result.getChunkId(), 0.0) + rrfScore);

            log.debug("    semantic: chunk_id={}, rank={}, rrf_score={}",
                    result.getChunkId(), rank, String.format("%.6f", rrfScore));
        }

        // Вычисляем RRF score для keyword ranking
        for (int i = 0; i < keywordRanking.size(); i++) {
            MergedSearchResultDto result = keywordRanking.get(i);
            int rank = i + 1; // Ранги начинаются с 1
            double rrfScore = 1.0 / (k + rank);

            rrfScores.put(result.getChunkId(),
                    rrfScores.getOrDefault(result.getChunkId(), 0.0) + rrfScore);

            log.debug("    keyword: chunk_id={}, rank={}, rrf_score={}",
                    result.getChunkId(), rank, String.format("%.6f", rrfScore));
        }

        // Обновляем mergedScore в результатах
        for (MergedSearchResultDto result : results) {
            Double finalScore = rrfScores.getOrDefault(result.getChunkId(), 0.0);
            result.setMergedScore(finalScore);

            log.debug("    final: chunk_id={}, rrf_score={}",
                    result.getChunkId(), String.format("%.6f", finalScore));
        }

        // Сортируем по RRF score в порядке убывания
        return results.stream()
                .sorted((a, b) -> {
                    Double scoreA = a.getMergedScore() != null ? a.getMergedScore() : 0.0;
                    Double scoreB = b.getMergedScore() != null ? b.getMergedScore() : 0.0;
                    return scoreB.compareTo(scoreA);
                })
                .collect(Collectors.toList());
    }

    /**
     * Переранжирует результаты с использованием стратегии WEIGHTED_SUM по умолчанию.
     * Параметры: semantic_weight=0.6, keyword_weight=0.4
     *
     * @param results результаты для переранжирования
     * @return переранжированные результаты
     */
    public List<MergedSearchResultDto> rerankDefault(List<MergedSearchResultDto> results) {
        return rerank(results, RerankingStrategyConfig.builder().build());
    }

    /**
     * Переранжирует результаты с использованием стратегии WEIGHTED_SUM с пользовательскими весами.
     *
     * @param results результаты для переранжирования
     * @param semanticWeight вес семантического поиска (0.0 - 1.0)
     * @param keywordWeight вес ключевого поиска (0.0 - 1.0)
     * @return переранжированные результаты
     */
    public List<MergedSearchResultDto> rerankWeightedSum(
            List<MergedSearchResultDto> results,
            double semanticWeight,
            double keywordWeight) {
        return rerank(results, RerankingStrategyConfig.builder()
                .strategy(Strategy.WEIGHTED_SUM)
                .semanticWeight(semanticWeight)
                .keywordWeight(keywordWeight)
                .build());
    }



    /**
     * Переранжирует результаты с использованием стратегии RRF.
     *
     * @param results результаты для переранжирования
     * @param k константа k для RRF (default: 60)
     * @return переранжированные результаты
     */
    public List<MergedSearchResultDto> rerankRRF(List<MergedSearchResultDto> results, int k) {
        return rerank(results, RerankingStrategyConfig.builder()
                .strategy(Strategy.RRF)
                .rrfK(k)
                .build());
    }

    /**
     * Переранжирует результаты с использованием стратегии RRF с k=60 по умолчанию.
     *
     * @param results результаты для переранжирования
     * @return переранжированные результаты
     */
    public List<MergedSearchResultDto> rerankRRF(List<MergedSearchResultDto> results) {
        return rerankRRF(results, 60);
    }
}

