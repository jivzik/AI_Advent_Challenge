package de.jivz.rag.service;

import de.jivz.rag.dto.MergedSearchResultDto;
import de.jivz.rag.dto.SearchResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис для объединения (merging) результатов семантического и ключевого поиска.
 *
 * ЭТАП 3: Объединение результатов
 *
 * Алгоритм:
 * 1. Создаём Map для хранения всех уникальных чанков (ключ = chunk_id)
 * 2. Обрабатываем семантические результаты:
 *    - Добавляем в Map с semanticScore
 * 3. Обрабатываем результаты ключевого поиска:
 *    - Если chunk_id уже в Map → добавляем keywordScore
 *    - Если нет → добавляем новый с keywordScore
 * 4. Вычисляем комбинированную оценку (mergedScore)
 * 5. Сортируем по mergedScore в порядке убывания
 *
 * Пример:
 * Semantic: [(chunk1, 0.89), (chunk3, 0.82), (chunk5, 0.75)]
 * Keyword:  [(chunk2, 0.95), (chunk1, 0.88), (chunk4, 0.70)]
 *
 * После объединения:
 * chunk1: semantic=0.89, keyword=0.88, merged=0.866 (0.6*0.89 + 0.4*0.88)
 * chunk2: semantic=null, keyword=0.95, merged=0.380 (0.4*0.95)
 * chunk3: semantic=0.82, keyword=null, merged=0.492 (0.6*0.82)
 * chunk4: semantic=null, keyword=0.70, merged=0.280 (0.4*0.70)
 * chunk5: semantic=0.75, keyword=null, merged=0.450 (0.6*0.75)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SearchResultMergingService {

    /**
     * Объединяет результаты семантического и ключевого поиска.
     *
     * @param semanticResults результаты семантического поиска
     * @param keywordResults результаты ключевого поиска
     * @param semanticWeight вес семантического поиска (0.0-1.0)
     * @param keywordWeight вес ключевого поиска (0.0-1.0)
     * @param topK максимальное количество результатов
     * @return объединённые результаты, отсортированные по mergedScore
     */
    public List<MergedSearchResultDto> mergeResults(
            List<SearchResultDto> semanticResults,
            List<SearchResultDto> keywordResults,
            double semanticWeight,
            double keywordWeight,
            int topK) {

        log.info("🔀 Merging search results: semantic={}, keyword={}, weight=({}/{})  topK={}",
                semanticResults != null ? semanticResults.size() : 0,
                keywordResults != null ? keywordResults.size() : 0,
                semanticWeight, keywordWeight,
                topK);

        // Нормализуем веса (в сумме должны быть 1.0)
        double totalWeight = semanticWeight + keywordWeight;
        double normalizedSemanticWeight;
        double normalizedKeywordWeight;

        if (totalWeight > 0) {
            normalizedSemanticWeight = semanticWeight / totalWeight;
            normalizedKeywordWeight = keywordWeight / totalWeight;
        } else {
            normalizedSemanticWeight = 0.5;
            normalizedKeywordWeight = 0.5;
        }

        log.debug("📊 Normalized weights: semantic={}, keyword={}",
                normalizedSemanticWeight, normalizedKeywordWeight);

        // Создаём Map для хранения всех уникальных чанков
        Map<Long, MergedSearchResultDto> mergedMap = new LinkedHashMap<>();

        // Шаг 1: Обрабатываем семантические результаты
        if (semanticResults != null && !semanticResults.isEmpty()) {
            for (SearchResultDto result : semanticResults) {
                if (result.getChunkId() == null) continue;

                MergedSearchResultDto merged = MergedSearchResultDto.builder()
                        .chunkId(result.getChunkId())
                        .documentId(result.getDocumentId())
                        .documentName(result.getDocumentName())
                        .chunkIndex(result.getChunkIndex())
                        .chunkText(result.getChunkText())
                        .metadata(result.getMetadata())
                        .createdAt(result.getCreatedAt())
                        .semanticScore(result.getSimilarity())
                        .keywordScore(null)
                        .build();

                mergedMap.put(result.getChunkId(), merged);
                log.debug("  📌 Added semantic result: chunkId={}, score={}",
                        result.getChunkId(), result.getSimilarity());
            }
        }

        // Шаг 2: Обрабатываем результаты ключевого поиска
        if (keywordResults != null && !keywordResults.isEmpty()) {
            for (SearchResultDto result : keywordResults) {
                if (result.getChunkId() == null) continue;

                if (mergedMap.containsKey(result.getChunkId())) {
                    // Чанк уже есть → добавляем keyword score
                    MergedSearchResultDto existing = mergedMap.get(result.getChunkId());
                    existing.setKeywordScore(result.getSimilarity());
                    log.debug("  🔗 Updated with keyword score: chunkId={}, score={}",
                            result.getChunkId(), result.getSimilarity());
                } else {
                    // Новый чанк → добавляем его
                    MergedSearchResultDto merged = MergedSearchResultDto.builder()
                            .chunkId(result.getChunkId())
                            .documentId(result.getDocumentId())
                            .documentName(result.getDocumentName())
                            .chunkIndex(result.getChunkIndex())
                            .chunkText(result.getChunkText())
                            .metadata(result.getMetadata())
                            .createdAt(result.getCreatedAt())
                            .semanticScore(null)
                            .keywordScore(result.getSimilarity())
                            .build();

                    mergedMap.put(result.getChunkId(), merged);
                    log.debug("  📌 Added keyword-only result: chunkId={}, score={}",
                            result.getChunkId(), result.getSimilarity());
                }
            }
        }

        // Шаг 3: Вычисляем комбинированные оценки
        final double finalSemanticWeight = normalizedSemanticWeight;
        final double finalKeywordWeight = normalizedKeywordWeight;

        mergedMap.values().forEach(result ->
                result.calculateMergedScore(finalSemanticWeight, finalKeywordWeight)
        );

        // Шаг 4: Сортируем по mergedScore в порядке убывания
        List<MergedSearchResultDto> sortedResults = mergedMap.values().stream()
                .sorted((a, b) -> {
                    Double scoreA = a.getMergedScore() != null ? a.getMergedScore() : 0.0;
                    Double scoreB = b.getMergedScore() != null ? b.getMergedScore() : 0.0;
                    return scoreB.compareTo(scoreA); // Убывающий порядок
                })
                .limit(topK)
                .collect(Collectors.toList());

        log.info("✅ Merged {} results, top {} selected", mergedMap.size(), sortedResults.size());

        // Логируем результаты
        for (int i = 0; i < sortedResults.size(); i++) {
            MergedSearchResultDto result = sortedResults.get(i);
            log.debug("  {}. chunkId={}, semantic={}, keyword={}, merged={}",
                    i + 1,
                    result.getChunkId(),
                    String.format("%.3f", result.getSemanticScore() != null ? result.getSemanticScore() : 0.0),
                    String.format("%.3f", result.getKeywordScore() != null ? result.getKeywordScore() : 0.0),
                    String.format("%.3f", result.getMergedScore() != null ? result.getMergedScore() : 0.0));
        }

        return sortedResults;
    }

    /**
     * Объединяет результаты с параметрами по умолчанию (0.6 / 0.4).
     *
     * @param semanticResults результаты семантического поиска
     * @param keywordResults результаты ключевого поиска
     * @param topK максимальное количество результатов
     * @return объединённые результаты
     */
    public List<MergedSearchResultDto> mergeResults(
            List<SearchResultDto> semanticResults,
            List<SearchResultDto> keywordResults,
            int topK) {
        return mergeResults(semanticResults, keywordResults, 0.6, 0.4, topK);
    }

    /**
     * Объединяет результаты с параметрами по умолчанию и возвращает все результаты.
     *
     * @param semanticResults результаты семантического поиска
     * @param keywordResults результаты ключевого поиска
     * @return объединённые результаты
     */
    public List<MergedSearchResultDto> mergeResults(
            List<SearchResultDto> semanticResults,
            List<SearchResultDto> keywordResults) {
        return mergeResults(semanticResults, keywordResults, Integer.MAX_VALUE);
    }

    /**
     * Фильтрует объединённые результаты по минимальной оценке.
     *
     * @param results результаты для фильтрации
     * @param minScore минимальная оценка
     * @return отфильтрованные результаты
     */
    public List<MergedSearchResultDto> filterByScore(
            List<MergedSearchResultDto> results,
            double minScore) {

        log.info("🔍 Filtering merged results by minScore={}", minScore);

        List<MergedSearchResultDto> filtered = results.stream()
                .filter(result -> {
                    Double score = result.getMergedScore() != null ? result.getMergedScore() : 0.0;
                    return score >= minScore;
                })
                .collect(Collectors.toList());

        log.info("✅ Filtered: {} → {} results", results.size(), filtered.size());
        return filtered;
    }

    /**
     * Преобразует объединённые результаты в SearchResultDto (для обратной совместимости).
     *
     * Использует mergedScore в качестве similarity.
     *
     * @param mergedResults объединённые результаты
     * @return список SearchResultDto
     */
    public List<SearchResultDto> toSearchResultDtos(List<MergedSearchResultDto> mergedResults) {
        return mergedResults.stream()
                .map(merged -> SearchResultDto.builder()
                        .chunkId(merged.getChunkId())
                        .documentId(merged.getDocumentId())
                        .documentName(merged.getDocumentName())
                        .chunkIndex(merged.getChunkIndex())
                        .chunkText(merged.getChunkText())
                        .metadata(merged.getMetadata())
                        .createdAt(merged.getCreatedAt())
                        .similarity(merged.getMergedScore())
                        .build())
                .collect(Collectors.toList());
    }
}

