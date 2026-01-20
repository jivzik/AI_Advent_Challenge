package de.jivz.supportservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties für lokale Ollama LLM Integration
 */
@Component
@ConfigurationProperties(prefix = "llm.ollama")
@Data
public class OllamaProperties {

    /**
     * Base URL for Ollama API endpoint
     * Default: http://localhost:11434
     */
    private String baseUrl = "http://localhost:11434";

    /**
     * Model name to use for chat completions
     * Default: gemma2:2b
     */
    private String model = "gemma2:2b";

    /**
     * Temperature controls randomness in responses (0.0 - 2.0)
     * Default: 0.7
     */
    private Double temperature = 0.7;

    /**
     * Maximum number of tokens to generate in response
     * Default: 1000
     */
    private Integer maxTokens = 1000;

    /**
     * Timeout in seconds for LLM API calls
     * Default: 120 seconds
     */
    private Integer timeoutSeconds = 120;
}

