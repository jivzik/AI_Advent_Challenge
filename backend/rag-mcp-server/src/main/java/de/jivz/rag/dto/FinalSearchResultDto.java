package de.jivz.rag.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Финальный результат поиска с полными метаданными.
 *
 * ЭТАП 5: Финальная сортировка и фильтрация
 *
 * Содержит:
 * - Основные данные чанка
 * - Все scores (semantic, keyword, combined)
 * - Позицию в финальном списке
 * - Метаинформацию
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalSearchResultDto {

    // ============ Основные данные ============

    private Long chunkId;
    private Long documentId;
    private String documentName;
    private Integer chunkIndex;
    private String chunkText;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;

    // ============ Scores ============

    /**
     * Оценка от семантического поиска (null если не было семантического поиска)
     */
    private Double semanticScore;

    /**
     * Оценка от ключевого поиска (null если не было ключевого поиска)
     */
    private Double keywordScore;

    /**
     * Финальная комбинированная оценка (0.0 - 1.0)
     * Вычисляется на ЭТАП 3-4 (merging + reranking)
     */
    private Double combinedScore;

    // ============ Метаданные финального этапа ============

    /**
     * Позиция в финальном отсортированном списке (начинается с 1)
     * Используется для отображения ранга результата
     */
    private Integer relevanceRank;

    /**
     * Процентиль относительно всех результатов (0-100)
     * Например, 95 = в топ 5% результатов
     */
    private Double relevancePercentile;

    /**
     * Источник результата
     * Может быть: "SEMANTIC", "KEYWORD", "BOTH"
     */
    private String source;

    /**
     * Был ли результат отфильтрован или исключён на этапе финальной фильтрации?
     * (для отладки и статистики)
     */
    private Boolean filtered;
    private String filterReason;

    // ============ Вспомогательные методы ============

    /**
     * Возвращает наилучший результат из semantic и keyword scores
     */
    public Double getBestScore() {
        Double semantic = semanticScore != null ? semanticScore : 0.0;
        Double keyword = keywordScore != null ? keywordScore : 0.0;
        return Math.max(semantic, keyword);
    }

    /**
     * Возвращает наихудший результат из semantic и keyword scores
     */
    public Double getWorstScore() {
        Double semantic = semanticScore != null ? semanticScore : 0.0;
        Double keyword = keywordScore != null ? keywordScore : 0.0;
        return Math.min(semantic, keyword);
    }

    /**
     * Определяет источник результата на основе наличия scores
     */
    public void determineSource() {
        boolean hasSemantic = semanticScore != null && semanticScore > 0;
        boolean hasKeyword = keywordScore != null && keywordScore > 0;

        if (hasSemantic && hasKeyword) {
            this.source = "BOTH";
        } else if (hasSemantic) {
            this.source = "SEMANTIC";
        } else if (hasKeyword) {
            this.source = "KEYWORD";
        } else {
            this.source = "UNKNOWN";
        }
    }

    /**
     * Конвертирует из MergedSearchResultDto
     */
    public static FinalSearchResultDto from(MergedSearchResultDto merged) {
        FinalSearchResultDto result = FinalSearchResultDto.builder()
                .chunkId(merged.getChunkId())
                .documentId(merged.getDocumentId())
                .documentName(merged.getDocumentName())
                .chunkIndex(merged.getChunkIndex())
                .chunkText(merged.getChunkText())
                .metadata(merged.getMetadata())
                .createdAt(merged.getCreatedAt())
                .semanticScore(merged.getSemanticScore())
                .keywordScore(merged.getKeywordScore())
                .combinedScore(merged.getMergedScore())
                .build();

        result.determineSource();
        return result;
    }
}

