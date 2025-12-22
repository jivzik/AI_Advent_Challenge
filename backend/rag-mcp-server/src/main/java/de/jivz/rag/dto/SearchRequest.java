package de.jivz.rag.dto;

import lombok.*;

/**
 * Запрос на поиск документов.
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
}

