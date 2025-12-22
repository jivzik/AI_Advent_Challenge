package de.jivz.rag.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Результат объединённого поиска - содержит scores от обоих методов поиска.
 *
 * Структура:
 * - Основные данные чанка (из semantic или keyword результата, в зависимости от того, что есть)
 * - semanticScore: оценка от semantic search (или null)
 * - keywordScore: оценка от keyword search (или null)
 * - mergedScore: комбинированная оценка (ЭТАП 3: Merging) или переранжированная оценка (ЭТАП 4: Reranking)
 *
 * ЭТАП 3: Merging
 * - mergedScore вычисляется как взвешенная сумма: semanticWeight × semanticScore + keywordWeight × keywordScore
 *
 * ЭТАП 4: Reranking
 * - mergedScore переоценивается с использованием одной из стратегий:
 *   1. WEIGHTED_SUM (по умолчанию) - взвешенная сумма
 *   2. MAX_SCORE - максимум из двух оценок
 *   3. RRF - Reciprocal Rank Fusion (на основе позиций в списках)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MergedSearchResultDto {

    // Данные чанка
    private Long chunkId;
    private Long documentId;
    private String documentName;
    private Integer chunkIndex;
    private String chunkText;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;

    // Scores от разных методов поиска
    private Double semanticScore;
    private Double keywordScore;

    // Комбинированная оценка (для финального ранжирования)
    private Double mergedScore;

    /**
     * Вычисляет комбинированную оценку на основе семантического и ключевого поиска.
     *
     * Использует взвешенное среднее:
     * mergedScore = semanticWeight * semanticScore + keywordWeight * keywordScore
     *
     * @param semanticWeight вес семантического поиска (default: 0.6)
     * @param keywordWeight вес ключевого поиска (default: 0.4)
     * @return комбинированная оценка
     */
    public Double calculateMergedScore(double semanticWeight, double keywordWeight) {
        Double score = 0.0;

        if (semanticScore != null) {
            score += semanticWeight * semanticScore;
        }

        if (keywordScore != null) {
            score += keywordWeight * keywordScore;
        }

        this.mergedScore = score;
        return score;
    }

    /**
     * Вычисляет комбинированную оценку с параметрами по умолчанию (0.6 / 0.4).
     */
    public Double calculateMergedScore() {
        return calculateMergedScore(0.6, 0.4);
    }

    /**
     * Проверяет, есть ли хотя бы один скор (семантический или ключевой).
     */
    public boolean hasAnyScore() {
        return semanticScore != null || keywordScore != null;
    }

    /**
     * Проверяет, есть ли оба скора.
     */
    public boolean hasBothScores() {
        return semanticScore != null && keywordScore != null;
    }
}

