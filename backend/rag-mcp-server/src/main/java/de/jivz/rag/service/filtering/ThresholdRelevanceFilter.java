package de.jivz.rag.service.filtering;

import de.jivz.rag.dto.MergedSearchResultDto;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Фильтр релевантности на основе порогового значения (threshold).
 *
 * Принцип: Отфильтровывает результаты, у которых merged_score < threshold.
 * Следует SRP: один класс отвечает за одну стратегию фильтрации.
 *
 * Пример использования:
 * - Threshold = 0.5: фильтрует результаты с score < 0.5
 * - Threshold = 0.0: не фильтрует (все результаты проходят)
 * - Threshold = 1.0: очень строгий (только идеальные match)
 */
@Slf4j
public class ThresholdRelevanceFilter implements RelevanceFilter {

    private final double threshold;

    /**
     * Создаёт фильтр с заданным порогом.
     *
     * @param threshold минимальный порог релевантности (0.0 - 1.0)
     */
    public ThresholdRelevanceFilter(double threshold) {
        this.threshold = Math.max(0.0, Math.min(1.0, threshold));
    }

    /**
     * Применяет пороговую фильтрацию к результатам.
     *
     * @param results исходные результаты
     * @return результаты, где merged_score >= threshold
     */
    @Override
    public List<MergedSearchResultDto> filter(List<MergedSearchResultDto> results) {
        if (results == null || results.isEmpty()) {
            log.warn("⚠️  Results for filtering is empty");
            return results;
        }

        log.debug("🔍 Applying {} with threshold={}",
                getName(), String.format("%.4f", threshold));

        long beforeCount = results.size();

        List<MergedSearchResultDto> filtered = results.stream()
                .filter(result -> {
                    Double score = result.getMergedScore() != null ? result.getMergedScore() : 0.0;
                    boolean passes = score >= threshold;

                    if (!passes) {
                        log.debug("  ❌ Filtered out: chunkId={}, score={}, threshold={}",
                                result.getChunkId(),
                                String.format("%.4f", score),
                                String.format("%.4f", threshold));
                    }

                    return passes;
                })
                .collect(Collectors.toList());

        long afterCount = filtered.size();
        long removedCount = beforeCount - afterCount;

        log.info("✅ {} completed: {} results, {} filtered out ({}%)",
                getName(),
                beforeCount,
                removedCount,
                String.format("%.1f", (removedCount / (double) beforeCount) * 100));

        return filtered;
    }

    @Override
    public String getName() {
        return String.format("ThresholdRelevanceFilter_%s", String.format("%.4f", threshold));
    }

    @Override
    public String getDescription() {
        return String.format("Filters results with merged_score < %.4f", threshold);
    }
}

