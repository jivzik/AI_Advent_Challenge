package de.jivz.supportservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO für Ollama API-Antworten
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaResponse {

    /**
     * Verwendetes Modell
     */
    @JsonProperty("model")
    private String model;

    /**
     * Generierter Response-Text
     */
    @JsonProperty("response")
    private String response;

    /**
     * Generierung abgeschlossen
     */
    @JsonProperty("done")
    private Boolean done;

    /**
     * Kontext-Array für Conversation Continuity (optional)
     */
    @JsonProperty("context")
    private List<Integer> context;

    /**
     * Gesamtdauer in Nanosekunden
     */
    @JsonProperty("total_duration")
    private Long totalDuration;

    /**
     * Ladezeit in Nanosekunden
     */
    @JsonProperty("load_duration")
    private Long loadDuration;

    /**
     * Prompt Evaluation Count
     */
    @JsonProperty("prompt_eval_count")
    private Integer promptEvalCount;

    /**
     * Prompt Evaluation Dauer in Nanosekunden
     */
    @JsonProperty("prompt_eval_duration")
    private Long promptEvalDuration;

    /**
     * Evaluation Count (generierte Tokens)
     */
    @JsonProperty("eval_count")
    private Integer evalCount;

    /**
     * Evaluation Dauer in Nanosekunden
     */
    @JsonProperty("eval_duration")
    private Long evalDuration;
}

