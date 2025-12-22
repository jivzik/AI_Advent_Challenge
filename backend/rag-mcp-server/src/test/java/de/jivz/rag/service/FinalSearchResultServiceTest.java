package de.jivz.rag.service;

import de.jivz.rag.dto.FinalRankingConfig;
import de.jivz.rag.dto.FinalSearchResultDto;
import de.jivz.rag.dto.MergedSearchResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-тесты для FinalSearchResultService (ЭТАП 5: Финальная сортировка и фильтрация)
 *
 * Тестируются:
 * 1. Сортировка по combined_score
 * 2. Фильтрация по минимальному порогу
 * 3. Удаление дубликатов
 * 4. Ограничение на макс чанков с документа
 * 5. Ограничение на топ-K результатов
 * 6. Добавление метаданных
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FinalSearchResultService Tests")
public class FinalSearchResultServiceTest {

    @InjectMocks
    private FinalSearchResultService finalSearchService;

    private List<MergedSearchResultDto> testResults;

    @BeforeEach
    void setUp() {
        testResults = createTestData();
    }

    /**
     * Создаёт тестовые данные:
     *
     * Document 1:
     * ├─ chunk1: combined=0.886
     * ├─ chunk2: combined=0.75
     * └─ chunk3: combined=0.65
     *
     * Document 2:
     * ├─ chunk4: combined=0.92
     * ├─ chunk5: combined=0.45
     * └─ chunk6: combined=0.25 (будет отфильтрован)
     *
     * Document 3:
     * └─ chunk7: combined=0.55
     */
    private List<MergedSearchResultDto> createTestData() {
        List<MergedSearchResultDto> results = new ArrayList<>();

        // Document 1
        results.add(createMergedResult(1L, 1L, "Doc 1", 0, "text1", 0.89, 0.88, 0.886));
        results.add(createMergedResult(2L, 1L, "Doc 1", 1, "text2", 0.75, 0.75, 0.75));
        results.add(createMergedResult(3L, 1L, "Doc 1", 2, "text3", 0.65, 0.65, 0.65));

        // Document 2
        results.add(createMergedResult(4L, 2L, "Doc 2", 0, "text4", 0.92, 0.92, 0.92));
        results.add(createMergedResult(5L, 2L, "Doc 2", 1, "text5", 0.45, 0.45, 0.45));
        results.add(createMergedResult(6L, 2L, "Doc 2", 2, "text6", 0.25, 0.25, 0.25));

        // Document 3
        results.add(createMergedResult(7L, 3L, "Doc 3", 0, "text7", 0.55, 0.55, 0.55));

        return results;
    }

    private MergedSearchResultDto createMergedResult(
            Long chunkId, Long docId, String docName, Integer chunkIdx,
            String text, Double semantic, Double keyword, Double merged) {
        return MergedSearchResultDto.builder()
                .chunkId(chunkId)
                .documentId(docId)
                .documentName(docName)
                .chunkIndex(chunkIdx)
                .chunkText(text)
                .semanticScore(semantic)
                .keywordScore(keyword)
                .mergedScore(merged)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ============ Сортировка Tests ============

    @Test
    @DisplayName("✅ Test 1: Sorting by combined_score (descending)")
    void testSortingByScore() {
        // Arrange
        FinalRankingConfig config = FinalRankingConfig.builder()
                .minScoreThreshold(0.0)
                .topK(Integer.MAX_VALUE)
                .sortByScore(true)
                .build();

        // Act
        List<FinalSearchResultDto> results = finalSearchService.finalizeResults(testResults, config);

        // Assert
        assertEquals(7, results.size());
        // chunk4: 0.92 (первая)
        assertEquals(4L, results.get(0).getChunkId());
        assertEquals(0.92, results.get(0).getCombinedScore(), 0.001);

        // chunk1: 0.886 (вторая)
        assertEquals(1L, results.get(1).getChunkId());
        assertEquals(0.886, results.get(1).getCombinedScore(), 0.001);

        // chunk2: 0.75 (третья)
        assertEquals(2L, results.get(2).getChunkId());

        // Проверяем, что порядок убывающий
        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(results.get(i).getCombinedScore() >= results.get(i + 1).getCombinedScore(),
                    "Results should be sorted in descending order");
        }
    }

    // ============ Фильтрация Tests ============

    @Test
    @DisplayName("✅ Test 2: Filtering by minScoreThreshold")
    void testFilteringByThreshold() {
        // Arrange - threshold = 0.5 (исключает chunk6: 0.25 и chunk5: 0.45)
        FinalRankingConfig config = FinalRankingConfig.builder()
                .minScoreThreshold(0.5)
                .topK(Integer.MAX_VALUE)
                .sortByScore(true)
                .build();

        // Act
        List<FinalSearchResultDto> results = finalSearchService.finalizeResults(testResults, config);

        // Assert
        assertEquals(5, results.size(), "Should filter out 2 results");

        // Проверяем, что исключены правильные результаты
        assertFalse(results.stream().anyMatch(r -> r.getChunkId() == 6L),
                "chunk6 (0.25) should be filtered out");
        assertFalse(results.stream().anyMatch(r -> r.getChunkId() == 5L),
                "chunk5 (0.45) should be filtered out");

        // Проверяем, что оставлены правильные результаты
        assertTrue(results.stream().anyMatch(r -> r.getChunkId() == 4L),
                "chunk4 (0.92) should be included");
        assertTrue(results.stream().anyMatch(r -> r.getChunkId() == 1L),
                "chunk1 (0.886) should be included");
    }

    @Test
    @DisplayName("✅ Test 3: Disabled threshold filtering")
    void testDisabledThresholdFiltering() {
        // Arrange - threshold = 0.0 (отключает фильтрацию)
        FinalRankingConfig config = FinalRankingConfig.builder()
                .minScoreThreshold(0.0)
                .topK(Integer.MAX_VALUE)
                .build();

        // Act
        List<FinalSearchResultDto> results = finalSearchService.finalizeResults(testResults, config);

        // Assert
        assertEquals(7, results.size(), "All results should be included");
    }

    // ============ TopK Tests ============

    @Test
    @DisplayName("✅ Test 4: TopK limiting")
    void testTopKLimiting() {
        // Arrange
        FinalRankingConfig config = FinalRankingConfig.builder()
                .minScoreThreshold(0.0)
                .topK(3)
                .sortByScore(true)
                .build();

        // Act
        List<FinalSearchResultDto> results = finalSearchService.finalizeResults(testResults, config);

        // Assert
        assertEquals(3, results.size(), "Should return only top 3");

        // Проверяем, что это именно топ-3
        assertEquals(4L, results.get(0).getChunkId()); // chunk4: 0.92
        assertEquals(1L, results.get(1).getChunkId()); // chunk1: 0.886
        assertEquals(2L, results.get(2).getChunkId()); // chunk2: 0.75
    }

    // ============ Максимум чанков с документа Tests ============

    @Test
    @DisplayName("✅ Test 5: Max chunks per document limiting")
    void testMaxChunksPerDocument() {
        // Arrange - максимум 1 чанк с документа
        FinalRankingConfig config = FinalRankingConfig.builder()
                .minScoreThreshold(0.0)
                .topK(Integer.MAX_VALUE)
                .maxChunksPerDocument(1)
                .sortByScore(true)
                .build();

        // Act
        List<FinalSearchResultDto> results = finalSearchService.finalizeResults(testResults, config);

        // Assert
        assertEquals(3, results.size(), "Should have only 3 results (1 per document)");

        // Проверяем, что выбраны лучшие чанки из каждого документа
        assertEquals(4L, results.get(0).getChunkId()); // Doc2: chunk4 (0.92)
        assertEquals(1L, results.get(1).getChunkId()); // Doc1: chunk1 (0.886)
        assertEquals(7L, results.get(2).getChunkId()); // Doc3: chunk7 (0.55)
    }

    @Test
    @DisplayName("✅ Test 6: Max 2 chunks per document")
    void testMaxTwoChunksPerDocument() {
        // Arrange
        FinalRankingConfig config = FinalRankingConfig.builder()
                .minScoreThreshold(0.0)
                .topK(Integer.MAX_VALUE)
                .maxChunksPerDocument(2)
                .sortByScore(true)
                .build();

        // Act
        List<FinalSearchResultDto> results = finalSearchService.finalizeResults(testResults, config);

        // Assert
        assertEquals(5, results.size(), "Should have 5 results (max 2 per document)");

        // Проверяем, что из Doc1 включены только 2 лучших чанка
        long doc1Chunks = results.stream()
                .filter(r -> r.getDocumentId() == 1L)
                .count();
        assertEquals(2, doc1Chunks, "Doc1 should have 2 chunks");
    }

    // ============ Метаданные Tests ============

    @Test
    @DisplayName("✅ Test 7: Relevance rank and percentile metadata")
    void testMetadata() {
        // Arrange
        FinalRankingConfig config = FinalRankingConfig.builder()
                .minScoreThreshold(0.0)
                .topK(Integer.MAX_VALUE)
                .sortByScore(true)
                .includeMetadata(true)
                .build();

        // Act
        List<FinalSearchResultDto> results = finalSearchService.finalizeResults(testResults, config);

        // Assert
        assertEquals(7, results.size());

        // Проверяем ранги
        for (int i = 0; i < results.size(); i++) {
            assertEquals(i + 1, results.get(i).getRelevanceRank(),
                    "Rank should be position + 1");
        }

        // Проверяем процентили (должны быть убывающими)
        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(results.get(i).getRelevancePercentile() > results.get(i + 1).getRelevancePercentile(),
                    "Percentile should decrease");
        }

        // Первый результат должен быть в топ 100%
        assertTrue(results.get(0).getRelevancePercentile() > 90);

        // Последний результат должен быть в низко 20%
        assertTrue(results.get(6).getRelevancePercentile() < 20);
    }

    @Test
    @DisplayName("✅ Test 8: Source determination (SEMANTIC/KEYWORD/BOTH)")
    void testSourceDetermination() {
        // Arrange
        FinalRankingConfig config = FinalRankingConfig.builder()
                .minScoreThreshold(0.0)
                .topK(Integer.MAX_VALUE)
                .build();

        // Act
        List<FinalSearchResultDto> results = finalSearchService.finalizeResults(testResults, config);

        // Assert
        for (FinalSearchResultDto result : results) {
            assertNotNull(result.getSource(), "Source should be determined");
            assertTrue(
                    result.getSource().equals("SEMANTIC") ||
                    result.getSource().equals("KEYWORD") ||
                    result.getSource().equals("BOTH"),
                    "Source should be one of: SEMANTIC, KEYWORD, BOTH");
        }
    }

    // ============ Комбинированные Tests ============

    @Test
    @DisplayName("✅ Test 9: Complete pipeline with filtering and topK")
    void testCompletePipeline() {
        // Arrange
        FinalRankingConfig config = FinalRankingConfig.builder()
                .minScoreThreshold(0.5)
                .topK(3)
                .sortByScore(true)
                .includeMetadata(true)
                .build();

        // Act
        List<FinalSearchResultDto> results = finalSearchService.finalizeResults(testResults, config);

        // Assert
        assertEquals(3, results.size(), "Should return exactly 3 results");

        // Проверяем, что все результаты выше порога
        for (FinalSearchResultDto result : results) {
            assertTrue(result.getCombinedScore() >= 0.5,
                    "All results should be above threshold");
        }

        // Проверяем порядок
        assertEquals(4L, results.get(0).getChunkId()); // chunk4: 0.92
        assertEquals(1L, results.get(1).getChunkId()); // chunk1: 0.886
        assertEquals(2L, results.get(2).getChunkId()); // chunk2: 0.75
    }

    @Test
    @DisplayName("✅ Test 10: Default configuration")
    void testDefaultConfiguration() {
        // Act
        List<FinalSearchResultDto> results = finalSearchService.finalizeDefault(testResults);

        // Assert
        assertTrue(results.size() <= 10, "Default topK is 10");
        assertTrue(results.stream().allMatch(r -> r.getCombinedScore() >= 0.3),
                "Default minScoreThreshold is 0.3");
        assertTrue(results.stream().allMatch(r -> r.getRelevanceRank() != null),
                "Metadata should be included by default");
    }

    // ============ Edge Cases Tests ============

    @Test
    @DisplayName("✅ Test 11: Empty results")
    void testEmptyResults() {
        // Arrange
        FinalRankingConfig config = FinalRankingConfig.builder().build();

        // Act
        List<FinalSearchResultDto> results = finalSearchService.finalizeResults(new ArrayList<>(), config);

        // Assert
        assertTrue(results.isEmpty(), "Should return empty list");
    }

    @Test
    @DisplayName("✅ Test 12: Null results")
    void testNullResults() {
        // Arrange
        FinalRankingConfig config = FinalRankingConfig.builder().build();

        // Act
        List<FinalSearchResultDto> results = finalSearchService.finalizeResults(null, config);

        // Assert
        assertTrue(results.isEmpty(), "Should return empty list for null input");
    }

    @Test
    @DisplayName("✅ Test 13: Configuration validation")
    void testConfigValidation() {
        // Arrange
        FinalRankingConfig invalidConfig = FinalRankingConfig.builder()
                .minScoreThreshold(1.5)
                .build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, invalidConfig::validate,
                "Should throw exception for invalid threshold");
    }

    @Test
    @DisplayName("✅ Test 14: Convenience method - finalizeWithThreshold")
    void testConvenienceMethodWithThreshold() {
        // Act
        List<FinalSearchResultDto> results = finalSearchService.finalizeWithThreshold(
                testResults, 0.5, 5);

        // Assert
        assertTrue(results.size() <= 5, "Should respect topK");
        assertTrue(results.stream().allMatch(r -> r.getCombinedScore() >= 0.5),
                "Should respect threshold");
    }

    @Test
    @DisplayName("✅ Test 15: Convenience method - finalizeWithDiversification")
    void testConvenienceMethodWithDiversification() {
        // Act
        List<FinalSearchResultDto> results = finalSearchService.finalizeWithDiversification(
                testResults, 10, 2);

        // Assert
        assertTrue(results.size() <= 10, "Should respect topK");

        // Проверяем, что максимум 2 чанка с документа
        for (Long docId = 1L; docId <= 3L; docId++) {
            long chunksPerDoc = results.stream()
                    .filter(r -> r.getDocumentId().equals(docId))
                    .count();
            assertTrue(chunksPerDoc <= 2,
                    "Should have max 2 chunks per document");
        }
    }

    // ============ Utility ============

    private void assertAlmostEquals(double expected, double actual, double delta) {
        assertTrue(Math.abs(expected - actual) <= delta,
                String.format("Expected: %f, Actual: %f, Delta: %f",
                        expected, actual, delta));
    }
}

