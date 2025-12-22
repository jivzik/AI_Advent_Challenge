package de.jivz.rag.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.rag.dto.SearchResultDto;
import de.jivz.rag.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Сервис для полнотекстового поиска (Full-Text Search / FTS) в документах.
 *
 * Использует встроенную функциональность PostgreSQL:
 * - tsvector: препроцессированная версия текста для быстрого поиска
 * - GIN индексы: для эффективного поиска по инвертированным индексам
 * - Русская морфология: поддержка окончаний и основ русских слов
 *
 * Преимущества:
 * - Быстрый полнотекстовый поиск (индексированный)
 * - Понимание морфологии (вернуться + вернулись → вернуть)
 * - Ранжирование по релевантности (TF-IDF)
 * - Поддержка сложных запросов (AND, OR, NOT)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KeywordSearchService {

    private final DocumentChunkRepository chunkRepository;
    private final ObjectMapper objectMapper;

    /**
     * Поиск по ключевым словам (простой поиск).
     *
     * Использует plainto_tsquery, которая:
     * - Автоматически нормализует запрос
     * - Применяет русскую морфологию
     * - Объединяет слова оператором AND
     *
     * Например: "вернуться вернулись" → поиск документов с любой формой "вернуть"
     *
     * @param query текст для поиска
     * @param topK максимальное количество результатов
     * @return отсортированный по релевантности список результатов
     */
    public List<SearchResultDto> keywordSearch(String query, int topK) {
        log.info("🔍 Keyword search: query='{}', topK={}", query, topK);

        if (query == null || query.trim().isEmpty()) {
            log.warn("⚠️ Empty search query");
            return List.of();
        }

        try {
            List<Object[]> results = chunkRepository.searchByKeywords(query, topK);
            List<SearchResultDto> searchResults = mapResults(results);
            log.info("✅ Keyword search found {} results", searchResults.size());
            return searchResults;
        } catch (Exception e) {
            log.error("❌ Keyword search error: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Поиск по ключевым словам в конкретном документе.
     *
     * @param query текст для поиска
     * @param documentId ID документа для поиска
     * @param topK максимальное количество результатов
     * @return список результатов из выбранного документа
     */
    public List<SearchResultDto> keywordSearchInDocument(String query, Long documentId, int topK) {
        log.info("🔍 Keyword search in document: query='{}', docId={}, topK={}",
                query, documentId, topK);

        if (query == null || query.trim().isEmpty()) {
            log.warn("⚠️ Empty search query");
            return List.of();
        }

        if (documentId == null || documentId <= 0) {
            log.warn("⚠️ Invalid document ID: {}", documentId);
            return List.of();
        }

        try {
            List<Object[]> results = chunkRepository.searchByKeywordsInDocument(query, documentId, topK);
            List<SearchResultDto> searchResults = mapResults(results);
            log.info("✅ Keyword search in document found {} results", searchResults.size());
            return searchResults;
        } catch (Exception e) {
            log.error("❌ Keyword search in document error: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Расширенный поиск с поддержкой операторов.
     *
     * Поддерживаемые операторы:
     * - & (AND): оба слова должны присутствовать
     * - | (OR): хотя бы одно слово
     * - ! (NOT): исключить слово
     * - <-> (близость): слова рядом
     *
     * Примеры:
     * - "machine & learning" → содержит оба слова
     * - "python | java" → содержит одно из слов
     * - "ai & !robot" → содержит AI, но не robot
     * - "machine <-> learning" → слова рядом
     *
     * @param query tsquery выражение с операторами
     * @param topK максимальное количество результатов
     * @return список результатов
     */
    public List<SearchResultDto> advancedSearch(String query, int topK) {
        log.info("🔍 Advanced search: query='{}', topK={}", query, topK);

        if (query == null || query.trim().isEmpty()) {
            log.warn("⚠️ Empty search query");
            return List.of();
        }

        try {
            List<Object[]> results = chunkRepository.searchByAdvancedQuery(query, topK);
            List<SearchResultDto> searchResults = mapResults(results);
            log.info("✅ Advanced search found {} results", searchResults.size());
            return searchResults;
        } catch (Exception e) {
            log.error("❌ Advanced search error: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Поиск с расширенным ранжированием.
     *
     * Использует ts_rank_cd для более точного расчета релевантности:
     * - Учитывает частоту слов в документе (TF)
     * - Учитывает редкость слов в коллекции (IDF)
     * - Учитывает длину документа
     * - Учитывает близость слов друг к другу
     *
     * Флаг 32 означает:
     * - 1 (log frequency weighting)
     * - 2 (inverse document frequency weighting)
     * - 4 (length normalization)
     * - 8 (extended cover density ranking)
     * - 16 (cover density ranking)
     *
     * @param query текст для поиска
     * @param topK максимальное количество результатов
     * @return отсортированный по улучшенной релевантности список результатов
     */
    public List<SearchResultDto> advancedKeywordSearch(String query, int topK) {
        log.info("🔍 Advanced keyword search (with ts_rank_cd): query='{}', topK={}",
                query, topK);

        if (query == null || query.trim().isEmpty()) {
            log.warn("⚠️ Empty search query");
            return List.of();
        }

        try {
            List<Object[]> results = chunkRepository.searchByKeywordsAdvanced(query, topK);
            List<SearchResultDto> searchResults = mapResults(results);
            log.info("✅ Advanced keyword search found {} results", searchResults.size());
            return searchResults;
        } catch (Exception e) {
            log.error("❌ Advanced keyword search error: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Маппинг результатов native query в DTO.
     *
     * Порядок колонок из query:
     * id, document_id, document_name, chunk_index, chunk_text, metadata, created_at, relevance_score
     */
    @SuppressWarnings("unchecked")
    private List<SearchResultDto> mapResults(List<Object[]> results) {
        List<SearchResultDto> searchResults = new ArrayList<>();

        for (Object[] row : results) {
            try {
                SearchResultDto dto = SearchResultDto.builder()
                        .chunkId(row[0] != null ? ((Number) row[0]).longValue() : null)
                        .documentId(row[1] != null ? ((Number) row[1]).longValue() : null)
                        .documentName((String) row[2])
                        .chunkIndex(row[3] != null ? ((Number) row[3]).intValue() : null)
                        .chunkText((String) row[4])
                        .metadata(parseMetadata(row[5]))
                        .createdAt(row[6] != null ? ((Timestamp) row[6]).toLocalDateTime() : null)
                        .similarity(row[7] != null ? ((Number) row[7]).doubleValue() : null)
                        .build();

                searchResults.add(dto);
            } catch (Exception e) {
                log.warn("⚠️ Error mapping search result: {}", e.getMessage());
            }
        }

        return searchResults;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMetadata(Object metadataObj) {
        if (metadataObj == null) return null;

        if (metadataObj instanceof Map) {
            return (Map<String, Object>) metadataObj;
        }

        if (metadataObj instanceof String) {
            try {
                return objectMapper.readValue((String) metadataObj, Map.class);
            } catch (JsonProcessingException e) {
                log.warn("⚠️ Failed to parse metadata: {}", e.getMessage());
                return null;
            }
        }

        return null;
    }

    /**
     * Нормализует поисковый запрос перед выполнением поиска.
     *
     * Удаляет:
     * - Множественные пробелы
     * - Специальные символы (кроме операторов &, |, !)
     * - Пробелы в начале и конце
     *
     * @param query исходный запрос
     * @return нормализованный запрос
     */
    public static String normalizeQuery(String query) {
        if (query == null) return "";

        // Удаляем множественные пробелы
        query = query.replaceAll("\\s+", " ");

        // Удаляем опасные символы, кроме операторов
        query = query.replaceAll("[^а-яА-ЯёЁa-zA-Z0-9\\s&|!<>\\-]", "");

        return query.trim();
    }

    /**
     * Форматирует tsquery выражение для расширенного поиска.
     *
     * Преобразует простой текст в валидный tsquery синтаксис.
     * Пример: "python AND java" → "python & java"
     *
     * @param query текст для преобразования
     * @return отформатированный tsquery
     */
    public static String formatAsQuery(String query) {
        if (query == null || query.isEmpty()) return "";

        // Нормализуем сначала
        query = normalizeQuery(query);

        // Преобразуем AND/OR/NOT в операторы
        query = query.replaceAll("\\bAND\\b", "&");
        query = query.replaceAll("\\bOR\\b", "|");
        query = query.replaceAll("\\bNOT\\b", "!");

        return query;
    }
}

