package de.jivz.rag.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Результат поиска - чанк с оценкой релевантности.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResultDto {

    private Long chunkId;
    private Long documentId;
    private String documentName;
    private Integer chunkIndex;
    private String chunkText;
    private Double similarity;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
}

