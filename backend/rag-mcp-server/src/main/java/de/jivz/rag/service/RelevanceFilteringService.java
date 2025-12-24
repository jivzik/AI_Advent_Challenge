package de.jivz.rag.service;

import de.jivz.rag.dto.MergedSearchResultDto;
import de.jivz.rag.service.filtering.LlmFilterRelevanceFilter;
import de.jivz.rag.service.filtering.NoopRelevanceFilter;
import de.jivz.rag.service.filtering.RelevanceFilter;
import de.jivz.rag.service.filtering.ThresholdRelevanceFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис для управления фильтрацией результатов поиска по релевантности.
 *
 * Ответственность (SRP):
 * - Создание подходящего фильтра на основе конфигурации
 * - Применение фильтра к результатам
 * - Логирование статистики фильтрации
 *
 *架构принципы:
 * - Dependency Inversion: зависит от интерфейса RelevanceFilter, а не конкретных реализаций
 * - Factory паттерн: создание фильтров через конфигурацию
 * - Open/Closed: новые фильтры добавляются без изменения этого класса
 */
@Service
@Slf4j
public class RelevanceFilteringService {

    /**
     * Типы поддерживаемых фильтров.
     */
    public enum FilterType {
        THRESHOLD,      // Фильтр по пороговому значению (merged_score)
        LLM_FILTER,     // Фильтр по LLM-оценке (llmScore)
        NOOP            // Без фильтрации (нейтральный)
    }

    /**
     * Создаёт фильтр на основе типа и конфигурации.
     *
     * Factory паттерн для создания правильного фильтра без if-else в клиентском коде.
     *
     * @param type тип фильтра
     * @param threshold порог (для THRESHOLD и LLM_FILTER фильтров)
     * @return готовый к использованию фильтр
     */
    public RelevanceFilter createFilter(FilterType type, double threshold) {
        switch (type) {
            case THRESHOLD:
                return new ThresholdRelevanceFilter(threshold);
            case LLM_FILTER:
                return new LlmFilterRelevanceFilter(threshold);
            case NOOP:
                return new NoopRelevanceFilter();
            default:
                log.warn("Unknown filter type: {}, using NOOP", type);
                return new NoopRelevanceFilter();
        }
    }

    /**
     * Создаёт THRESHOLD фильтр с заданным порогом.
     *
     * @param threshold минимальный порог релевантности
     * @return ThresholdRelevanceFilter
     */
    public RelevanceFilter createThresholdFilter(double threshold) {
        return createFilter(FilterType.THRESHOLD, threshold);
    }

    /**
     * Создаёт LLM-фильтр с заданным порогом.
     *
     * @param threshold минимальный порог LLM-оценки
     * @return LlmFilterRelevanceFilter
     */
    public RelevanceFilter createLlmFilter(double threshold) {
        return createFilter(FilterType.LLM_FILTER, threshold);
    }

    /**
     * Создаёт NOOP фильтр (без фильтрации).
     *
     * @return NoopRelevanceFilter
     */
    public RelevanceFilter createNoopFilter() {
        return createFilter(FilterType.NOOP, 0.0);
    }

    /**
     * Применяет фильтр к результатам поиска.
     *
     * @param results результаты для фильтрации
     * @param filter фильтр для применения
     * @return отфильтрованные результаты
     */
    public List<MergedSearchResultDto> applyFilter(
            List<MergedSearchResultDto> results,
            RelevanceFilter filter) {

        if (filter == null) {
            log.warn("Filter is null, returning results unchanged");
            return results;
        }

        log.info("🔄 Applying filter: {}",
                filter.getName());
        log.debug("   Description: {}", filter.getDescription());

        return filter.filter(results);
    }

    /**
     * Применяет фильтр на основе конфигурации типа и порога.
     *
     * @param results результаты для фильтрации
     * @param filterType тип фильтра
     * @param threshold порог (для THRESHOLD фильтра)
     * @return отфильтрованные результаты
     */
    public List<MergedSearchResultDto> applyFilter(
            List<MergedSearchResultDto> results,
            FilterType filterType,
            double threshold) {

        RelevanceFilter filter = createFilter(filterType, threshold);
        return applyFilter(results, filter);
    }

    /**
     * Применяет фильтр THRESHOLD с заданным порогом.
     *
     * @param results результаты для фильтрации
     * @param threshold порог релевантности
     * @return отфильтрованные результаты
     */
    public List<MergedSearchResultDto> applyThresholdFilter(
            List<MergedSearchResultDto> results,
            double threshold) {

        return applyFilter(results, FilterType.THRESHOLD, threshold);
    }

    /**
     * Применяет LLM-фильтр с заданным порогом.
     *
     * @param results результаты для фильтрации
     * @param threshold порог LLM-оценки
     * @return отфильтрованные результаты
     */
    public List<MergedSearchResultDto> applyLlmFilter(
            List<MergedSearchResultDto> results,
            double threshold) {

        return applyFilter(results, FilterType.LLM_FILTER, threshold);
    }

    /**
     * Получить информацию о фильтре (для логирования/отладки).
     *
     * @param filter фильтр
     * @return строка с названием и описанием фильтра
     */
    public String getFilterInfo(RelevanceFilter filter) {
        if (filter == null) {
            return "No filter";
        }
        return String.format("%s: %s", filter.getName(), filter.getDescription());
    }
}

