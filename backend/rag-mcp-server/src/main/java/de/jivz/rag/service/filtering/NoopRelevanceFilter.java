package de.jivz.rag.service.filtering;

import de.jivz.rag.dto.MergedSearchResultDto;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Нейтральный фильтр, который не выполняет никакой фильтрации.
 *
 * Использование: Когда нужно отключить фильтрацию, но поддерживать унифицированный интерфейс.
 * Следует Null Object паттерну: безопасно возвращает все результаты без изменений.
 *
 * Преимущества:
 * - Избегаем null checks в клиентском коде
 * - Упрощаем логику (всегда есть фильтр, но он может быть no-op)
 * - Легче тестировать
 */
@Slf4j
public class NoopRelevanceFilter implements RelevanceFilter {

    /**
     * Возвращает все результаты без изменений.
     *
     * @param results исходные результаты
     * @return те же результаты (без фильтрации)
     */
    @Override
    public List<MergedSearchResultDto> filter(List<MergedSearchResultDto> results) {
        if (results == null || results.isEmpty()) {
            log.debug("⏭️  No filtering needed (results is empty)");
            return results;
        }

        log.debug("⏭️  Skipping filtering (filter disabled)");
        log.info("✅ NoopRelevanceFilter completed: {} results (no filtering applied)",
                results.size());

        return results;
    }

    @Override
    public String getName() {
        return "NoopRelevanceFilter";
    }

    @Override
    public String getDescription() {
        return "No filtering applied (passes all results unchanged)";
    }
}

