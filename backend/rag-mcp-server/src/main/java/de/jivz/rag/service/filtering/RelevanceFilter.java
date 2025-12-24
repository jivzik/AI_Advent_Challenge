package de.jivz.rag.service.filtering;

import de.jivz.rag.dto.MergedSearchResultDto;

import java.util.List;

/**
 * Интерфейс для фильтрации результатов поиска по релевантности.
 *
 * Применяет Open/Closed принцип: открыт для расширения (новые фильтры),
 * закрыт для модификации (существующие фильтры не меняются).
 *
 * Каждая реализация отвечает за одну стратегию фильтрации.
 */
public interface RelevanceFilter {

    /**
     * Применяет фильтр к результатам поиска.
     *
     * @param results исходные результаты для фильтрации
     * @return отфильтрованные результаты (может быть подмножеством исходных)
     */
    List<MergedSearchResultDto> filter(List<MergedSearchResultDto> results);

    /**
     * Возвращает название фильтра для логирования.
     *
     * @return название фильтра (например, "ThresholdFilter_0.5")
     */
    String getName();

    /**
     * Возвращает описание конфигурации фильтра для логирования.
     *
     * @return описание конфигурации
     */
    String getDescription();
}

