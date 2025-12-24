package de.jivz.rag.dto;

import lombok.*;

import java.util.List;

/**
 * Запрос на поиск документов.
 * Поддерживает semantic, keyword и hybrid режимы.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchRequest {

    /**
     * Поисковый запрос (текст).
     */
    private String query;

    /**
     * Количество результатов (default: 5).
     */
    @Builder.Default
    private Integer topK = 5;

    /**
     * Минимальный порог сходства (0.0 - 1.0, default: 0.5).
     */
    @Builder.Default
    private Double threshold = 0.5;

    /**
     * ID документа для поиска только в нём (optional).
     */
    private Long documentId;

    /**
     * Режим поиска: semantic, keyword, hybrid (default: semantic).
     */
    @Builder.Default
    private String searchMode = "semantic";

    /**
     * Вес семантического поиска для hybrid режима (0.0 - 1.0, default: 0.5).
     */
    @Builder.Default
    private Double semanticWeight = 0.5;

    /**
     * Фильтр по именам документов (optional).
     */
    private List<String> documents;

    // ========== Параметры фильтрации релевантности ==========

    /**
     * Применять ли фильтр релевантности (default: false).
     */
    @Builder.Default
    private Boolean applyRelevanceFilter = false;

    /**
     * Тип фильтра релевантности: THRESHOLD или NOOP (default: THRESHOLD).
     */
    @Builder.Default
    private String relevanceFilterType = "THRESHOLD";

    /**
     * Порог фильтра релевантности (0.0 - 1.0, default: 0.5).
     * Используется для THRESHOLD фильтра.
     */
    @Builder.Default
    private Double relevanceFilterThreshold = 0.5;

    // ========== Helper methods ==========

    public boolean hasQuery() {
        return query != null && !query.isBlank();
    }

    public int topKOrDefault() {
        return topK != null ? topK : 5;
    }

    public double thresholdOrDefault() {
        return threshold != null ? threshold : 0.5;
    }

    public String modeOrDefault() {
        return searchMode != null ? searchMode : "semantic";
    }

    public double semanticWeightOrDefault() {
        return semanticWeight != null ? semanticWeight : 0.5;
    }

    public boolean shouldApplyRelevanceFilter() {
        return applyRelevanceFilter != null && applyRelevanceFilter;
    }

    public String getRelevanceFilterTypeOrDefault() {
        return relevanceFilterType != null ? relevanceFilterType : "THRESHOLD";
    }

    public double getRelevanceFilterThresholdOrDefault() {
        return relevanceFilterThreshold != null ? relevanceFilterThreshold : 0.5;
    }
}