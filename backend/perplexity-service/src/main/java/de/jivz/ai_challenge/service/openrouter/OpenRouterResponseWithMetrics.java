package de.jivz.ai_challenge.service.openrouter;

/**
 * Response wrapper that includes both the reply and metrics.
 */
public class OpenRouterResponseWithMetrics {
    private final String reply;
    private final Integer inputTokens;
    private final Integer outputTokens;
    private final Integer totalTokens;
    private final Double cost;
    private final Long responseTimeMs;
    private final String model;

    public OpenRouterResponseWithMetrics(String reply, Integer inputTokens, Integer outputTokens,
                                         Integer totalTokens, Double cost, Long responseTimeMs, String model) {
        this.reply = reply;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.totalTokens = totalTokens;
        this.cost = cost;
        this.responseTimeMs = responseTimeMs;
        this.model = model;
    }

    public String getReply() {
        return reply;
    }

    public Integer getInputTokens() {
        return inputTokens;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public Double getCost() {
        return cost;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public String getModel() {
        return model;
    }
}

