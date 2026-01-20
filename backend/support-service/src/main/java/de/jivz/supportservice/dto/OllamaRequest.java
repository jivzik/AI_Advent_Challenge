package de.jivz.supportservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO für Ollama API-Anfragen
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OllamaRequest {

    /**
     * Modell für die Generierung
     */
    @JsonProperty("model")
    private String model;

    /**
     * User prompt/message
     */
    @JsonProperty("prompt")
    private String prompt;

    /**
     * System prompt (optional)
     */
    @JsonProperty("system")
    private String system;

    /**
     * Enable streaming responses
     */
    @JsonProperty("stream")
    private Boolean stream;

    /**
     * Model parameters (temperature, num_predict, etc.)
     */
    @JsonProperty("options")
    private Map<String, Object> options;
}

