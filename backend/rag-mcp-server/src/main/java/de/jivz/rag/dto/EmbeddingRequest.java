package de.jivz.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO для OpenRouter Embeddings API.
 *
 * Формат:
 * {
 *   "input": ["text1", "text2"],
 *   "model": "qwen/qwen3-embedding-8b"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingRequest {

    /**
     * Массив текстов для генерации эмбеддингов.
     */
    @JsonProperty("input")
    private List<String> input;

    /**
     * Модель для генерации эмбеддингов.
     */
    @JsonProperty("model")
    private String model;
}

