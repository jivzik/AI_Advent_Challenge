package de.jivz.rag.controller;

import de.jivz.rag.dto.MergedSearchResultDto;
import de.jivz.rag.dto.SearchQualityMetrics;
import de.jivz.rag.dto.SearchRequest;
import de.jivz.rag.dto.SearchResponseDto;
import de.jivz.rag.service.HybridSearchService;
import de.jivz.rag.service.RelevanceFilteringService;
import de.jivz.rag.service.SearchQualityComparator;
import de.jivz.rag.service.SearchRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST контроллер для поиска по документам.
 * Принимает HTTP запросы, логирует, валидирует и делегирует в SearchRequestService.
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SearchController {

    private final SearchRequestService searchService;
    private final HybridSearchService hybridSearchService;
    private final RelevanceFilteringService filteringService;
    private final SearchQualityComparator qualityComparator;

    // ...existing code...

    /**
     * POST /api/search
     * Универсальный поиск: semantic, keyword, hybrid.
     */
    @PostMapping
    public ResponseEntity<?> search(@RequestBody SearchRequest request) {
        log.info("Search: query='{}', mode={}, topK={}",
                request.getQuery(), request.modeOrDefault(), request.topKOrDefault());

        if (!request.hasQuery()) {
            return badRequest("Query is required");
        }

        SearchResponseDto response = searchService.search(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/search/keywords
     * Полнотекстовый поиск.
     */
    @PostMapping("/keywords")
    public ResponseEntity<?> keywordSearch(@RequestBody SearchRequest request) {
        log.info("Keyword search: query='{}', topK={}", request.getQuery(), request.topKOrDefault());

        if (!request.hasQuery()) {
            return badRequest("Query is required");
        }

        SearchResponseDto response = searchService.keywordSearch(
                request.getQuery(), request.topKOrDefault());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/search/keywords/document/{documentId}
     * Поиск в конкретном документе.
     */
    @PostMapping("/keywords/document/{documentId}")
    public ResponseEntity<?> keywordSearchInDocument(
            @PathVariable Long documentId,
            @RequestBody SearchRequest request) {

        log.info("Keyword search in doc: query='{}', docId={}, topK={}",
                request.getQuery(), documentId, request.topKOrDefault());

        if (!request.hasQuery()) {
            return badRequest("Query is required");
        }
        if (documentId == null || documentId <= 0) {
            return badRequest("Invalid document ID");
        }

        SearchResponseDto response = searchService.keywordSearchInDocument(
                request.getQuery(), documentId, request.topKOrDefault());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/search/advanced
     * Поиск с операторами (AND, OR, NOT).
     */
    @PostMapping("/advanced")
    public ResponseEntity<?> advancedSearch(@RequestBody SearchRequest request) {
        log.info("Advanced search: query='{}', topK={}", request.getQuery(), request.topKOrDefault());

        if (!request.hasQuery()) {
            return badRequest("Query is required");
        }

        SearchResponseDto response = searchService.advancedSearch(
                request.getQuery(), request.topKOrDefault());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/search/ranked
     * Поиск с расширенным ранжированием.
     */
    @PostMapping("/ranked")
    public ResponseEntity<?> rankedSearch(@RequestBody SearchRequest request) {
        log.info("Ranked search: query='{}', topK={}", request.getQuery(), request.topKOrDefault());

        if (!request.hasQuery()) {
            return badRequest("Query is required");
        }

        SearchResponseDto response = searchService.rankedSearch(
                request.getQuery(), request.topKOrDefault());
        return ResponseEntity.ok(response);
    }

    // ========== Новые endpoints для фильтрации релевантности ==========

    /**
     * POST /api/search/compare-quality
     * Сравнивает качество поиска в трёх режимах фильтрации.
     *
     * Query params:
     * - query: поисковый запрос (required)
     * - topK: количество результатов (default: 5)
     * - filterThreshold: порог фильтра релевантности (default: 0.5) - для Режима B
     * - useLlmReranker: использовать LLM-переранжирование (default: false) - для Режима C
     * - llmFilterThreshold: порог LLM-фильтра (default: 0.7) - для Режима C
     *
     * Режимы:
     * A - БЕЗ фильтра: результаты после merge + rerank, без фильтров (resultsNoFilter)
     * B - С пороговым фильтром: применяется ThresholdRelevanceFilter по merged_score (resultsWithThresholdFilter)
     * C - С LLM-фильтром: после LLM-переранжирования + фильтрации по llmScore (resultsWithLlmFilter)
     *
     * Response: SearchQualityMetrics с метриками для всех трёх режимов
     *
     * Примеры:
     * POST /api/search/compare-quality?query=machine+learning&topK=10&filterThreshold=0.6
     * POST /api/search/compare-quality?query=истории+про+детей&topK=10&filterThreshold=0.3&useLlmReranker=true&llmFilterThreshold=0.7
     */
    @PostMapping("/compare-quality")
    public ResponseEntity<?> compareSearchQuality(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK,
            @RequestParam(defaultValue = "0.5") double filterThreshold,
            @RequestParam(defaultValue = "false") boolean useLlmReranker,
            @RequestParam(defaultValue = "0.7") double llmFilterThreshold) {

        log.info("Compare search quality: query='{}', topK={}, filterThreshold={}, useLlmReranker={}, llmFilterThreshold={}",
                query, topK, filterThreshold, useLlmReranker, llmFilterThreshold);

        if (query == null || query.isBlank()) {
            return badRequest("Query is required");
        }

        if (topK <= 0 || topK > 100) {
            return badRequest("topK must be between 1 and 100");
        }

        if (filterThreshold < 0.0 || filterThreshold > 1.0) {
            return badRequest("filterThreshold must be between 0.0 and 1.0");
        }

        if (llmFilterThreshold < 0.0 || llmFilterThreshold > 1.0) {
            return badRequest("llmFilterThreshold must be between 0.0 and 1.0");
        }

        try {
            // Выполняем гибридный поиск (без фильтра изначально)
            List<MergedSearchResultDto> hybridResults = hybridSearchService.search(
                    query,
                    topK,
                    0.0,
                    0.6,  // Default semantic weight
                    0.4   // Default keyword weight
            );

            // Сравниваем три режима фильтрации
            SearchQualityMetrics metrics = qualityComparator.compareThreeModesOfFiltering(
                    hybridResults,
                    query,
                    filterThreshold,
                    useLlmReranker,
                    llmFilterThreshold
            );

            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            log.error("Error comparing search quality", e);
            return ResponseEntity.status(500).body(
                    Map.of("error", "Failed to compare search quality: " + e.getMessage())
            );
        }
    }

    /**
     * POST /api/search/with-filter
     * Выполняет гибридный поиск с применением фильтра релевантности.
     *
     * Request body: SearchRequest
     * - applyRelevanceFilter: true/false
     * - relevanceFilterType: "THRESHOLD" или "NOOP"
     * - relevanceFilterThreshold: 0.0 - 1.0
     *
     * Response: SearchResponseDto
     */
    @PostMapping("/with-filter")
    public ResponseEntity<?> searchWithFilter(@RequestBody SearchRequest request) {
        log.info("Search with filter: query='{}', mode={}, topK={}, applyFilter={}",
                request.getQuery(), request.modeOrDefault(), request.topKOrDefault(),
                request.shouldApplyRelevanceFilter());

        if (!request.hasQuery()) {
            return badRequest("Query is required");
        }

        SearchResponseDto response = searchService.search(request);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }
}