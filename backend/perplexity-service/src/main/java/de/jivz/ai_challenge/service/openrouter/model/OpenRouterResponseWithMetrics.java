package de.jivz.ai_challenge.service.openrouter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response wrapper that includes both the reply and metrics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenRouterResponseWithMetrics {
    private String reply;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private Double cost;
    private Long responseTimeMs;
    private String model;

    /**
     * Returns a formatted summary of the metrics.
     */
    public String getMetricsSummary() {
        return String.format(
                "Model: %s | Tokens: %d input, %d output, %d total | Cost: $%.6f | Time: %d ms",
                model,
                inputTokens != null ? inputTokens : 0,
                outputTokens != null ? outputTokens : 0,
                totalTokens != null ? totalTokens : 0,
                cost != null ? cost : 0.0,
                responseTimeMs != null ? responseTimeMs : 0
        );
    }
}