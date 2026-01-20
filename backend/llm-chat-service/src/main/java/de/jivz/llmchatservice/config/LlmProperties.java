package de.jivz.llmchatservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Ollama LLM integration.
 * Binds properties with prefix "llm.ollama" from application.properties.
 */
@Data
@Component
@ConfigurationProperties(prefix = "llm.ollama")
public class LlmProperties {

    /**
     * Base URL for Ollama API endpoint
     * Default: http://localhost:11434
     */
    private String baseUrl = "http://localhost:11434";

    /**
     * Model name to use for chat completions
     * Default: llama2
     * Examples: llama2, mistral, codellama, neural-chat
     */
    private String model;

    /**
     * Temperature controls randomness in responses (0.0 - 2.0)
     * Lower values = more focused and deterministic
     * Higher values = more creative and varied
     * Default: 0.7
     */
    private Double temperature = 0.7;

    /**
     * Maximum number of tokens to generate in response
     * Default: 2000
     */
    private Integer maxTokens = 2000;

    /**
     * Timeout in seconds for LLM API calls
     * Default: 120 seconds
     */
    private Integer timeoutSeconds = 120;

}
