package de.jivz.rag.service;

import de.jivz.rag.dto.MergedSearchResultDto;
import de.jivz.rag.dto.SearchQualityMetrics;
import de.jivz.rag.dto.SearchResultDto;
import de.jivz.rag.service.filtering.RelevanceFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для сравнения качества поиска с разными режимами фильтрации.
 *
 * Режимы:
 * A - БЕЗ фильтра (resultsNoFilter)
 * B - С пороговым фильтром (resultsWithThresholdFilter)
 * C - С LLM-фильтром (resultsWithLlmFilter)
 *
 * Ответственность (SRP):
 * - Сравнение результатов ДО и ПОСЛЕ применения разных фильтров
 * - Вычисление метрик качества (precision, recall, F1)
 * - Анализ влияния фильтров на результаты
 *
 * Метрики качества:
 * - Precision: какой % результатов остался полезным
 * - Recall: какой % исходных результатов сохранён
 * - F1-score: сбалансированная метрика precision и recall
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SearchQualityComparator {

    private final RelevanceFilteringService filteringService;
    private final LlmRerankingService llmRerankingService;

    /**
     * Сравнивает результаты с фильтром и без фильтра.
     *
     * @param results исходные результаты
     * @param filter применяемый фильтр
     * @param query поисковый запрос (для логирования)
     * @return метрики сравнения
     */
    public SearchQualityMetrics compareWithAndWithoutFilter(
            List<MergedSearchResultDto> results,
            RelevanceFilter filter,
            String query) {

        long startTime = System.currentTimeMillis();

        if (results == null || results.isEmpty()) {
            log.warn("⚠️  Results for comparison is empty");
            return buildEmptyMetrics(query, filter);
        }

        if (filter == null) {
            log.warn("⚠️  Filter is null");
            return buildEmptyMetrics(query, null);
        }

        log.info("📊 Comparing search quality: {} results, query='{}'",
                results.size(), query);
        log.debug("   Filter: {}", filter.getName());

        // Применяем фильтр
        List<MergedSearchResultDto> filteredResults = filteringService.applyFilter(results, filter);

        // Вычисляем метрики
        SearchQualityMetrics metrics = calculateMetrics(
                query,
                results,
                filteredResults,
                filter
        );

        // Добавляем время выполнения
        metrics.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        metrics.setFilterApplied(true);

        log.info("✅ Comparison completed:");
        log.info("   {}", metrics);

        return metrics;
    }

    /**
     * Сравнивает результаты с заданным фильтром типа (без создания фильтра вручную).
     *
     * @param results исходные результаты
     * @param filterType тип фильтра
     * @param threshold порог для фильтра
     * @param query поисковый запрос
     * @return метрики сравнения
     */
    public SearchQualityMetrics compareWithThresholdFilter(
            List<MergedSearchResultDto> results,
            RelevanceFilteringService.FilterType filterType,
            double threshold,
            String query) {

        RelevanceFilter filter = filteringService.createFilter(filterType, threshold);
        return compareWithAndWithoutFilter(results, filter, query);
    }

    /**
     * Сравнивает три режима фильтрации результатов:
     *
     * Режим A - БЕЗ фильтра: результаты после merge + rerank, без фильтров
     * Режим B - С пороговым фильтром: применяется ThresholdRelevanceFilter по merged_score
     * Режим C - С LLM-фильтром: после LLM-переранжирования + фильтрации по llmScore
     *
     * @param results исходные результаты
     * @param query поисковый запрос
     * @param filterThreshold порог для порогового фильтра (Режим B)
     * @param useLlmReranker использовать ли LLM-переранжирование (Режим C)
     * @param llmFilterThreshold порог для LLM-фильтра (Режим C)
     * @return метрики со сравнением всех трёх режимов
     */
    public SearchQualityMetrics compareThreeModesOfFiltering(
            List<MergedSearchResultDto> results,
            String query,
            double filterThreshold,
            boolean useLlmReranker,
            double llmFilterThreshold) {

        long startTime = System.currentTimeMillis();

        if (results == null || results.isEmpty()) {
            log.warn("⚠️  Results for comparison is empty");
            return buildEmptyMetrics(query, null);
        }

        log.info("📊 Comparing three filtering modes: {} results, query='{}'",
                results.size(), query);
        log.info("   Mode B threshold: {}", filterThreshold);
        log.info("   Mode C enabled: {}, LLM threshold: {}", useLlmReranker, llmFilterThreshold);

        // РЕЖИМ A: БЕЗ фильтра
        List<MergedSearchResultDto> resultsNoFilter = new java.util.ArrayList<>(results);
        log.info("✅ Mode A (No filter): {} results", resultsNoFilter.size());

        // РЕЖИМ B: С пороговым фильтром
        List<MergedSearchResultDto> resultsWithThresholdFilter =
                filteringService.applyThresholdFilter(results, filterThreshold);
        log.info("✅ Mode B (Threshold filter): {} results", resultsWithThresholdFilter.size());

        // РЕЖИМ C: С LLM-фильтром (опционально)
        List<MergedSearchResultDto> resultsWithLlmFilter;
        if (useLlmReranker) {
            // Сначала переранжируем с помощью LLM
            List<MergedSearchResultDto> llmReranked = llmRerankingService.rerankWithLlm(results, query);
            log.debug("  After LLM reranking: {} results", llmReranked.size());

            // Затем применяем LLM-фильтр
            resultsWithLlmFilter = filteringService.applyLlmFilter(llmReranked, llmFilterThreshold);
            log.info("✅ Mode C (LLM filter with threshold {}): {} results", llmFilterThreshold, resultsWithLlmFilter.size());
        } else {
            // Если LLM отключен, resultsWithLlmFilter будет пустым
            resultsWithLlmFilter = new java.util.ArrayList<>();
            log.info("⏭️  Mode C (LLM filter): DISABLED (useLlmReranker=false)");
        }

        // Вычисляем метрики для всех трёх режимов
        return calculateMetricsForThreeModes(
                query,
                resultsNoFilter,
                resultsWithThresholdFilter,
                resultsWithLlmFilter,
                filterThreshold,
                llmFilterThreshold,
                useLlmReranker,
                System.currentTimeMillis() - startTime
        );
    }

    /**
     * Вычисляет метрики качества поиска.
     *
     * @param query поисковый запрос
     * @param resultsWithout результаты БЕЗ фильтра
     * @param resultsWith результаты С фильтром
     * @param filter применённый фильтр
     * @return вычисленные метрики
     */
    private SearchQualityMetrics calculateMetrics(
            String query,
            List<MergedSearchResultDto> resultsWithout,
            List<MergedSearchResultDto> resultsWith,
            RelevanceFilter filter) {

        int countBefore = resultsWithout.size();
        int countAfter = resultsWith.size();
        int countRemoved = countBefore - countAfter;
        double percentageRemoved = countBefore > 0
                ? (countRemoved / (double) countBefore) * 100
                : 0.0;

        // Вычисляем метрики качества
        double precision = calculatePrecision(countAfter, countBefore);
        double recall = calculateRecall(countAfter, countBefore);
        double f1Score = calculateF1Score(precision, recall);

        // Вычисляем статистику scores
        double avgScoreBefore = calculateAverageScore(resultsWithout);
        double avgScoreAfter = calculateAverageScore(resultsWith);
        double minScoreBefore = calculateMinScore(resultsWithout);
        double maxScoreBefore = calculateMaxScore(resultsWithout);
        double minScoreAfter = calculateMinScore(resultsWith);
        double maxScoreAfter = calculateMaxScore(resultsWith);

        // Конвертируем в SearchResultDto для совместимости
        List<SearchResultDto> convertedWithout = convertToSearchResultDto(resultsWithout);
        List<SearchResultDto> convertedWith = convertToSearchResultDto(resultsWith);

        // Формируем комментарий
        String comment = generateComment(countRemoved, avgScoreBefore, avgScoreAfter);

        return SearchQualityMetrics.builder()
                .query(query)
                .filterName(filter.getName())
                .filterDescription(filter.getDescription())
                .countBefore(countBefore)
                .precision(precision)
                .recall(recall)
                .f1Score(f1Score)
                .avgScoreBefore(avgScoreBefore)
                .minScoreBefore(minScoreBefore)
                .maxScoreBefore(maxScoreBefore)
                .comment(comment)
                .build();
    }

    /**
     * Вычисляет метрики для всех трёх режимов фильтрации.
     */
    private SearchQualityMetrics calculateMetricsForThreeModes(
            String query,
            List<MergedSearchResultDto> resultsNoFilter,
            List<MergedSearchResultDto> resultsWithThresholdFilter,
            List<MergedSearchResultDto> resultsWithLlmFilter,
            double filterThreshold,
            double llmFilterThreshold,
            boolean llmFilterApplied,
            long executionTimeMs) {

        // Базовые метрики (по исходным результатам)
        int countBefore = resultsNoFilter.size();
        double avgScoreBefore = calculateAverageScore(resultsNoFilter);
        double minScoreBefore = calculateMinScore(resultsNoFilter);
        double maxScoreBefore = calculateMaxScore(resultsNoFilter);

        // Метрики Режима B (пороговый фильтр)
        int countAfterThreshold = resultsWithThresholdFilter.size();
        int countRemovedThreshold = countBefore - countAfterThreshold;
        double percentageRemovedThreshold = countBefore > 0
                ? (countRemovedThreshold / (double) countBefore) * 100
                : 0.0;
        double avgScoreAfterThreshold = calculateAverageScore(resultsWithThresholdFilter);
        double minScoreAfterThreshold = calculateMinScore(resultsWithThresholdFilter);
        double maxScoreAfterThreshold = calculateMaxScore(resultsWithThresholdFilter);

        // Метрики Режима C (LLM-фильтр)
        int countAfterLlm = llmFilterApplied ? resultsWithLlmFilter.size() : 0;
        int countRemovedLlm = llmFilterApplied ? (countBefore - countAfterLlm) : 0;
        double percentageRemovedLlm = (llmFilterApplied && countBefore > 0)
                ? (countRemovedLlm / (double) countBefore) * 100
                : 0.0;
        double avgLlmScoreBefore = llmFilterApplied ? calculateAverageLlmScore(resultsNoFilter) : 0.0;
        double avgLlmScoreAfter = llmFilterApplied ? calculateAverageLlmScore(resultsWithLlmFilter) : 0.0;
        double avgScoreAfterLlm = llmFilterApplied ? calculateAverageScore(resultsWithLlmFilter) : 0.0;
        double minLlmScoreAfter = llmFilterApplied ? calculateMinLlmScore(resultsWithLlmFilter) : 0.0;
        double maxLlmScoreAfter = llmFilterApplied ? calculateMaxLlmScore(resultsWithLlmFilter) : 0.0;

        // Конвертируем в SearchResultDto
        List<SearchResultDto> convertedNoFilter = convertToSearchResultDto(resultsNoFilter);
        List<SearchResultDto> convertedWithThreshold = convertToSearchResultDto(resultsWithThresholdFilter);
        List<SearchResultDto> convertedWithLlm = convertToSearchResultDto(resultsWithLlmFilter);

        // Генерируем комментарий
        String comment = generateCommentForThreeModes(
                countRemovedThreshold, countRemovedLlm,
                avgScoreBefore, avgScoreAfterThreshold, avgLlmScoreAfter,
                llmFilterApplied
        );

        return SearchQualityMetrics.builder()
                .query(query)
                // Результаты трёх режимов (только новые имена)
                .resultsNoFilter(convertedNoFilter)
                .resultsWithThresholdFilter(convertedWithThreshold)
                .resultsWithLlmFilter(convertedWithLlm)
                // Базовые метрики
                .countBefore(countBefore)
                .avgScoreBefore(avgScoreBefore)
                .minScoreBefore(minScoreBefore)
                .maxScoreBefore(maxScoreBefore)
                // Метрики Режима B (новые имена)
                .countAfterThreshold(countAfterThreshold)
                .countRemovedThreshold(countRemovedThreshold)
                .percentageRemovedThreshold(percentageRemovedThreshold)
                .avgScoreAfterThreshold(avgScoreAfterThreshold)
                .minScoreAfterThreshold(minScoreAfterThreshold)
                .maxScoreAfterThreshold(maxScoreAfterThreshold)
                // Метрики Режима C (новые имена)
                .countAfterLlm(countAfterLlm)
                .countRemovedLlm(countRemovedLlm)
                .percentageRemovedLlm(percentageRemovedLlm)
                .avgLlmScoreBefore(avgLlmScoreBefore)
                .avgLlmScoreAfter(avgLlmScoreAfter)
                .avgScoreAfterLlm(avgScoreAfterLlm)
                .minLlmScoreAfter(minLlmScoreAfter)
                .maxLlmScoreAfter(maxLlmScoreAfter)
                // Конфигурация фильтров
                .filterThreshold(filterThreshold)
                .llmFilterThreshold(llmFilterThreshold)
                .thresholdFilterApplied(true)
                .llmFilterApplied(llmFilterApplied)
                // Метаинформация
                .executionTimeMs(executionTimeMs)
                .comment(comment)
                .build();
    }

    /**
     * Вычисляет precision: какой % результатов остался полезным.
     *
     * Precision = countAfter / countBefore
     * (чем выше, тем строже фильтр отсеивает результаты)
     */
    private double calculatePrecision(int countAfter, int countBefore) {
        if (countBefore == 0) return 0.0;
        return (countAfter / (double) countBefore);
    }

    /**
     * Вычисляет recall: какой % исходных результатов сохранён.
     *
     * Recall = countAfter / countBefore
     * (в нашем случае совпадает с precision)
     */
    private double calculateRecall(int countAfter, int countBefore) {
        if (countBefore == 0) return 0.0;
        return (countAfter / (double) countBefore);
    }

    /**
     * Вычисляет F1-score: гармоническое среднее precision и recall.
     *
     * F1 = 2 * (precision * recall) / (precision + recall)
     */
    private double calculateF1Score(double precision, double recall) {
        if (precision + recall == 0.0) return 0.0;
        return 2.0 * (precision * recall) / (precision + recall);
    }

    /**
     * Вычисляет средний score результатов.
     */
    private double calculateAverageScore(List<MergedSearchResultDto> results) {
        if (results == null || results.isEmpty()) {
            return 0.0;
        }
        return results.stream()
                .mapToDouble(r -> r.getMergedScore() != null ? r.getMergedScore() : 0.0)
                .average()
                .orElse(0.0);
    }

    /**
     * Вычисляет средний llmScore результатов.
     */
    private double calculateAverageLlmScore(List<MergedSearchResultDto> results) {
        if (results == null || results.isEmpty()) {
            return 0.0;
        }
        return results.stream()
                .mapToDouble(r -> r.getLlmScore() != null ? r.getLlmScore() : 0.0)
                .average()
                .orElse(0.0);
    }

    /**
     * Вычисляет минимальный score результатов.
     */
    private double calculateMinScore(List<MergedSearchResultDto> results) {
        if (results == null || results.isEmpty()) {
            return 0.0;
        }
        return results.stream()
                .mapToDouble(r -> r.getMergedScore() != null ? r.getMergedScore() : 0.0)
                .min()
                .orElse(0.0);
    }

    /**
     * Вычисляет минимальный llmScore результатов.
     */
    private double calculateMinLlmScore(List<MergedSearchResultDto> results) {
        if (results == null || results.isEmpty()) {
            return 0.0;
        }
        return results.stream()
                .mapToDouble(r -> r.getLlmScore() != null ? r.getLlmScore() : 0.0)
                .min()
                .orElse(0.0);
    }

    /**
     * Вычисляет максимальный score результатов.
     */
    private double calculateMaxScore(List<MergedSearchResultDto> results) {
        if (results == null || results.isEmpty()) {
            return 0.0;
        }
        return results.stream()
                .mapToDouble(r -> r.getMergedScore() != null ? r.getMergedScore() : 0.0)
                .max()
                .orElse(0.0);
    }

    /**
     * Вычисляет максимальный llmScore результатов.
     */
    private double calculateMaxLlmScore(List<MergedSearchResultDto> results) {
        if (results == null || results.isEmpty()) {
            return 0.0;
        }
        return results.stream()
                .mapToDouble(r -> r.getLlmScore() != null ? r.getLlmScore() : 0.0)
                .max()
                .orElse(0.0);
    }

    /**
     * Конвертирует MergedSearchResultDto в SearchResultDto.
     */
    private List<SearchResultDto> convertToSearchResultDto(
            List<MergedSearchResultDto> results) {
        return results.stream()
                .map(merged -> SearchResultDto.builder()
                        .chunkId(merged.getChunkId())
                        .documentId(merged.getDocumentId())
                        .documentName(merged.getDocumentName())
                        .chunkIndex(merged.getChunkIndex())
                        .chunkText(merged.getChunkText())
                        .similarity(merged.getMergedScore())
                        .metadata(merged.getMetadata())
                        .createdAt(merged.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Генерирует автоматический комментарий на основе результатов.
     */
    private String generateComment(int countRemoved, double avgBefore, double avgAfter) {
        String filterImpact = countRemoved == 0
                ? "Фильтр не повлиял на результаты"
                : String.format("Отфильтровано %d результатов", countRemoved);

        String scoreImpact = String.format(
                "Средний score %.4f → %.4f (разница: %.4f)",
                avgBefore, avgAfter, avgAfter - avgBefore
        );

        return String.format("%s. %s", filterImpact, scoreImpact);
    }

    /**
     * Генерирует комментарий для трёх режимов фильтрации.
     */
    private String generateCommentForThreeModes(
            int countRemovedThreshold, int countRemovedLlm,
            double avgScoreBefore, double avgScoreAfterThreshold, double avgLlmScoreAfter,
            boolean llmFilterApplied) {

        String filterImpactB = countRemovedThreshold == 0
                ? "Пороговый фильтр не повлиял на результаты"
                : String.format("Отфильтровано %d результатов пороговым фильтром", countRemovedThreshold);

        String filterImpactC = countRemovedLlm == 0
                ? "LLM-фильтр не повлиял на результаты"
                : String.format("Отфильтровано %d результатов LLM-фильтром", countRemovedLlm);

        String scoreImpactB = String.format(
                "Средний score до фильтрации: %.4f, после: %.4f (разница: %.4f)",
                avgScoreBefore, avgScoreAfterThreshold, avgScoreAfterThreshold - avgScoreBefore
        );
        String scoreImpactC = String.format(
                "Средний llmScore до фильтрации: %.4f, после: %.4f (разница: %.4f)",
                avgScoreBefore, avgLlmScoreAfter, avgLlmScoreAfter - avgScoreBefore
        );

        return String.format("%s. %s. %s", filterImpactB, filterImpactC, scoreImpactB);
    }

    /**
     * Создаёт пустые метрики (когда нет данных для сравнения).
     */
    private SearchQualityMetrics buildEmptyMetrics(String query, RelevanceFilter filter) {
        return SearchQualityMetrics.builder()
                .query(query)
                .filterName(filter != null ? filter.getName() : "Unknown")
                .filterDescription(filter != null ? filter.getDescription() : "N/A")
                .countBefore(0)
                .precision(0.0)
                .recall(0.0)
                .f1Score(0.0)
                .avgScoreBefore(0.0)
                .comment("No results to compare")
                .build();
    }
}

