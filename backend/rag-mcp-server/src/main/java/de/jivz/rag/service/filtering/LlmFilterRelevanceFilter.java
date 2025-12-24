package de.jivz.rag.service.filtering;

import de.jivz.rag.dto.MergedSearchResultDto;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Фильтр релевантности на основе LLM-оценки.
 *
 * Принцип: Отфильтровывает результаты, у которых llmScore < llmFilterThreshold.
 *
 * Используется в режиме C (LLM-фильтр) для удаления результатов,
 * которые LLM оценил как мало релевантные относительно запроса.
 *
 * Пример использования:
 * - llmFilterThreshold = 0.7: фильтрует результаты с llmScore < 0.7
 * - llmFilterThreshold = 0.5: умеренная фильтрация
 * - llmFilterThreshold = 0.9: очень строгая фильтрация
 */
@Slf4j
public class LlmFilterRelevanceFilter implements RelevanceFilter {

    private final double threshold;

    /**
     * Создаёт фильтр с заданным порогом LLM-оценки.
     *
     * @param threshold минимальный порог LLM-оценки (0.0 - 1.0)
     */
    public LlmFilterRelevanceFilter(double threshold) {
        this.threshold = Math.max(0.0, Math.min(1.0, threshold));
    }

    /**
     * Применяет LLM-фильтрацию к результатам.
     *
     * @param results исходные результаты (должны иметь llmScore)
     * @return результаты, где llmScore >= threshold
     */
    @Override
    public List<MergedSearchResultDto> filter(List<MergedSearchResultDto> results) {
        if (results == null || results.isEmpty()) {
            log.warn("⚠️  Results for LLM filtering is empty");
            return results;
        }

        log.debug("🔍 Applying {} with threshold={}",
                getName(), String.format("%.4f", threshold));

        long beforeCount = results.size();

        List<MergedSearchResultDto> filtered = results.stream()
                .filter(result -> {
                    Double llmScore = result.getLlmScore();
                    return llmScore != null && llmScore >= threshold;
                })
                .collect(Collectors.toList());

        long afterCount = filtered.size();
        long removedCount = beforeCount - afterCount;

        log.debug("  ✅ Before: {}, After: {}, Removed: {}",
                beforeCount, afterCount, removedCount);

        return filtered;
    }

    @Override
    public String getName() {
        return String.format("LlmFilter_%.2f", threshold);
    }

    @Override
    public String getDescription() {
        return String.format("LLM-based filter: removes results with llmScore < %.4f", threshold);
    }
}

