package de.jivz.rag.dto;

import lombok.*;

import java.util.List;

/**
 * DTO для результатов сравнения качества поиска.
 *
 * Содержит:
 * - Результаты с фильтром и без
 * - Метрики качества (precision, recall, F1)
 * - Статистику фильтрации (сколько результатов отфильтровано)
 *
 * Используется для анализа влияния фильтра на качество результатов.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchQualityMetrics {

    // ========== Исходные данные ==========

    /**
     * Поисковый запрос.
     */
    private String query;

    /**
     * Использованный фильтр.
     */
    private String filterName;

    /**
     * Описание конфигурации фильтра.
     */
    private String filterDescription;

    // ========== Результаты поиска ==========

    /**
     * Результаты БЕЗ фильтрации (Режим A).
     * После merge + rerank, без применения фильтров.
     */
    private List<SearchResultDto> resultsNoFilter;

    /**
     * Результаты С пороговым фильтром (Режим B).
     * После применения ThresholdRelevanceFilter по merged_score.
     */
    private List<SearchResultDto> resultsWithThresholdFilter;

    /**
     * Результаты С LLM-фильтром (Режим C).
     * После LLM-переранжирования и применения llmFilterThreshold.
     */
    private List<SearchResultDto> resultsWithLlmFilter;


    // ========== Статистика фильтрации ==========

    /**
     * Количество результатов до фильтрации.
     */
    private int countBefore;

    /**
     * Количество результатов после порогового фильтра (Режим B).
     */
    private int countAfterThreshold;

    /**
     * Количество отфильтрованных результатов пороговым фильтром.
     */
    private int countRemovedThreshold;

    /**
     * Процент отфильтрованных результатов пороговым фильтром.
     */
    private double percentageRemovedThreshold;

    /**
     * Количество результатов после LLM-фильтра (Режим C).
     */
    private int countAfterLlm;

    /**
     * Количество отфильтрованных результатов LLM-фильтром.
     */
    private int countRemovedLlm;

    /**
     * Процент отфильтрованных результатов LLM-фильтром.
     */
    private double percentageRemovedLlm;

    // ========== Метрики качества ==========

    /**
     * Точность (precision): доля релевантных результатов среди возвращённых.
     * Если countAfter > 0: relevantResults / countAfter
     * Иначе: 0.0
     */
    private double precision;

    /**
     * Полнота (recall): доля найденных релевантных результатов из всех релевантных.
     * countAfter / countBefore (если countBefore > 0)
     */
    private double recall;

    /**
     * F1-score: гармоническое среднее precision и recall.
     * 2 * (precision * recall) / (precision + recall)
     */
    private double f1Score;

    /**
     * Средний score до фильтрации.
     */
    private double avgScoreBefore;

    /**
     * Средний score после порогового фильтра.
     */
    private double avgScoreAfterThreshold;

    /**
     * Средний LLM score до LLM-фильтра.
     */
    private double avgLlmScoreBefore;

    /**
     * Средний LLM score после LLM-фильтра.
     */
    private double avgLlmScoreAfter;

    /**
     * Средний merged_score результатов, оставшихся после LLM-фильтра.
     */
    private double avgScoreAfterLlm;


    /**
     * Минимальный score до фильтрации.
     */
    private double minScoreBefore;

    /**
     * Максимальный score до фильтрации.
     */
    private double maxScoreBefore;

    /**
     * Минимальный score после порогового фильтра.
     */
    private double minScoreAfterThreshold;

    /**
     * Максимальный score после порогового фильтра.
     */
    private double maxScoreAfterThreshold;

    /**
     * Минимальный LLM score после LLM-фильтра.
     */
    private double minLlmScoreAfter;

    /**
     * Максимальный LLM score после LLM-фильтра.
     */
    private double maxLlmScoreAfter;


    // ========== Метаинформация ==========

    /**
     * Время выполнения фильтрации (ms).
     */
    private long executionTimeMs;

    /**
     * Был ли пороговый фильтр применён.
     */
    private boolean thresholdFilterApplied;

    /**
     * Был ли LLM-фильтр применён.
     */
    private boolean llmFilterApplied;

    /**
     * Порог для порогового фильтра (merged_score).
     */
    private double filterThreshold;

    /**
     * Порог для LLM-фильтра (llmScore).
     */
    private double llmFilterThreshold;

    /**
     * Дополнительные комментарии.
     */
    private String comment;

    // Для обратной совместимости со старым API
    /**
     * @deprecated Используйте thresholdFilterApplied вместо этого поля
     */
    @Deprecated
    private boolean filterApplied;

}

