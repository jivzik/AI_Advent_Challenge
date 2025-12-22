package de.jivz.rag.dto;

import lombok.*;

/**
 * Конфигурация стратегии переранжирования.
 *
 * ЭТАП 4: Reranking (переранжирование)
 *
 * Поддерживаемые стратегии:
 * 1. WEIGHTED_SUM - взвешенная сумма (по умолчанию)
 * 2. MAX_SCORE - максимум из двух оценок
 * 3. RRF - Reciprocal Rank Fusion (на основе позиций)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RerankingStrategyConfig {

    /**
     * Стратегия переранжирования
     */
    public enum Strategy {
        /**
         * Взвешенная сумма (Weighted Sum):
         * combined_score = semantic_weight × semantic_score + keyword_weight × keyword_score
         *
         * Пример (weights: 0.6 / 0.4):
         * chunk1: 0.6 × 0.89 + 0.4 × 0.88 = 0.886
         * chunk2: 0.6 × 0.0 + 0.4 × 0.95 = 0.380
         */
        WEIGHTED_SUM("Weighted Sum: semantic_weight × semantic_score + keyword_weight × keyword_score"),

        /**
         * Максимум из двух оценок (Max Score):
         * combined_score = max(semantic_score, keyword_score)
         *
         * Пример:
         * chunk1: max(0.89, 0.88) = 0.89
         * chunk2: max(0.0, 0.95) = 0.95
         * Логика: берём лучший результат из двух методов
         */
        MAX_SCORE("Max Score: max(semantic_score, keyword_score)"),

        /**
         * Reciprocal Rank Fusion (RRF):
         * Не используются raw scores, а позиции в ранжированных списках
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
         *
         * RRF более robust к различным масштабам scores
         */
        RRF("Reciprocal Rank Fusion: Σ(1 / (k + rank))");

        private final String description;

        Strategy(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Выбранная стратегия (default: WEIGHTED_SUM)
     */
    @Builder.Default
    private Strategy strategy = Strategy.WEIGHTED_SUM;

    /**
     * Вес семантического поиска (0.0 - 1.0)
     * Default: 0.6 (для WEIGHTED_SUM)
     * Не используется для RRF
     */
    @Builder.Default
    private Double semanticWeight = 0.6;

    /**
     * Вес ключевого поиска (0.0 - 1.0)
     * Default: 0.4 (для WEIGHTED_SUM)
     * Не используется для RRF
     */
    @Builder.Default
    private Double keywordWeight = 0.4;

    /**
     * Константа k для RRF (default: 60)
     * Используется только для стратегии RRF
     */
    @Builder.Default
    private Integer rrfK = 60;

    /**
     * Валидирует конфигурацию
     * @throws IllegalArgumentException если конфигурация невалидна
     */
    public void validate() {
        if (semanticWeight < 0.0 || semanticWeight > 1.0) {
            throw new IllegalArgumentException("semanticWeight должен быть в диапазоне [0.0, 1.0]");
        }
        if (keywordWeight < 0.0 || keywordWeight > 1.0) {
            throw new IllegalArgumentException("keywordWeight должен быть в диапазоне [0.0, 1.0]");
        }
        if (rrfK <= 0) {
            throw new IllegalArgumentException("rrfK должен быть > 0");
        }
    }

    /**
     * Вычисляет нормализованные веса (в сумме = 1.0)
     * @return массив [normalizedSemanticWeight, normalizedKeywordWeight]
     */
    public double[] getNormalizedWeights() {
        double total = semanticWeight + keywordWeight;
        if (total <= 0) {
            return new double[]{0.5, 0.5};
        }
        return new double[]{
            semanticWeight / total,
            keywordWeight / total
        };
    }
}

