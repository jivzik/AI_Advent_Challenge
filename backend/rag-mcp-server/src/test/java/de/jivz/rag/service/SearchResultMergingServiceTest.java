package de.jivz.rag.service;

import de.jivz.rag.dto.MergedSearchResultDto;
import de.jivz.rag.dto.SearchResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SearchResultMergingService Tests")
class SearchResultMergingServiceTest {

    private SearchResultMergingService mergingService;

    @BeforeEach
    void setUp() {
        mergingService = new SearchResultMergingService();
    }

    /**
     * Тест 1: Объединение результатов без дубликатов.
     *
     * Проверяет:
     * - Создание Map для хранения уникальных чанков
     * - Объединение результатов из двух источников
     */
    @Test
    @DisplayName("Merge results without duplicates")
    void testMergeResultsWithoutDuplicates() {
        // Arrange: Создаём семантические результаты
        List<SearchResultDto> semanticResults = List.of(
                createSearchResult(1L, "Chunk 1", 0.89),
                createSearchResult(3L, "Chunk 3", 0.82),
                createSearchResult(5L, "Chunk 5", 0.75)
        );

        // Arrange: Создаём результаты ключевого поиска
        List<SearchResultDto> keywordResults = List.of(
                createSearchResult(2L, "Chunk 2", 0.95),
                createSearchResult(1L, "Chunk 1", 0.88),  // Дубликат
                createSearchResult(4L, "Chunk 4", 0.70)
        );

        // Act: Объединяем результаты
        List<MergedSearchResultDto> merged = mergingService.mergeResults(
                semanticResults, keywordResults, Integer.MAX_VALUE);

        // Assert: Проверяем, что нет дубликатов
        assertEquals(5, merged.size(), "Should have 5 unique chunks");

        // Проверяем, что все чанки присутствуют
        assertTrue(merged.stream().anyMatch(r -> r.getChunkId() == 1L));
        assertTrue(merged.stream().anyMatch(r -> r.getChunkId() == 2L));
        assertTrue(merged.stream().anyMatch(r -> r.getChunkId() == 3L));
        assertTrue(merged.stream().anyMatch(r -> r.getChunkId() == 4L));
        assertTrue(merged.stream().anyMatch(r -> r.getChunkId() == 5L));
    }

    /**
     * Тест 2: Проверка scores для каждого чанка.
     *
     * Проверяет:
     * - chunk1: semantic=0.89, keyword=0.88 (оба скора)
     * - chunk2: semantic=null, keyword=0.95 (только keyword)
     * - chunk3: semantic=0.82, keyword=null (только semantic)
     */
    @Test
    @DisplayName("Verify scores for each chunk")
    void testVerifyScoresForEachChunk() {
        // Arrange
        List<SearchResultDto> semanticResults = List.of(
                createSearchResult(1L, "Chunk 1", 0.89),
                createSearchResult(3L, "Chunk 3", 0.82)
        );

        List<SearchResultDto> keywordResults = List.of(
                createSearchResult(2L, "Chunk 2", 0.95),
                createSearchResult(1L, "Chunk 1", 0.88)
        );

        // Act
        List<MergedSearchResultDto> merged = mergingService.mergeResults(
                semanticResults, keywordResults, Integer.MAX_VALUE);

        // Assert: Проверяем chunk1 (оба скора)
        MergedSearchResultDto chunk1 = merged.stream()
                .filter(r -> r.getChunkId() == 1L)
                .findFirst()
                .orElseThrow();
        assertEquals(0.89, chunk1.getSemanticScore(), 0.001);
        assertEquals(0.88, chunk1.getKeywordScore(), 0.001);
        assertTrue(chunk1.hasBothScores());

        // Assert: Проверяем chunk2 (только keyword)
        MergedSearchResultDto chunk2 = merged.stream()
                .filter(r -> r.getChunkId() == 2L)
                .findFirst()
                .orElseThrow();
        assertNull(chunk2.getSemanticScore());
        assertEquals(0.95, chunk2.getKeywordScore(), 0.001);

        // Assert: Проверяем chunk3 (только semantic)
        MergedSearchResultDto chunk3 = merged.stream()
                .filter(r -> r.getChunkId() == 3L)
                .findFirst()
                .orElseThrow();
        assertEquals(0.82, chunk3.getSemanticScore(), 0.001);
        assertNull(chunk3.getKeywordScore());
    }

    /**
     * Тест 3: Проверка вычисления комбинированной оценки.
     *
     * mergedScore = 0.6 * semanticScore + 0.4 * keywordScore
     */
    @Test
    @DisplayName("Verify merged score calculation")
    void testMergedScoreCalculation() {
        // Arrange
        List<SearchResultDto> semanticResults = List.of(
                createSearchResult(1L, "Chunk 1", 0.89)
        );

        List<SearchResultDto> keywordResults = List.of(
                createSearchResult(1L, "Chunk 1", 0.88)
        );

        // Act
        List<MergedSearchResultDto> merged = mergingService.mergeResults(
                semanticResults, keywordResults, 0.6, 0.4, Integer.MAX_VALUE);

        // Assert: mergedScore = 0.6 * 0.89 + 0.4 * 0.88 = 0.534 + 0.352 = 0.886
        MergedSearchResultDto result = merged.get(0);
        double expectedScore = 0.6 * 0.89 + 0.4 * 0.88;
        assertEquals(expectedScore, result.getMergedScore(), 0.001,
                "Merged score should be: 0.6 * 0.89 + 0.4 * 0.88");
    }

    /**
     * Тест 4: Проверка сортировки по mergedScore.
     *
     * Результаты должны быть отсортированы в порядке убывания mergedScore.
     */
    @Test
    @DisplayName("Verify sorting by merged score (descending)")
    void testSortingByMergedScore() {
        // Arrange
        List<SearchResultDto> semanticResults = List.of(
                createSearchResult(1L, "Chunk 1", 0.50),
                createSearchResult(3L, "Chunk 3", 0.90)
        );

        List<SearchResultDto> keywordResults = List.of(
                createSearchResult(2L, "Chunk 2", 0.80)
        );

        // Act: weights 0.6 / 0.4
        List<MergedSearchResultDto> merged = mergingService.mergeResults(
                semanticResults, keywordResults, 0.6, 0.4, Integer.MAX_VALUE);

        // Assert: Проверяем сортировку
        // chunk3: 0.6 * 0.90 = 0.54
        // chunk2: 0.4 * 0.80 = 0.32
        // chunk1: 0.6 * 0.50 = 0.30
        assertEquals(3, merged.get(0).getChunkId());  // chunk3 первый
        assertEquals(2, merged.get(1).getChunkId());  // chunk2 второй
        assertEquals(1, merged.get(2).getChunkId());  // chunk1 третий
    }

    /**
     * Тест 5: Проверка ограничения по topK.
     *
     * Если topK=2, то должно быть возвращено максимум 2 результата.
     */
    @Test
    @DisplayName("Verify topK limit")
    void testTopKLimit() {
        // Arrange
        List<SearchResultDto> semanticResults = List.of(
                createSearchResult(1L, "Chunk 1", 0.89),
                createSearchResult(2L, "Chunk 2", 0.87),
                createSearchResult(3L, "Chunk 3", 0.85)
        );

        // Act: topK = 2
        List<MergedSearchResultDto> merged = mergingService.mergeResults(
                semanticResults, List.of(), 0.6, 0.4, 2);

        // Assert: Должно быть только 2 результата
        assertEquals(2, merged.size());
    }

    /**
     * Тест 6: Проверка фильтрации по минимальной оценке.
     */
    @Test
    @DisplayName("Filter by minimum score")
    void testFilterByMinScore() {
        // Arrange
        List<SearchResultDto> semanticResults = List.of(
                createSearchResult(1L, "Chunk 1", 0.80),
                createSearchResult(2L, "Chunk 2", 0.50),
                createSearchResult(3L, "Chunk 3", 0.30)
        );

        List<MergedSearchResultDto> merged = mergingService.mergeResults(
                semanticResults, List.of(), Integer.MAX_VALUE);

        // Act: Фильтруем с minScore = 0.4
        // Оценки: chunk1=0.6*0.80=0.48, chunk2=0.6*0.50=0.30, chunk3=0.6*0.30=0.18
        List<MergedSearchResultDto> filtered = mergingService.filterByScore(merged, 0.4);

        // Assert: Должно остаться только 1 результат (chunk1, т.к. 0.48 > 0.4)
        assertEquals(1, filtered.size());
        assertTrue(filtered.stream().allMatch(r -> r.getMergedScore() >= 0.4));
    }

    /**
     * Тест 7: Пустые результаты.
     */
    @Test
    @DisplayName("Handle empty results")
    void testEmptyResults() {
        // Act
        List<MergedSearchResultDto> merged = mergingService.mergeResults(
                List.of(), List.of(), Integer.MAX_VALUE);

        // Assert
        assertTrue(merged.isEmpty());
    }

    /**
     * Тест 8: Null результаты.
     */
    @Test
    @DisplayName("Handle null results")
    void testNullResults() {
        // Act
        List<MergedSearchResultDto> merged = mergingService.mergeResults(
                null, null, Integer.MAX_VALUE);

        // Assert
        assertTrue(merged.isEmpty());
    }

    /**
     * Тест 9: Преобразование в SearchResultDto.
     */
    @Test
    @DisplayName("Convert to SearchResultDto")
    void testConvertToSearchResultDto() {
        // Arrange
        List<SearchResultDto> semanticResults = List.of(
                createSearchResult(1L, "Chunk 1", 0.89)
        );

        List<MergedSearchResultDto> merged = mergingService.mergeResults(
                semanticResults, List.of(), Integer.MAX_VALUE);

        // Act
        List<SearchResultDto> converted = mergingService.toSearchResultDtos(merged);

        // Assert
        assertEquals(1, converted.size());
        assertEquals(1L, converted.get(0).getChunkId());
        assertNotNull(converted.get(0).getSimilarity());
        assertEquals(merged.get(0).getMergedScore(), converted.get(0).getSimilarity(), 0.001);
    }

    /**
     * Тест 10: Проверка нормализации весов.
     *
     * Веса должны быть нормализованы так, чтобы их сумма была 1.0.
     */
    @Test
    @DisplayName("Normalize weights")
    void testNormalizeWeights() {
        // Arrange: weights 0.3 и 0.3 (сумма = 0.6)
        List<SearchResultDto> semanticResults = List.of(
                createSearchResult(1L, "Chunk 1", 0.80)
        );

        List<SearchResultDto> keywordResults = List.of(
                createSearchResult(1L, "Chunk 1", 0.80)
        );

        // Act
        List<MergedSearchResultDto> merged = mergingService.mergeResults(
                semanticResults, keywordResults, 0.3, 0.3, Integer.MAX_VALUE);

        // Assert: Нормализованные веса = 0.5 / 0.5
        // mergedScore = 0.5 * 0.80 + 0.5 * 0.80 = 0.80
        assertEquals(0.80, merged.get(0).getMergedScore(), 0.001);
    }

    // ========== Helper Methods ==========

    /**
     * Создаёт SearchResultDto для тестирования.
     */
    private SearchResultDto createSearchResult(Long chunkId, String chunkText, Double similarity) {
        return SearchResultDto.builder()
                .chunkId(chunkId)
                .documentId(1L)
                .documentName("Test Document")
                .chunkIndex(0)
                .chunkText(chunkText)
                .similarity(similarity)
                .createdAt(LocalDateTime.now())
                .build();
    }
}

