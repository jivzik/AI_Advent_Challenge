package de.jivz.rag.dto;


import lombok.Builder;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Search response DTO.
 */
@Getter
@Builder
public class SearchResponseDto {

    private final String query;
    private final String searchMode;
    private final int resultsCount;
    private final String processingTime;
    private final List<Map<String, Object>> results;

    // Optional fields
    private final Long documentId;
    private final String rankingMethod;

    /**
     * Создать response из результатов поиска.
     */
    public static SearchResponseDto from(List<SearchResultDto> results,
                                      SearchContext context) {
        return SearchResponseDto.builder()
                .query(context.query())
                .searchMode(context.searchMode())
                .resultsCount(results.size())
                .processingTime(context.formattedTime())
                .results(formatResults(results, context.searchMode()))
                .documentId(context.documentId())
                .rankingMethod(context.rankingMethod())
                .build();
    }

    private static List<Map<String, Object>> formatResults(List<SearchResultDto> results,
                                                           String searchMode) {
        return results.stream()
                .map(r -> formatResult(r, searchMode))
                .toList();
    }

    private static Map<String, Object> formatResult(SearchResultDto r, String searchMode) {
        var map = new LinkedHashMap<String, Object>();

        if (r.getChunkId() != null) {
            map.put("chunkId", r.getChunkId());
        }
        map.put("documentName", r.getDocumentName() != null ? r.getDocumentName() : "");
        map.put("chunkText", r.getChunkText() != null ? r.getChunkText() : "");
        map.put("chunkIndex", r.getChunkIndex() != null ? r.getChunkIndex() : 0);

        // Для keyword режима используем "relevance", иначе "similarity"
        double score = r.getSimilarity() != null ? r.getSimilarity() : 0.0;
        if ("keyword".equals(searchMode) || "advanced".equals(searchMode) || "ranked".equals(searchMode)) {
            map.put("relevance", score);
        } else {
            map.put("similarity", score);
        }

        if (r.getCreatedAt() != null) {
            map.put("createdAt", r.getCreatedAt());
        }

        return map;
    }

    /**
     * Контекст поиска для формирования ответа.
     */
    public record SearchContext(
            String query,
            String searchMode,
            long startTime,
            Long documentId,
            String rankingMethod
    ) {
        public SearchContext(String query, String searchMode, long startTime) {
            this(query, searchMode, startTime, null, null);
        }

        public SearchContext withDocumentId(Long documentId) {
            return new SearchContext(query, searchMode, startTime, documentId, rankingMethod);
        }

        public SearchContext withRankingMethod(String method) {
            return new SearchContext(query, searchMode, startTime, documentId, method);
        }

        public String formattedTime() {
            long millis = System.currentTimeMillis() - startTime;
            return millis < 1000 ? millis + "ms" : String.format("%.1fs", millis / 1000.0);
        }
    }
}