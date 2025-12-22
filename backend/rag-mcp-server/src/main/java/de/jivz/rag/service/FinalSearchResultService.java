package de.jivz.rag.service;

import de.jivz.rag.dto.FinalRankingConfig;
import de.jivz.rag.dto.FinalSearchResultDto;
import de.jivz.rag.dto.MergedSearchResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис для финальной сортировки и фильтрации результатов.
 *
 * ЭТАП 5: Финальная сортировка и фильтрация
 *
 * Функции:
 * 1. Сортировка по combined_score (убывание)
 * 2. Фильтрация по минимальному порогу
 * 3. Удаление дубликатов (max N чанков с одного документа)
 * 4. Ограничение на топ-K результатов
 * 5. Добавление метаданных (ранг, процентиль, источник)
 *
 * Результат: Список из максимум K чанков, отсортированных по релевантности,
 * с добавленными метаданными.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FinalSearchResultService {

    /**
     * Выполняет финальную сортировку и фильтрацию результатов.
     *
     * @param results результаты для обработки
     * @param config конфигурация сортировки и фильтрации
     * @return отфильтрованные и отсортированные результаты с метаданными
     */
    public List<FinalSearchResultDto> finalizeResults(
            List<MergedSearchResultDto> results,
            FinalRankingConfig config) {

        if (results == null || results.isEmpty()) {
            log.warn("⚠️  Results for finalization is empty");
            return new ArrayList<>();
        }

        config.validate();

        log.info("🎯 Finalizing {} results with config: threshold={}, topK={}, maxPerDoc={}",
                results.size(),
                config.getMinScoreThreshold(),
                config.getTopK(),
                config.getMaxChunksPerDocument());

        // Шаг 1: Сортировка по combined_score (если требуется)
        List<MergedSearchResultDto> sorted = results;
        if (config.getSortByScore()) {
            sorted = sortByScore(results);
        }

        // Шаг 2: Фильтрация по минимальному порогу
        List<MergedSearchResultDto> filtered = filterByThreshold(sorted, config);

        // Шаг 3: Удаление дубликатов
        List<MergedSearchResultDto> deduplicated = removeDuplicates(filtered, config);

        // Шаг 4: Ограничение на максимум чанков с одного документа
        List<MergedSearchResultDto> limited = limitChunksPerDocument(deduplicated, config);

        // Шаг 5: Ограничение на топ-K результатов
        List<MergedSearchResultDto> topK = limitTopK(limited, config);

        // Шаг 6: Конвертирование в FinalSearchResultDto с метаданными
        List<FinalSearchResultDto> finalized = convertToFinal(topK, config);

        log.info("✅ Finalization completed: {} final results",
                finalized.size());

        // Логируем результаты
        for (int i = 0; i < finalized.size(); i++) {
            FinalSearchResultDto result = finalized.get(i);
            log.debug("  {}. docId={}, chunkId={}, score={}, source={}, rank={}",
                    i + 1,
                    result.getDocumentId(),
                    result.getChunkId(),
                    String.format("%.4f", result.getCombinedScore()),
                    result.getSource(),
                    result.getRelevanceRank());
        }

        return finalized;
    }

    /**
     * Сортирует результаты по combined_score в порядке убывания.
     *
     * @param results результаты для сортировки
     * @return отсортированные результаты
     */
    private List<MergedSearchResultDto> sortByScore(List<MergedSearchResultDto> results) {
        log.debug("📊 Sorting by combined_score (descending)");

        return results.stream()
                .sorted((a, b) -> {
                    Double scoreA = a.getMergedScore() != null ? a.getMergedScore() : 0.0;
                    Double scoreB = b.getMergedScore() != null ? b.getMergedScore() : 0.0;
                    return scoreB.compareTo(scoreA);
                })
                .collect(Collectors.toList());
    }

    /**
     * Фильтрует результаты по минимальному порогу combined_score.
     *
     * @param results результаты для фильтрации
     * @param config конфигурация с порогом
     * @return отфильтрованные результаты
     */
    private List<MergedSearchResultDto> filterByThreshold(
            List<MergedSearchResultDto> results,
            FinalRankingConfig config) {

        Double threshold = config.getMinScoreThreshold();
        if (threshold <= 0.0) {
            log.debug("⏭️  Threshold filtering disabled (threshold={})");
            return results;
        }

        log.debug("🔍 Filtering by minScoreThreshold={}", threshold);

        List<MergedSearchResultDto> filtered = results.stream()
                .filter(result -> {
                    Double score = result.getMergedScore() != null ? result.getMergedScore() : 0.0;
                    return score >= threshold;
                })
                .collect(Collectors.toList());

        int removed = results.size() - filtered.size();
        if (removed > 0) {
            log.debug("  ❌ Filtered out {} results (score < {})",
                    removed, threshold);
        }

        return filtered;
    }

    /**
     * Удаляет дубликаты по содержимому чанков (если требуется).
     *
     * Две версии:
     * 1. Точное совпадение текста
     * 2. Похожесть текста (>95% совпадение)
     *
     * @param results результаты для дедупликации
     * @param config конфигурация
     * @return результаты без дубликатов
     */
    private List<MergedSearchResultDto> removeDuplicates(
            List<MergedSearchResultDto> results,
            FinalRankingConfig config) {

        if (!config.getRemoveDuplicates()) {
            log.debug("⏭️  Duplicate removal disabled");
            return results;
        }

        log.debug("🧹 Removing duplicates (threshold={})",
                config.getDuplicateSimilarityThreshold());

        List<MergedSearchResultDto> deduped = new ArrayList<>();
        Set<String> seenTexts = new HashSet<>();

        for (MergedSearchResultDto result : results) {
            String text = result.getChunkText() != null ? result.getChunkText().trim() : "";

            // Проверяем точное совпадение
            if (seenTexts.contains(text)) {
                log.debug("  ⏭️  Skipped duplicate: chunkId={} (exact match)",
                        result.getChunkId());
                continue;
            }

            // Проверяем похожесть (если требуется)
            boolean isDuplicate = false;
            if (config.getDuplicateSimilarityThreshold() < 1.0) {
                for (String seenText : seenTexts) {
                    double similarity = calculateTextSimilarity(text, seenText);
                    if (similarity >= config.getDuplicateSimilarityThreshold()) {
                        log.debug("  ⏭️  Skipped similar: chunkId={} (similarity={})",
                                result.getChunkId(),
                                String.format("%.2f", similarity));
                        isDuplicate = true;
                        break;
                    }
                }
            }

            if (!isDuplicate) {
                deduped.add(result);
                seenTexts.add(text);
            }
        }

        int removed = results.size() - deduped.size();
        if (removed > 0) {
            log.debug("  ✅ Removed {} duplicates", removed);
        }

        return deduped;
    }

    /**
     * Ограничивает максимальное количество чанков с одного документа.
     *
     * @param results результаты для ограничения
     * @param config конфигурация
     * @return результаты с ограничением
     */
    private List<MergedSearchResultDto> limitChunksPerDocument(
            List<MergedSearchResultDto> results,
            FinalRankingConfig config) {

        Integer maxPerDoc = config.getMaxChunksPerDocument();
        if (maxPerDoc >= Integer.MAX_VALUE) {
            log.debug("⏭️  Per-document limiting disabled");
            return results;
        }

        log.debug("📄 Limiting to max {} chunks per document", maxPerDoc);

        List<MergedSearchResultDto> limited = new ArrayList<>();
        Map<Long, Integer> docChunkCounts = new HashMap<>();

        for (MergedSearchResultDto result : results) {
            Long docId = result.getDocumentId();
            int count = docChunkCounts.getOrDefault(docId, 0);

            if (count < maxPerDoc) {
                limited.add(result);
                docChunkCounts.put(docId, count + 1);
            } else {
                log.debug("  ⏭️  Skipped chunk from doc {}: already have {} chunks",
                        docId, maxPerDoc);
            }
        }

        int removed = results.size() - limited.size();
        if (removed > 0) {
            log.debug("  ✅ Removed {} results (exceeded max per document)", removed);
        }

        return limited;
    }

    /**
     * Ограничивает результаты на топ-K.
     *
     * @param results результаты для ограничения
     * @param config конфигурация
     * @return топ-K результатов
     */
    private List<MergedSearchResultDto> limitTopK(
            List<MergedSearchResultDto> results,
            FinalRankingConfig config) {

        Integer topK = config.getTopK();
        if (topK >= Integer.MAX_VALUE || topK >= results.size()) {
            log.debug("⏭️  topK limiting not needed (topK={}, results={})",
                    topK, results.size());
            return results;
        }

        log.debug("🎯 Limiting to top {} results", topK);

        List<MergedSearchResultDto> limited = results.stream()
                .limit(topK)
                .collect(Collectors.toList());

        int removed = results.size() - limited.size();
        log.debug("  ✅ Removed {} results (excess)", removed);

        return limited;
    }

    /**
     * Конвертирует результаты в FinalSearchResultDto и добавляет метаданные.
     *
     * @param results результаты для конвертирования
     * @param config конфигурация
     * @return финальные результаты с метаданными
     */
    private List<FinalSearchResultDto> convertToFinal(
            List<MergedSearchResultDto> results,
            FinalRankingConfig config) {

        log.debug("📝 Adding metadata (rank, percentile, source)");

        List<FinalSearchResultDto> finalized = new ArrayList<>();

        // Вычисляем статистику для процентилей
        double maxScore = results.stream()
                .map(r -> r.getMergedScore() != null ? r.getMergedScore() : 0.0)
                .max(Double::compare)
                .orElse(1.0);
        double minScore = results.stream()
                .map(r -> r.getMergedScore() != null ? r.getMergedScore() : 0.0)
                .min(Double::compare)
                .orElse(0.0);

        int totalResults = results.size();

        for (int i = 0; i < results.size(); i++) {
            MergedSearchResultDto merged = results.get(i);

            // Конвертируем в FinalSearchResultDto
            FinalSearchResultDto final_result = FinalSearchResultDto.from(merged);

            // Добавляем метаданные
            if (config.getIncludeMetadata()) {
                // Ранг (позиция в списке, начинается с 1)
                final_result.setRelevanceRank(i + 1);

                // Процентиль (какой процент результатов хуже этого)
                double percentile = (totalResults - i) / (double) totalResults * 100.0;
                final_result.setRelevancePercentile(percentile);

                // Источник уже определён в from()
            }

            finalized.add(final_result);
        }

        return finalized;
    }

    /**
     * Вычисляет похожесть двух текстов на основе Jaccard similarity.
     *
     * @param text1 первый текст
     * @param text2 второй текст
     * @return похожесть (0.0 - 1.0)
     */
    private double calculateTextSimilarity(String text1, String text2) {
        if (text1.equals(text2)) {
            return 1.0;
        }

        // Разбиваем на слова
        Set<String> words1 = new HashSet<>(Arrays.asList(text1.toLowerCase().split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(text2.toLowerCase().split("\\s+")));

        // Вычисляем Jaccard similarity
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }

    // ============ Convenience методы ============

    /**
     * Финализирует результаты с конфигурацией по умолчанию.
     *
     * Default: threshold=0.3, topK=10, maxPerDoc=INT_MAX
     *
     * @param results результаты для финализации
     * @return финализированные результаты
     */
    public List<FinalSearchResultDto> finalizeDefault(List<MergedSearchResultDto> results) {
        return finalizeResults(results, FinalRankingConfig.builder().build());
    }

    /**
     * Финализирует результаты с пользовательским порогом и topK.
     *
     * @param results результаты для финализации
     * @param minScoreThreshold минимальный порог
     * @param topK максимальное количество результатов
     * @return финализированные результаты
     */
    public List<FinalSearchResultDto> finalizeWithThreshold(
            List<MergedSearchResultDto> results,
            double minScoreThreshold,
            int topK) {
        return finalizeResults(results, FinalRankingConfig.builder()
                .minScoreThreshold(minScoreThreshold)
                .topK(topK)
                .build());
    }

    /**
     * Финализирует результаты с ограничением на максимум чанков с документа.
     *
     * @param results результаты для финализации
     * @param topK максимальное количество результатов
     * @param maxChunksPerDocument максимум чанков с одного документа
     * @return финализированные результаты
     */
    public List<FinalSearchResultDto> finalizeWithDiversification(
            List<MergedSearchResultDto> results,
            int topK,
            int maxChunksPerDocument) {
        return finalizeResults(results, FinalRankingConfig.builder()
                .minScoreThreshold(0.3)
                .topK(topK)
                .maxChunksPerDocument(maxChunksPerDocument)
                .build());
    }

    /**
     * Финализирует результаты с удалением дубликатов.
     *
     * @param results результаты для финализации
     * @param topK максимальное количество результатов
     * @return финализированные результаты без дубликатов
     */
    public List<FinalSearchResultDto> finalizeWithDeduplication(
            List<MergedSearchResultDto> results,
            int topK) {
        return finalizeResults(results, FinalRankingConfig.builder()
                .minScoreThreshold(0.3)
                .topK(topK)
                .removeDuplicates(true)
                .build());
    }
}

