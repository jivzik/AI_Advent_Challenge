package de.jivz.rag.service;

import de.jivz.rag.dto.SearchResultDto;
import de.jivz.rag.repository.DocumentChunkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для полнотекстового поиска (FTS).
 *
 * Требует работающей PostgreSQL с таблицей document_chunks.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Full-Text Search Tests")
class KeywordSearchServiceTest {

    @Autowired
    private KeywordSearchService keywordSearchService;

    @Autowired
    private DocumentChunkRepository chunkRepository;

    // ==================== Query Normalization Tests ====================

    @Test
    @DisplayName("Should normalize query by removing extra spaces")
    void testNormalizeQuerySpaces() {
        String input = "  python    java    machine  ";
        String expected = "python java machine";
        assertEquals(expected, KeywordSearchService.normalizeQuery(input));
    }

    @Test
    @DisplayName("Should normalize query by removing special characters")
    void testNormalizeQuerySpecialChars() {
        String input = "python@#$java";
        String result = KeywordSearchService.normalizeQuery(input);
        assertTrue(result.contains("python"));
        assertTrue(result.contains("java"));
    }

    @Test
    @DisplayName("Should preserve FTS operators in query")
    void testNormalizeQueryPreservesOperators() {
        String input = "python & java | machine ! deep";
        String result = KeywordSearchService.normalizeQuery(input);
        assertTrue(result.contains("&"));
        assertTrue(result.contains("|"));
        assertTrue(result.contains("!"));
    }

    @Test
    @DisplayName("Should handle empty query")
    void testNormalizeQueryEmpty() {
        String result = KeywordSearchService.normalizeQuery("");
        assertEquals("", result);
    }

    @Test
    @DisplayName("Should handle null query")
    void testNormalizeQueryNull() {
        String result = KeywordSearchService.normalizeQuery(null);
        assertEquals("", result);
    }

    // ==================== Query Formatting Tests ====================

    @Test
    @DisplayName("Should convert AND to & operator")
    void testFormatAsQueryAND() {
        String input = "python AND java";
        String result = KeywordSearchService.formatAsQuery(input);
        assertTrue(result.contains("&"));
    }

    @Test
    @DisplayName("Should convert OR to | operator")
    void testFormatAsQueryOR() {
        String input = "python OR java";
        String result = KeywordSearchService.formatAsQuery(input);
        assertTrue(result.contains("|"));
    }

    @Test
    @DisplayName("Should convert NOT to ! operator")
    void testFormatAsQueryNOT() {
        String input = "python AND NOT java";
        String result = KeywordSearchService.formatAsQuery(input);
        assertTrue(result.contains("&"));
        assertTrue(result.contains("!"));
    }

    // ==================== Keyword Search Tests ====================

    @Test
    @DisplayName("Should return empty list for empty query")
    void testKeywordSearchEmptyQuery() {
        List<SearchResultDto> results = keywordSearchService.keywordSearch("", 10);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should return empty list for null query")
    void testKeywordSearchNullQuery() {
        List<SearchResultDto> results = keywordSearchService.keywordSearch(null, 10);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should perform keyword search (integration test)")
    void testKeywordSearch() {
        // Этот тест требует наличия данных в БД
        // В production test profile используется test database
        List<SearchResultDto> results = keywordSearchService.keywordSearch("test", 5);
        assertNotNull(results);
        // Результат может быть пустым если нет данных в БД
    }

    @Test
    @DisplayName("Should respect topK parameter")
    void testKeywordSearchRespectTopK() {
        List<SearchResultDto> results = keywordSearchService.keywordSearch("test", 3);
        assertNotNull(results);
        assertTrue(results.size() <= 3);
    }

    // ==================== Keyword Search In Document Tests ====================

    @Test
    @DisplayName("Should return empty list for empty query in document search")
    void testKeywordSearchInDocumentEmptyQuery() {
        List<SearchResultDto> results = keywordSearchService.keywordSearchInDocument("", 1L, 10);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should return empty list for invalid document ID")
    void testKeywordSearchInDocumentInvalidDocId() {
        List<SearchResultDto> results = keywordSearchService.keywordSearchInDocument("test", 0L, 10);
        assertTrue(results.isEmpty());

        results = keywordSearchService.keywordSearchInDocument("test", -1L, 10);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should return empty list for null document ID")
    void testKeywordSearchInDocumentNullDocId() {
        List<SearchResultDto> results = keywordSearchService.keywordSearchInDocument("test", null, 10);
        assertTrue(results.isEmpty());
    }

    // ==================== Advanced Search Tests ====================

    @Test
    @DisplayName("Should return empty list for empty query in advanced search")
    void testAdvancedSearchEmptyQuery() {
        List<SearchResultDto> results = keywordSearchService.advancedSearch("", 10);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should handle AND operator in advanced search")
    void testAdvancedSearchAND() {
        // Требует данные в БД с обоими словами
        List<SearchResultDto> results = keywordSearchService.advancedSearch("python & java", 10);
        assertNotNull(results);
    }

    @Test
    @DisplayName("Should handle OR operator in advanced search")
    void testAdvancedSearchOR() {
        List<SearchResultDto> results = keywordSearchService.advancedSearch("python | java", 10);
        assertNotNull(results);
    }

    @Test
    @DisplayName("Should handle NOT operator in advanced search")
    void testAdvancedSearchNOT() {
        List<SearchResultDto> results = keywordSearchService.advancedSearch("python & !java", 10);
        assertNotNull(results);
    }

    // ==================== Advanced Keyword Search Tests ====================

    @Test
    @DisplayName("Should perform advanced keyword search with ts_rank_cd")
    void testAdvancedKeywordSearch() {
        List<SearchResultDto> results = keywordSearchService.advancedKeywordSearch("test", 5);
        assertNotNull(results);
        assertTrue(results.size() <= 5);
    }

    @Test
    @DisplayName("Should return results with relevance scores")
    void testAdvancedKeywordSearchRelevanceScores() {
        List<SearchResultDto> results = keywordSearchService.advancedKeywordSearch("test", 5);
        for (SearchResultDto result : results) {
            assertNotNull(result.getSimilarity());
            assertTrue(result.getSimilarity() >= 0);
        }
    }

    // ==================== Result Mapping Tests ====================

    @Test
    @DisplayName("Should map search results correctly")
    void testSearchResultMapping() {
        List<SearchResultDto> results = keywordSearchService.keywordSearch("test", 1);
        if (!results.isEmpty()) {
            SearchResultDto result = results.get(0);
            assertNotNull(result.getChunkId());
            assertNotNull(result.getChunkText());
            // relevance может быть null в некоторых случаях
            assertNotNull(result.getCreatedAt());
        }
    }

    // ==================== Performance Tests ====================

    @Test
    @DisplayName("Keyword search should complete within reasonable time")
    void testKeywordSearchPerformance() {
        long startTime = System.currentTimeMillis();
        keywordSearchService.keywordSearch("test", 10);
        long duration = System.currentTimeMillis() - startTime;

        // Должно выполниться за менее чем 1 секунду
        assertTrue(duration < 1000, "Search took " + duration + "ms, expected < 1000ms");
    }

    @Test
    @DisplayName("Advanced search should complete within reasonable time")
    void testAdvancedSearchPerformance() {
        long startTime = System.currentTimeMillis();
        keywordSearchService.advancedKeywordSearch("test & python", 10);
        long duration = System.currentTimeMillis() - startTime;

        assertTrue(duration < 1000, "Search took " + duration + "ms, expected < 1000ms");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle query with only spaces")
    void testQueryOnlySpaces() {
        List<SearchResultDto> results = keywordSearchService.keywordSearch("   ", 10);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should handle query with special unicode characters")
    void testQueryUnicodeCharacters() {
        List<SearchResultDto> results = keywordSearchService.keywordSearch("тест", 5);
        assertNotNull(results);
    }

    @Test
    @DisplayName("Should handle zero topK")
    void testZeroTopK() {
        List<SearchResultDto> results = keywordSearchService.keywordSearch("test", 0);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should handle negative topK")
    void testNegativeTopK() {
        // Должно быть обработано как 0 или безопасное значение
        List<SearchResultDto> results = keywordSearchService.keywordSearch("test", -1);
        assertNotNull(results);
    }

    @Test
    @DisplayName("Should handle very large topK")
    void testVeryLargeTopK() {
        // Должно работать без ошибок (может вернуть меньше результатов)
        List<SearchResultDto> results = keywordSearchService.keywordSearch("test", 1000000);
        assertNotNull(results);
    }

    // ==================== Regression Tests ====================

    @Test
    @DisplayName("Should return results sorted by relevance")
    void testResultsSortedByRelevance() {
        List<SearchResultDto> results = keywordSearchService.keywordSearch("test", 10);

        if (results.size() > 1) {
            // Проверяем, что результаты отсортированы по релевантности (убывающий порядок)
            for (int i = 1; i < results.size(); i++) {
                Double prevRelevance = results.get(i - 1).getSimilarity();
                Double currRelevance = results.get(i).getSimilarity();

                if (prevRelevance != null && currRelevance != null) {
                    assertTrue(prevRelevance >= currRelevance,
                            "Results should be sorted by relevance in descending order");
                }
            }
        }
    }
}

