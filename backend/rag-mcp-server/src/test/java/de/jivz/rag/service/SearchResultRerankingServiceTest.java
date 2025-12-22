package de.jivz.rag.service;

import de.jivz.rag.dto.MergedSearchResultDto;
import de.jivz.rag.dto.RerankingStrategyConfig;
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
 * Unit-тесты для SearchResultRerankingService (ЭТАП 4: Reranking)
 *
 * Тестируются все три стратегии переранжирования:
 * 1. WEIGHTED_SUM
 * 2. MAX_SCORE
 * 3. RRF (Reciprocal Rank Fusion)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SearchResultRerankingService Tests")
public class SearchResultRerankingServiceTest {

    @InjectMocks
    private SearchResultRerankingService rerankingService;

    private List<MergedSearchResultDto> testResults;

    @BeforeEach
    void setUp() {
        testResults = createTestData();
    }

    /**
     * Создаёт тестовые данные:
     *
     * Semantic ranking: [chunk1(0.89), chunk3(0.82), chunk5(0.75)]
     * Keyword ranking:  [chunk2(0.95), chunk1(0.88), chunk4(0.70)]
     *
     * @return список результатов для тестирования
     */
    private List<MergedSearchResultDto> createTestData() {
        List<MergedSearchResultDto> results = new ArrayList<>();

        // chunk1: semantic=0.89, keyword=0.88
        results.add(MergedSearchResultDto.builder()
                .chunkId(1L)
                .documentId(1L)
                .documentName("doc1")
                .chunkIndex(0)
                .chunkText("text1")
                .semanticScore(0.89)
                .keywordScore(0.88)
                .createdAt(LocalDateTime.now())
                .build());

        // chunk2: semantic=null, keyword=0.95
        results.add(MergedSearchResultDto.builder()
                .chunkId(2L)
                .documentId(1L)
                .documentName("doc1")
                .chunkIndex(1)
                .chunkText("text2")
                .semanticScore(null)
                .keywordScore(0.95)
                .createdAt(LocalDateTime.now())
                .build());

        // chunk3: semantic=0.82, keyword=null
        results.add(MergedSearchResultDto.builder()
                .chunkId(3L)
                .documentId(2L)
                .documentName("doc2")
                .chunkIndex(0)
                .chunkText("text3")
                .semanticScore(0.82)
                .keywordScore(null)
                .createdAt(LocalDateTime.now())
                .build());

        // chunk4: semantic=null, keyword=0.70
        results.add(MergedSearchResultDto.builder()
                .chunkId(4L)
                .documentId(2L)
                .documentName("doc2")
                .chunkIndex(1)
                .chunkText("text4")
                .semanticScore(null)
                .keywordScore(0.70)
                .createdAt(LocalDateTime.now())
                .build());

        // chunk5: semantic=0.75, keyword=null
        results.add(MergedSearchResultDto.builder()
                .chunkId(5L)
                .documentId(3L)
                .documentName("doc3")
                .chunkIndex(0)
                .chunkText("text5")
                .semanticScore(0.75)
                .keywordScore(null)
                .createdAt(LocalDateTime.now())
                .build());

        return results;
    }

    // ============ WEIGHTED_SUM Tests ============

    @Test
    @DisplayName("✅ Test 1: WEIGHTED_SUM with default weights (0.6/0.4)")
    void testWeightedSumDefault() {
        // Arrange
        RerankingStrategyConfig config = RerankingStrategyConfig.builder()
                .strategy(RerankingStrategyConfig.Strategy.WEIGHTED_SUM)
                .semanticWeight(0.6)
                .keywordWeight(0.4)
                .build();

        // Act
        List<MergedSearchResultDto> reranked = rerankingService.rerank(testResults, config);

        // Assert
        assertEquals(5, reranked.size(), "Should have 5 results");

        // Проверяем порядок: chunk1, chunk3, chunk5, chunk2, chunk4
        // chunk1: 0.6×0.89 + 0.4×0.88 = 0.534 + 0.352 = 0.886
        assertEquals(1L, reranked.get(0).getChunkId(), "First should be chunk1");
        assertAlmostEquals(0.886, reranked.get(0).getMergedScore(), 0.001);

        // chunk2: 0.6×0 + 0.4×0.95 = 0.38
        assertEquals(2L, reranked.get(3).getChunkId(), "chunk2 should be 4th");
        assertAlmostEquals(0.38, reranked.get(3).getMergedScore(), 0.001);
    }

    @Test
    @DisplayName("✅ Test 2: WEIGHTED_SUM with custom weights (0.7/0.3)")
    void testWeightedSumCustomWeights() {
        // Arrange - для общих вопросов: больший вес на семантику
        RerankingStrategyConfig config = RerankingStrategyConfig.builder()
                .strategy(RerankingStrategyConfig.Strategy.WEIGHTED_SUM)
                .semanticWeight(0.7)
                .keywordWeight(0.3)
                .build();

        // Act
        List<MergedSearchResultDto> reranked = rerankingService.rerank(testResults, config);

        // Assert
        assertEquals(5, reranked.size());
        assertEquals(1L, reranked.get(0).getChunkId());
        // chunk1: 0.7×0.89 + 0.3×0.88 = 0.623 + 0.264 = 0.887
        assertAlmostEquals(0.887, reranked.get(0).getMergedScore(), 0.001);
    }

    @Test
    @DisplayName("✅ Test 3: WEIGHTED_SUM with keyword priority (0.3/0.7)")
    void testWeightedSumKeywordPriority() {
        // Arrange - для точного поиска: больший вес на keyword
        RerankingStrategyConfig config = RerankingStrategyConfig.builder()
                .strategy(RerankingStrategyConfig.Strategy.WEIGHTED_SUM)
                .semanticWeight(0.3)
                .keywordWeight(0.7)
                .build();

        // Act
        List<MergedSearchResultDto> reranked = rerankingService.rerank(testResults, config);

        // Assert
        assertEquals(5, reranked.size());
        // chunk2: 0.3×0 + 0.7×0.95 = 0.665 (должна быть выше chunk1)
        assertEquals(2L, reranked.get(0).getChunkId(), "chunk2 should be first with keyword priority");
        assertAlmostEquals(0.665, reranked.get(0).getMergedScore(), 0.001);
    }

    @Test
    @DisplayName("✅ Test 4: WEIGHTED_SUM scores verification")
    void testWeightedSumScoresVerification() {
        // Arrange
        RerankingStrategyConfig config = RerankingStrategyConfig.builder()
                .strategy(RerankingStrategyConfig.Strategy.WEIGHTED_SUM)
                .semanticWeight(0.6)
                .keywordWeight(0.4)
                .build();

        // Act
        List<MergedSearchResultDto> reranked = rerankingService.rerank(testResults, config);

        // Assert - проверяем все вычисленные scores
        // chunk1: 0.6×0.89 + 0.4×0.88 = 0.886
        MergedSearchResultDto chunk1 = reranked.stream()
                .filter(r -> r.getChunkId() == 1L).findFirst().orElse(null);
        assertNotNull(chunk1);
        assertAlmostEquals(0.886, chunk1.getMergedScore(), 0.001);

        // chunk2: 0.6×0 + 0.4×0.95 = 0.38
        MergedSearchResultDto chunk2 = reranked.stream()
                .filter(r -> r.getChunkId() == 2L).findFirst().orElse(null);
        assertNotNull(chunk2);
        assertAlmostEquals(0.38, chunk2.getMergedScore(), 0.001);

        // chunk3: 0.6×0.82 + 0.4×0 = 0.492
        MergedSearchResultDto chunk3 = reranked.stream()
                .filter(r -> r.getChunkId() == 3L).findFirst().orElse(null);
        assertNotNull(chunk3);
        assertAlmostEquals(0.492, chunk3.getMergedScore(), 0.001);

        // chunk4: 0.6×0 + 0.4×0.7 = 0.28
        MergedSearchResultDto chunk4 = reranked.stream()
                .filter(r -> r.getChunkId() == 4L).findFirst().orElse(null);
        assertNotNull(chunk4);
        assertAlmostEquals(0.28, chunk4.getMergedScore(), 0.001);

        // chunk5: 0.6×0.75 + 0.4×0 = 0.45
        MergedSearchResultDto chunk5 = reranked.stream()
                .filter(r -> r.getChunkId() == 5L).findFirst().orElse(null);
        assertNotNull(chunk5);
        assertAlmostEquals(0.45, chunk5.getMergedScore(), 0.001);
    }

    // ============ MAX_SCORE Tests ============

    @Test
    @DisplayName("✅ Test 5: MAX_SCORE strategy")
    void testMaxScore() {
        // Arrange
        RerankingStrategyConfig config = RerankingStrategyConfig.builder()
                .strategy(RerankingStrategyConfig.Strategy.MAX_SCORE)
                .build();

        // Act
        List<MergedSearchResultDto> reranked = rerankingService.rerank(testResults, config);

        // Assert
        assertEquals(5, reranked.size());

        // chunk2: max(0, 0.95) = 0.95 (первая)
        assertEquals(2L, reranked.get(0).getChunkId(), "chunk2 should be first");
        assertAlmostEquals(0.95, reranked.get(0).getMergedScore(), 0.001);

        // chunk1: max(0.89, 0.88) = 0.89 (вторая)
        assertEquals(1L, reranked.get(1).getChunkId(), "chunk1 should be second");
        assertAlmostEquals(0.89, reranked.get(1).getMergedScore(), 0.001);

        // chunk3: max(0.82, 0) = 0.82 (третья)
        assertEquals(3L, reranked.get(2).getChunkId(), "chunk3 should be third");
        assertAlmostEquals(0.82, reranked.get(2).getMergedScore(), 0.001);
    }

    @Test
    @DisplayName("✅ Test 6: MAX_SCORE priority to best method")
    void testMaxScorePriority() {
        // Arrange
        RerankingStrategyConfig config = RerankingStrategyConfig.builder()
                .strategy(RerankingStrategyConfig.Strategy.MAX_SCORE)
                .build();

        // Act
        List<MergedSearchResultDto> reranked = rerankingService.rerank(testResults, config);

        // Assert - chunk2 с высоким keywordScore должна быть выше chunk1
        assertEquals(2L, reranked.get(0).getChunkId());
        assertEquals(1L, reranked.get(1).getChunkId());
        assertTrue(reranked.get(0).getMergedScore() > reranked.get(1).getMergedScore());
    }

    // ============ RRF Tests ============

    @Test
    @DisplayName("✅ Test 7: RRF (Reciprocal Rank Fusion) strategy")
    void testRRF() {
        // Arrange
        RerankingStrategyConfig config = RerankingStrategyConfig.builder()
                .strategy(RerankingStrategyConfig.Strategy.RRF)
                .rrfK(60)
                .build();

        // Act
        List<MergedSearchResultDto> reranked = rerankingService.rerank(testResults, config);

        // Assert
        assertEquals(5, reranked.size());

        // Проверяем, что scores вычислены и отсортированы
        for (int i = 0; i < reranked.size() - 1; i++) {
            assertTrue(reranked.get(i).getMergedScore() >= reranked.get(i + 1).getMergedScore(),
                    "Results should be sorted by RRF score in descending order");
        }

        // chunk1 должна быть выше остальных т.к. она в обоих списках
        assertEquals(1L, reranked.get(0).getChunkId());
        assertTrue(reranked.get(0).getMergedScore() > 0, "RRF score should be positive");
    }

    @Test
    @DisplayName("✅ Test 8: RRF calculation verification")
    void testRRFCalculation() {
        // Arrange
        RerankingStrategyConfig config = RerankingStrategyConfig.builder()
                .strategy(RerankingStrategyConfig.Strategy.RRF)
                .rrfK(60)
                .build();

        // Act
        List<MergedSearchResultDto> reranked = rerankingService.rerank(testResults, config);

        // Assert
        // Semantic ranking: [chunk1(rank1), chunk3(rank2), chunk5(rank3)]
        // Keyword ranking:  [chunk2(rank1), chunk1(rank2), chunk4(rank3)]
        //
        // chunk1: 1/(60+1) + 1/(60+2) = 0.01639 + 0.01613 = 0.03252
        MergedSearchResultDto chunk1 = reranked.stream()
                .filter(r -> r.getChunkId() == 1L).findFirst().orElse(null);
        assertNotNull(chunk1);
        assertAlmostEquals(0.03252, chunk1.getMergedScore(), 0.0001);

        // chunk2: 1/(60+1) = 0.01639
        MergedSearchResultDto chunk2 = reranked.stream()
                .filter(r -> r.getChunkId() == 2L).findFirst().orElse(null);
        assertNotNull(chunk2);
        assertAlmostEquals(0.01639, chunk2.getMergedScore(), 0.0001);

        // chunk3: 1/(60+2) = 0.01613
        MergedSearchResultDto chunk3 = reranked.stream()
                .filter(r -> r.getChunkId() == 3L).findFirst().orElse(null);
        assertNotNull(chunk3);
        assertAlmostEquals(0.01613, chunk3.getMergedScore(), 0.0001);
    }

    @Test
    @DisplayName("✅ Test 9: RRF with different k value")
    void testRRFDifferentK() {
        // Arrange
        RerankingStrategyConfig config = RerankingStrategyConfig.builder()
                .strategy(RerankingStrategyConfig.Strategy.RRF)
                .rrfK(100) // Используем другое k
                .build();

        // Act
        List<MergedSearchResultDto> reranked = rerankingService.rerank(testResults, config);

        // Assert
        assertEquals(5, reranked.size());
        assertEquals(1L, reranked.get(0).getChunkId()); // chunk1 всё ещё первая

        // Scores должны быть меньше при большем k
        // chunk1: 1/(100+1) + 1/(100+2) = 0.00990 + 0.00980 = 0.01970
        assertAlmostEquals(0.01970, reranked.get(0).getMergedScore(), 0.0001);
    }

    // ============ Edge Cases & General Tests ============

    @Test
    @DisplayName("✅ Test 10: Rerank with empty results")
    void testRerankEmptyResults() {
        // Arrange
        List<MergedSearchResultDto> emptyResults = new ArrayList<>();
        RerankingStrategyConfig config = RerankingStrategyConfig.builder().build();

        // Act
        List<MergedSearchResultDto> reranked = rerankingService.rerank(emptyResults, config);

        // Assert
        assertTrue(reranked.isEmpty(), "Should return empty list for empty input");
    }

    @Test
    @DisplayName("✅ Test 11: Rerank with null results")
    void testRerankNullResults() {
        // Arrange
        RerankingStrategyConfig config = RerankingStrategyConfig.builder().build();

        // Act
        List<MergedSearchResultDto> reranked = rerankingService.rerank(null, config);

        // Assert
        assertTrue(reranked.isEmpty(), "Should return empty list for null input");
    }

    @Test
    @DisplayName("✅ Test 12: Sorting order verification")
    void testSortingOrder() {
        // Arrange
        RerankingStrategyConfig config = RerankingStrategyConfig.builder()
                .strategy(RerankingStrategyConfig.Strategy.WEIGHTED_SUM)
                .semanticWeight(0.6)
                .keywordWeight(0.4)
                .build();

        // Act
        List<MergedSearchResultDto> reranked = rerankingService.rerank(testResults, config);

        // Assert - проверяем что результаты отсортированы по убыванию
        for (int i = 0; i < reranked.size() - 1; i++) {
            assertTrue(reranked.get(i).getMergedScore() >= reranked.get(i + 1).getMergedScore(),
                    "Results should be sorted in descending order by merged score");
        }
    }

    @Test
    @DisplayName("✅ Test 13: Config validation for invalid weights")
    void testConfigValidation() {
        // Arrange
        RerankingStrategyConfig invalidConfig = RerankingStrategyConfig.builder()
                .semanticWeight(1.5)
                .keywordWeight(0.4)
                .build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, invalidConfig::validate,
                "Should throw exception for invalid weights");
    }

    @Test
    @DisplayName("✅ Test 14: Convenience methods")
    void testConvenienceMethods() {
        // Test rerankDefault
        List<MergedSearchResultDto> defaultReranked = rerankingService.rerankDefault(testResults);
        assertEquals(5, defaultReranked.size());

        // Test rerankWeightedSum
        List<MergedSearchResultDto> customReranked = rerankingService.rerankWeightedSum(testResults, 0.7, 0.3);
        assertEquals(5, customReranked.size());

        // Test rerankMaxScore
        List<MergedSearchResultDto> maxScoreReranked = rerankingService.rerankMaxScore(testResults);
        assertEquals(5, maxScoreReranked.size());

        // Test rerankRRF
        List<MergedSearchResultDto> rrfReranked = rerankingService.rerankRRF(testResults);
        assertEquals(5, rrfReranked.size());
    }

    @Test
    @DisplayName("✅ Test 15: Single result reranking")
    void testSingleResultReranking() {
        // Arrange
        List<MergedSearchResultDto> singleResult = new ArrayList<>();
        singleResult.add(testResults.get(0));
        RerankingStrategyConfig config = RerankingStrategyConfig.builder().build();

        // Act
        List<MergedSearchResultDto> reranked = rerankingService.rerank(singleResult, config);

        // Assert
        assertEquals(1, reranked.size());
        assertEquals(1L, reranked.get(0).getChunkId());
    }

    // ============ Utility Methods ============

    private void assertAlmostEquals(double expected, double actual, double delta) {
        assertTrue(Math.abs(expected - actual) <= delta,
                String.format("Expected: %f, Actual: %f, Delta: %f",
                        expected, actual, delta));
    }
}

