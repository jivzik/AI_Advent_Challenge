package de.jivz.rag.service;

import de.jivz.rag.dto.SearchRequest;
import de.jivz.rag.dto.SearchResponseDto;
import de.jivz.rag.dto.SearchResponseDto.SearchContext;
import de.jivz.rag.dto.SearchResultDto;
import de.jivz.rag.service.RagFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис обработки поисковых запросов.
 *
 * Инкапсулирует логику выбора режима поиска и формирования ответа.
 * Контроллер только логирует и делегирует сюда.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SearchRequestService {

    private final RagFacade ragFacade;

    /**
     * Универсальный поиск с выбором режима.
     */
    public SearchResponseDto search(SearchRequest request) {
        long startTime = System.currentTimeMillis();
        String mode = request.modeOrDefault();

        List<SearchResultDto> results = executeSearch(request, mode);

        SearchContext context = new SearchContext(request.getQuery(), mode, startTime);
        return SearchResponseDto.from(results, context);
    }

    /**
     * Keyword поиск.
     */
    public SearchResponseDto keywordSearch(String query, int topK) {
        long startTime = System.currentTimeMillis();

        List<SearchResultDto> results = ragFacade.keywordSearch(query, topK);

        SearchContext context = new SearchContext(query, "keyword", startTime);
        return SearchResponseDto.from(results, context);
    }

    /**
     * Keyword поиск в документе.
     */
    public SearchResponseDto keywordSearchInDocument(String query, Long documentId, int topK) {
        long startTime = System.currentTimeMillis();

        List<SearchResultDto> results = ragFacade.keywordSearchInDocument(query, documentId, topK);

        SearchContext context = new SearchContext(query, "keyword", startTime)
                .withDocumentId(documentId);
        return SearchResponseDto.from(results, context);
    }

    /**
     * Advanced поиск с операторами.
     */
    public SearchResponseDto advancedSearch(String query, int topK) {
        long startTime = System.currentTimeMillis();

        List<SearchResultDto> results = ragFacade.advancedKeywordSearch(query, topK);

        SearchContext context = new SearchContext(query, "advanced", startTime);
        return SearchResponseDto.from(results, context);
    }

    /**
     * Ranked поиск с ts_rank_cd.
     */
    public SearchResponseDto rankedSearch(String query, int topK) {
        long startTime = System.currentTimeMillis();

        List<SearchResultDto> results = ragFacade.advancedRankedKeywordSearch(query, topK);

        SearchContext context = new SearchContext(query, "ranked", startTime)
                .withRankingMethod("ts_rank_cd");
        return SearchResponseDto.from(results, context);
    }

    private List<SearchResultDto> executeSearch(SearchRequest request, String mode) {
        return switch (mode) {
            case "keyword" -> ragFacade.keywordSearch(
                    request.getQuery(),
                    request.topKOrDefault());

            case "hybrid" -> ragFacade.hybridSearch(
                    request.getQuery(),
                    request.topKOrDefault(),
                    request.thresholdOrDefault(),
                    request.semanticWeightOrDefault());

            default -> ragFacade.search(
                    request.getQuery(),
                    request.topKOrDefault(),
                    request.thresholdOrDefault(),
                    request.getDocumentId());
        };
    }
}