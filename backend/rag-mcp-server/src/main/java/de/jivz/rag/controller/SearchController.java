package de.jivz.rag.controller;

import de.jivz.rag.dto.SearchResultDto;
import de.jivz.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST контроллер для семантического поиска.
 *
 * POST /api/search
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SearchController {

    private final RagService ragService;

    /**
     * Семантический, ключевой или гибридный поиск по документам.
     *
     * POST /api/search
     * Body: {
     *   "query": "...",
     *   "topK": 5,
     *   "threshold": 0.7,
     *   "searchMode": "semantic|keyword|hybrid",
     *   "semanticWeight": 0.5,
     *   "documents": ["doc1.pdf", "doc2.pdf"]
     * }
     */
    @PostMapping
    public ResponseEntity<?> search(@RequestBody SearchRequestBody request) {
        long startTime = System.currentTimeMillis();

        String searchMode = request.searchMode() != null ? request.searchMode() : "semantic";
        log.info("🔍 Search: query='{}', topK={}, threshold={}, mode={}, semanticWeight={}",
                request.query(), request.topK(), request.threshold(), searchMode, request.semanticWeight());

        if (request.query() == null || request.query().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Query is required"));
        }

        int topK = request.topK() != null ? request.topK() : 5;
        double threshold = request.threshold() != null ? request.threshold() : 0.7;
        Long documentId = null;

        // Wenn documents gefiltert werden, verwenden wir nur den ersten für den moment
        // (später könnte das erweitert werden für mehrere Dokumente)
        if (request.documents() != null && !request.documents().isEmpty()) {
            log.info("📋 Filtering by documents: {}", request.documents());
        }

        List<SearchResultDto> results;

        switch (searchMode) {
            case "keyword" -> {
                log.info("🔑 Using keyword search mode");
                results = ragService.keywordSearch(request.query(), topK);
            }
            case "hybrid" -> {
                double semanticWeight = request.semanticWeight() != null ? request.semanticWeight() : 0.5;
                log.info("🔄 Using hybrid search mode (semantic weight: {}%)", Math.round(semanticWeight * 100));
                results = ragService.hybridSearch(request.query(), topK, threshold, semanticWeight);
            }
            case "semantic" -> {
                log.info("🧠 Using semantic search mode");
                results = ragService.search(request.query(), topK, threshold, null);
            }
            default -> {
                log.warn("❌ Unknown search mode: {}, defaulting to semantic", searchMode);
                results = ragService.search(request.query(), topK, threshold, null);
            }
        }

        long processingTime = System.currentTimeMillis() - startTime;

        // Преобразуем в формат из промта
        List<Map<String, Object>> formattedResults = results.stream()
                .map(r -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("documentName", r.getDocumentName() != null ? r.getDocumentName() : "");
                    map.put("chunkText", r.getChunkText() != null ? r.getChunkText() : "");
                    map.put("similarity", r.getSimilarity() != null ? r.getSimilarity() : 0.0);
                    map.put("chunkIndex", r.getChunkIndex() != null ? r.getChunkIndex() : 0);
                    // Für Keyword-Modus umbenennen
                    if ("keyword".equals(searchMode)) {
                        map.put("relevance", map.get("similarity"));
                    }
                    return map;
                })
                .toList();

        return ResponseEntity.ok(Map.of(
                "results", formattedResults,
                "processingTime", formatTime(processingTime),
                "searchMode", searchMode,
                "resultsCount", results.size()
        ));
    }

    /**
     * Полнотекстовый поиск по ключевым словам (Keyword Search / FTS).
     *
     * POST /api/search/keywords
     * Body: { "query": "...", "topK": 5 }
     *
     * Использует PostgreSQL FTS для быстрого полнотекстового поиска.
     * Поддерживает русский язык с морфологической нормализацией.
     */
    @PostMapping("/keywords")
    public ResponseEntity<?> keywordSearch(@RequestBody KeywordSearchRequest request) {
        long startTime = System.currentTimeMillis();

        log.info("🔍 Keyword search: query='{}', topK={}", request.query(), request.topK());

        if (request.query() == null || request.query().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Query is required"));
        }

        int topK = request.topK() != null ? request.topK() : 10;

        List<SearchResultDto> results = ragService.keywordSearch(request.query(), topK);
        long processingTime = System.currentTimeMillis() - startTime;

        List<Map<String, Object>> formattedResults = results.stream()
                .map(r -> Map.<String, Object>of(
                        "chunkId", r.getChunkId(),
                        "documentName", r.getDocumentName() != null ? r.getDocumentName() : "",
                        "chunkText", r.getChunkText() != null ? r.getChunkText() : "",
                        "relevance", r.getSimilarity() != null ? r.getSimilarity() : 0.0,
                        "chunkIndex", r.getChunkIndex() != null ? r.getChunkIndex() : 0,
                        "createdAt", r.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(Map.of(
                "query", request.query(),
                "resultsCount", results.size(),
                "results", formattedResults,
                "processingTime", formatTime(processingTime)
        ));
    }

    /**
     * Полнотекстовый поиск в конкретном документе.
     *
     * POST /api/search/keywords/document/:documentId
     * Body: { "query": "...", "topK": 5 }
     */
    @PostMapping("/keywords/document/{documentId}")
    public ResponseEntity<?> keywordSearchInDocument(
            @PathVariable Long documentId,
            @RequestBody KeywordSearchRequest request) {
        long startTime = System.currentTimeMillis();

        log.info("🔍 Keyword search in doc: query='{}', docId={}, topK={}",
                request.query(), documentId, request.topK());

        if (request.query() == null || request.query().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Query is required"));
        }

        if (documentId == null || documentId <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid document ID"));
        }

        int topK = request.topK() != null ? request.topK() : 10;

        List<SearchResultDto> results = ragService.keywordSearchInDocument(request.query(), documentId, topK);
        long processingTime = System.currentTimeMillis() - startTime;

        List<Map<String, Object>> formattedResults = results.stream()
                .map(r -> Map.<String, Object>of(
                        "chunkId", r.getChunkId(),
                        "chunkText", r.getChunkText() != null ? r.getChunkText() : "",
                        "relevance", r.getSimilarity() != null ? r.getSimilarity() : 0.0,
                        "chunkIndex", r.getChunkIndex() != null ? r.getChunkIndex() : 0
                ))
                .toList();

        return ResponseEntity.ok(Map.of(
                "documentId", documentId,
                "query", request.query(),
                "resultsCount", results.size(),
                "results", formattedResults,
                "processingTime", formatTime(processingTime)
        ));
    }

    /**
     * Расширенный поиск с поддержкой операторов.
     *
     * POST /api/search/advanced
     * Body: { "query": "python & machine", "topK": 10 }
     *
     * Операторы:
     * - & (AND): оба слова должны присутствовать
     * - | (OR): хотя бы одно слово
     * - ! (NOT): исключить слово
     *
     * Примеры:
     * - "python & java" → содержит оба слова
     * - "python | java" → содержит одно из слов
     * - "ai & !robot" → содержит AI, но не robot
     */
    @PostMapping("/advanced")
    public ResponseEntity<?> advancedSearch(@RequestBody AdvancedSearchRequest request) {
        long startTime = System.currentTimeMillis();

        log.info("🔍 Advanced search: query='{}', topK={}", request.query(), request.topK());

        if (request.query() == null || request.query().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Query is required"));
        }

        int topK = request.topK() != null ? request.topK() : 10;

        List<SearchResultDto> results = ragService.advancedKeywordSearch(request.query(), topK);
        long processingTime = System.currentTimeMillis() - startTime;

        List<Map<String, Object>> formattedResults = results.stream()
                .map(r -> Map.<String, Object>of(
                        "chunkId", r.getChunkId(),
                        "documentName", r.getDocumentName() != null ? r.getDocumentName() : "",
                        "chunkText", r.getChunkText() != null ? r.getChunkText() : "",
                        "relevance", r.getSimilarity() != null ? r.getSimilarity() : 0.0,
                        "chunkIndex", r.getChunkIndex() != null ? r.getChunkIndex() : 0
                ))
                .toList();

        return ResponseEntity.ok(Map.of(
                "query", request.query(),
                "resultsCount", results.size(),
                "results", formattedResults,
                "processingTime", formatTime(processingTime)
        ));
    }

    /**
     * Поиск с расширенным ранжированием (ts_rank_cd).
     *
     * POST /api/search/ranked
     * Body: { "query": "...", "topK": 5 }
     *
     * Использует более точное вычисление релевантности:
     * - TF (частота слов в документе)
     * - IDF (редкость слов в коллекции)
     * - Длина документа
     * - Близость слов друг к другу
     */
    @PostMapping("/ranked")
    public ResponseEntity<?> rankedKeywordSearch(@RequestBody KeywordSearchRequest request) {
        long startTime = System.currentTimeMillis();

        log.info("🔍 Ranked keyword search: query='{}', topK={}", request.query(), request.topK());

        if (request.query() == null || request.query().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Query is required"));
        }

        int topK = request.topK() != null ? request.topK() : 10;

        List<SearchResultDto> results = ragService.advancedRankedKeywordSearch(request.query(), topK);
        long processingTime = System.currentTimeMillis() - startTime;

        List<Map<String, Object>> formattedResults = results.stream()
                .map(r -> Map.<String, Object>of(
                        "chunkId", r.getChunkId(),
                        "documentName", r.getDocumentName() != null ? r.getDocumentName() : "",
                        "chunkText", r.getChunkText() != null ? r.getChunkText() : "",
                        "relevance", r.getSimilarity() != null ? r.getSimilarity() : 0.0,
                        "chunkIndex", r.getChunkIndex() != null ? r.getChunkIndex() : 0
                ))
                .toList();

        return ResponseEntity.ok(Map.of(
                "query", request.query(),
                "resultsCount", results.size(),
                "results", formattedResults,
                "processingTime", formatTime(processingTime),
                "rankingMethod", "ts_rank_cd"
        ));
    }

    private String formatTime(long millis) {
        if (millis < 1000) {
            return millis + "ms";
        }
        return String.format("%.1fs", millis / 1000.0);
    }

    /**
     * Request body für Suchanfragen mit Unterstützung für semantic, keyword und hybrid modes.
     */
    public record SearchRequestBody(
            String query,
            Integer topK,
            Double threshold,
            java.util.List<String> documents,
            String searchMode,
            Double semanticWeight
    ) {}

    /**
     * Request body для полнотекстового поиска по ключевым словам.
     */
    public record KeywordSearchRequest(
            String query,
            Integer topK
    ) {}

    /**
     * Request body для расширенного поиска с операторами.
     */
    public record AdvancedSearchRequest(
            String query,
            Integer topK
    ) {}
}

