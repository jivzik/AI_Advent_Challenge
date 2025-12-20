package de.jivz.ai_challenge.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Perplexity Tool Result DTO
 * Результат выполнения инструмента Perplexity с полной информацией
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public  class PerplexityToolResult {
    private boolean success;
    private String tool;
    private Result result;
    private String timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Result {
        private boolean success;
        private String answer;
        private String model;
        private Usage usage;
        private List<String> citations;


        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Usage {
            @JsonProperty("prompt_tokens")
            private Integer promptTokens;

            @JsonProperty("completion_tokens")
            private Integer completionTokens;

            @JsonProperty("total_tokens")
            private Integer totalTokens;

            @JsonProperty("search_context_size")
            private String searchContextSize;

            private Cost cost;

            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class Cost {
                @JsonProperty("input_tokens_cost")
                private Double inputTokensCost;

                @JsonProperty("output_tokens_cost")
                private Double outputTokensCost;

                @JsonProperty("request_cost")
                private Double requestCost;

                @JsonProperty("total_cost")
                private Double totalCost;
            }
        }
    }
}