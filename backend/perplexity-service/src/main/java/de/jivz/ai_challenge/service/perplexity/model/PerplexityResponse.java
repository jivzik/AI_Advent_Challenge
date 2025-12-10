package de.jivz.ai_challenge.service.perplexity.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * Response DTOs for Perplexity API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerplexityResponse {

    private String id;
    private String model;
    private Long created;
    private List<Choice> choices;
    private Usage usage;
    private List<String> citations;
    private List<SearchResult> searchResults;
    private String object;

    /**
     * Choice in the response.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Choice {
        private Integer index;
        private Message message;
        private Delta delta;
        private String finishReason;

    }

    /**
     * Message in the choice.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Message {
        private String role;
        private String content;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    /**
     * Delta for streaming responses.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Delta {
        private String role;
        private String content;

    }

    /**
     * Usage statistics.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;
        @JsonProperty("completion_tokens")
        private Integer completionTokens;
        @JsonProperty("total_tokens")
        private Integer totalTokens;
        @JsonProperty("search_context_size")
        private String searchContextSize;
        @JsonProperty("reasoning_tokens")
        private Integer reasoningTokens;
        @JsonProperty("citation_tokens")
        private Integer citationTokens;
        @JsonProperty("num_search_queries")
        private Integer numSearchQueries;
    }

    /**
     * Cost information.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Cost {
        private Double inputTokensCost;
        private Double outputTokensCost;
        private Double requestCost;
        private Double totalCost;

    }

    /**
     * Search result from Perplexity.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SearchResult {
        private String title;
        private String url;
        private String date;
        private String lastUpdated;
        private String snippet;
        private String source;

    }
}

