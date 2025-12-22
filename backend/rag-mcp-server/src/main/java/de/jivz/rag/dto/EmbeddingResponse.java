package de.jivz.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO от OpenRouter Embeddings API.
 *
 * Формат:
 * {
 *   "object": "list",
 *   "data": [
 *     {
 *       "object": "embedding",
 *       "embedding": [0.0123, -0.0456, ...],
 *       "index": 0
 *     }
 *   ],
 *   "model": "text-embedding-3-large",
 *   "id": "emb_1234567890abcdef",
 *   "usage": {
 *     "prompt_tokens": 32,
 *     "total_tokens": 32,
 *     "cost": 0.00064
 *   }
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingResponse {

    @JsonProperty("object")
    private String object;

    @JsonProperty("data")
    private List<EmbeddingData> data;

    @JsonProperty("model")
    private String model;

    @JsonProperty("id")
    private String id;

    @JsonProperty("usage")
    private Usage usage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmbeddingData {

        @JsonProperty("object")
        private String object;

        @JsonProperty("embedding")
        private List<Double> embedding;

        @JsonProperty("index")
        private Integer index;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {

        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        @JsonProperty("total_tokens")
        private Integer totalTokens;

        @JsonProperty("cost")
        private Double cost;
    }
}

