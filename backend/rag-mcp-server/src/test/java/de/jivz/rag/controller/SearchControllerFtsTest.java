package de.jivz.rag.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.rag.dto.SearchResultDto;
import de.jivz.rag.service.RagService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты для SearchController с FTS endpoint'ами.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Search Controller FTS Integration Tests")
class SearchControllerFtsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RagService ragService;

    // ==================== Keyword Search Tests ====================

    @Test
    @DisplayName("POST /api/search/keywords should accept valid request")
    void testKeywordSearchValidRequest() throws Exception {
        String request = objectMapper.writeValueAsString(
                new SearchController.KeywordSearchRequest("test", 5)
        );

        mockMvc.perform(post("/api/search/keywords")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").exists())
                .andExpect(jsonPath("$.resultsCount").isNumber())
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.processingTime").exists());
    }

    @Test
    @DisplayName("POST /api/search/keywords should reject empty query")
    void testKeywordSearchEmptyQuery() throws Exception {
        String request = objectMapper.writeValueAsString(
                new SearchController.KeywordSearchRequest("", 5)
        );

        mockMvc.perform(post("/api/search/keywords")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("POST /api/search/keywords should use default topK if not provided")
    void testKeywordSearchDefaultTopK() throws Exception {
        String request = objectMapper.writeValueAsString(
                new SearchController.KeywordSearchRequest("test", null)
        );

        mockMvc.perform(post("/api/search/keywords")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    @DisplayName("POST /api/search/keywords response should have correct structure")
    void testKeywordSearchResponseStructure() throws Exception {
        String request = objectMapper.writeValueAsString(
                new SearchController.KeywordSearchRequest("test", 10)
        );

        MvcResult result = mockMvc.perform(post("/api/search/keywords")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assert(responseBody.contains("\"query\""));
        assert(responseBody.contains("\"resultsCount\""));
        assert(responseBody.contains("\"results\""));
        assert(responseBody.contains("\"processingTime\""));
    }

    // ==================== Keyword Search In Document Tests ====================

    @Test
    @DisplayName("POST /api/search/keywords/document/{id} should accept valid request")
    void testKeywordSearchInDocumentValidRequest() throws Exception {
        String request = objectMapper.writeValueAsString(
                new SearchController.KeywordSearchRequest("test", 5)
        );

        mockMvc.perform(post("/api/search/keywords/document/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(1))
                .andExpect(jsonPath("$.query").exists())
                .andExpect(jsonPath("$.resultsCount").isNumber());
    }

    @Test
    @DisplayName("POST /api/search/keywords/document/{id} should reject invalid document ID")
    void testKeywordSearchInDocumentInvalidId() throws Exception {
        String request = objectMapper.writeValueAsString(
                new SearchController.KeywordSearchRequest("test", 5)
        );

        mockMvc.perform(post("/api/search/keywords/document/0")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("POST /api/search/keywords/document/{id} should reject empty query")
    void testKeywordSearchInDocumentEmptyQuery() throws Exception {
        String request = objectMapper.writeValueAsString(
                new SearchController.KeywordSearchRequest("", 5)
        );

        mockMvc.perform(post("/api/search/keywords/document/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest());
    }

    // ==================== Advanced Search Tests ====================

    @Test
    @DisplayName("POST /api/search/advanced should accept valid request")
    void testAdvancedSearchValidRequest() throws Exception {
        String request = objectMapper.writeValueAsString(
                new SearchController.AdvancedSearchRequest("python & java", 10)
        );

        mockMvc.perform(post("/api/search/advanced")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").exists())
                .andExpect(jsonPath("$.resultsCount").isNumber())
                .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    @DisplayName("POST /api/search/advanced should handle AND operator")
    void testAdvancedSearchAND() throws Exception {
        String request = objectMapper.writeValueAsString(
                new SearchController.AdvancedSearchRequest("test & advanced", 10)
        );

        MvcResult result = mockMvc.perform(post("/api/search/advanced")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andReturn();

        assert(result.getResponse().getContentAsString().contains("\"query\""));
    }

    @Test
    @DisplayName("POST /api/search/advanced should handle OR operator")
    void testAdvancedSearchOR() throws Exception {
        String request = objectMapper.writeValueAsString(
                new SearchController.AdvancedSearchRequest("test | advanced", 10)
        );

        mockMvc.perform(post("/api/search/advanced")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/search/advanced should handle NOT operator")
    void testAdvancedSearchNOT() throws Exception {
        String request = objectMapper.writeValueAsString(
                new SearchController.AdvancedSearchRequest("test & !advanced", 10)
        );

        mockMvc.perform(post("/api/search/advanced")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/search/advanced should reject empty query")
    void testAdvancedSearchEmptyQuery() throws Exception {
        String request = objectMapper.writeValueAsString(
                new SearchController.AdvancedSearchRequest("", 10)
        );

        mockMvc.perform(post("/api/search/advanced")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest());
    }

    // ==================== Ranked Search Tests ====================

    @Test
    @DisplayName("POST /api/search/ranked should accept valid request")
    void testRankedSearchValidRequest() throws Exception {
        String request = objectMapper.writeValueAsString(
                new SearchController.KeywordSearchRequest("test", 10)
        );

        mockMvc.perform(post("/api/search/ranked")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").exists())
                .andExpect(jsonPath("$.rankingMethod").value("ts_rank_cd"));
    }

    @Test
    @DisplayName("POST /api/search/ranked should include ranking method in response")
    void testRankedSearchRankingMethod() throws Exception {
        String request = objectMapper.writeValueAsString(
                new SearchController.KeywordSearchRequest("test", 5)
        );

        MvcResult result = mockMvc.perform(post("/api/search/ranked")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assert(responseBody.contains("\"rankingMethod\":\"ts_rank_cd\""));
    }

    @Test
    @DisplayName("POST /api/search/ranked should reject empty query")
    void testRankedSearchEmptyQuery() throws Exception {
        String request = objectMapper.writeValueAsString(
                new SearchController.KeywordSearchRequest("", 5)
        );

        mockMvc.perform(post("/api/search/ranked")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest());
    }

    // ==================== Response Format Tests ====================

    @Test
    @DisplayName("All FTS endpoints should return consistent response format")
    void testResponseFormatConsistency() throws Exception {
        String[] endpoints = {
                "/api/search/keywords",
                "/api/search/advanced",
                "/api/search/ranked"
        };

        String request = objectMapper.writeValueAsString(
                new SearchController.KeywordSearchRequest("test", 5)
        );

        for (String endpoint : endpoints) {
            mockMvc.perform(post(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.processingTime").exists());
        }
    }

    @Test
    @DisplayName("Result items should have correct structure")
    void testResultItemStructure() throws Exception {
        String request = objectMapper.writeValueAsString(
                new SearchController.KeywordSearchRequest("test", 5)
        );

        mockMvc.perform(post("/api/search/keywords")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].chunkId").isNumber())
                .andExpect(jsonPath("$.results[0].documentName").isString())
                .andExpect(jsonPath("$.results[0].chunkText").isString())
                .andExpect(jsonPath("$.results[0].relevance").isNumber())
                .andExpect(jsonPath("$.results[0].chunkIndex").isNumber());
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Should handle malformed JSON gracefully")
    void testMalformedJsonRequest() throws Exception {
        mockMvc.perform(post("/api/search/keywords")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Should handle missing required fields")
    void testMissingRequiredFields() throws Exception {
        String request = "{}";

        mockMvc.perform(post("/api/search/keywords")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle null query gracefully")
    void testNullQuery() throws Exception {
        String request = objectMapper.writeValueAsString(
                new SearchController.KeywordSearchRequest(null, 5)
        );

        mockMvc.perform(post("/api/search/keywords")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest());
    }

    // ==================== Performance Tests ====================

    @Test
    @DisplayName("Keyword search endpoint should respond quickly")
    void testKeywordSearchResponseTime() throws Exception {
        String request = objectMapper.writeValueAsString(
                new SearchController.KeywordSearchRequest("test", 10)
        );

        long startTime = System.currentTimeMillis();

        mockMvc.perform(post("/api/search/keywords")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk());

        long duration = System.currentTimeMillis() - startTime;

        // Endpoint должен ответить за менее чем 2 секунды
        assert(duration < 2000);
    }

    // ==================== CORS Tests ====================

    @Test
    @DisplayName("FTS endpoints should support CORS")
    void testCORSHeaders() throws Exception {
        String request = objectMapper.writeValueAsString(
                new SearchController.KeywordSearchRequest("test", 5)
        );

        mockMvc.perform(post("/api/search/keywords")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
                .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk());
    }
}

