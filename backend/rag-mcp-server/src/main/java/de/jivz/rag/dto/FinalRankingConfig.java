package de.jivz.rag.dto;

import lombok.*;

/**
 * Конфигурация для финальной сортировки и фильтрации результатов.
 *
 * ЭТАП 5: Финальная сортировка и фильтрация
 *
 * Функции:
 * 1. Сортировка по combined_score (убывание)
 * 2. Фильтрация по минимальному порогу
 * 3. Удаление дубликатов (max N чанков с одного документа)
 * 4. Ограничение на топ-K результатов
 * 5. Добавление метаданных
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalRankingConfig {

    /**
     * Минимальный combined_score для включения результата (0.0 - 1.0)
     * Default: 0.3 (для hybrid search)
     * Set: 0.0 для отключения фильтрации
     */
    @Builder.Default
    private Double minScoreThreshold = 0.3;

    /**
     * Максимальное количество результатов для возврата
     * Default: 10
     * Set: Integer.MAX_VALUE для отключения ограничения
     */
    @Builder.Default
    private Integer topK = 10;

    /**
     * Максимальное количество чанков с одного документа
     * Default: Integer.MAX_VALUE (не ограничивает)
     * Set: 1 для одного чанка с документа
     * Set: 3 для максимум 3 чанков с документа
     *
     * Полезно для разнообразия результатов (не одни результаты с одного документа)
     */
    @Builder.Default
    private Integer maxChunksPerDocument = Integer.MAX_VALUE;

    /**
     * Включать ли удаление дубликатов по содержимому?
     * Default: false
     *
     * Если true, то чанки с похожим содержимым (>95% совпадение) будут удалены
     */
    @Builder.Default
    private Boolean removeDuplicates = false;

    /**
     * Порог сходства для удаления дубликатов (0.0 - 1.0)
     * Default: 0.95 (95% совпадение)
     * Используется только если removeDuplicates = true
     */
    @Builder.Default
    private Double duplicateSimilarityThreshold = 0.95;

    /**
     * Отсортировать ли результаты по combined_score (убывание)?
     * Default: true
     * Set: false если результаты уже отсортированы
     */
    @Builder.Default
    private Boolean sortByScore = true;

    /**
     * Включать ли метаданные в результаты?
     * Default: true
     *
     * Добавляет:
     * - document_name
     * - chunk_index
     * - semantic_score (если есть)
     * - keyword_score (если есть)
     * - combined_score
     * - relevance_rank (позиция в финальном списке)
     */
    @Builder.Default
    private Boolean includeMetadata = true;

    /**
     * Валидирует конфигурацию
     * @throws IllegalArgumentException если конфигурация невалидна
     */
    public void validate() {
        if (minScoreThreshold < 0.0 || minScoreThreshold > 1.0) {
            throw new IllegalArgumentException(
                "minScoreThreshold должен быть в диапазоне [0.0, 1.0]");
        }
        if (topK < 1) {
            throw new IllegalArgumentException("topK должен быть >= 1");
        }
        if (maxChunksPerDocument < 1) {
            throw new IllegalArgumentException("maxChunksPerDocument должен быть >= 1");
        }
        if (duplicateSimilarityThreshold < 0.0 || duplicateSimilarityThreshold > 1.0) {
            throw new IllegalArgumentException(
                "duplicateSimilarityThreshold должен быть в диапазоне [0.0, 1.0]");
        }
    }
}

